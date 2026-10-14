#!/usr/bin/env bash
# Recria o banco, aplica V1-V3 do repositorio + pacote proposto e executa os testes.
# Uso: PGHOST=/tmp PGPORT=54329 REPO=/caminho/do/repo ./run_all.sh
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
DB="${DB:-loja_a3_test}"
REPO="${REPO:?informe REPO com o caminho do repositorio}"
PSQL="psql -X -v ON_ERROR_STOP=1 -q"

$PSQL -U postgres -d postgres -c "DROP DATABASE IF EXISTS $DB" -c "CREATE DATABASE $DB"
for f in "$REPO"/src/main/resources/db/migration/V*.sql; do $PSQL -U postgres -d "$DB" -f "$f"; done
for f in 01_evolucao_schema 02_views_dashboards 03_rbac 04_seed_demo; do
  $PSQL -U postgres -d "$DB" -f "$DIR/$f.sql" > /dev/null
  echo "aplicado: $f.sql"
done
$PSQL -U gerente_matheus -d "$DB" -f "$DIR/05_crud_exemplos.sql" > "$DIR/saida_crud.txt" && echo "executado: 05_crud_exemplos.sql (ROLLBACK)"

FRANCISCO=$(psql -X -At -U postgres -d "$DB" -c "SELECT o.id FROM orders o JOIN app_users u ON u.id=o.seller_id WHERE u.email='franciscoferreira@sbi.tech' AND o.status='PENDING_PAYMENT'")
LOG="$DIR/resultado_testes.txt"; : > "$LOG"
psql -X -q -U postgres        -d "$DB" -f "$DIR/06a_testes_banco.sql"    2>&1 | tee -a "$LOG" >/dev/null
psql -X -q -U vendedor_allan   -d "$DB" -v francisco_pedido="$FRANCISCO" -f "$DIR/06b_testes_vendedor.sql" 2>&1 | tee -a "$LOG" >/dev/null
psql -X -q -U gerente_matheus  -d "$DB" -f "$DIR/06c_testes_gerente.sql"  2>&1 | tee -a "$LOG" >/dev/null

# CC-01: duas vendas simultaneas da ultima unidade (CA-13-01)
SETUP="SELECT set_config('app.user_id',(SELECT id::text FROM app_users WHERE email='matheussiqueira@sbi.tech'),false);"
psql -X -q -U postgres -d "$DB" -c "BEGIN; $SETUP INSERT INTO product(product_type,name,price,weight,sku) VALUES ('PHYSICAL','Ultima unidade',999,1,'ULT-1'); INSERT INTO stock_entry(product_id,quantity,unit_cost) SELECT id,1,500 FROM product WHERE sku='ULT-1'; COMMIT;" >/dev/null
VENDA="SELECT fn_criar_pedido((SELECT min(id) FROM customer), jsonb_build_array(jsonb_build_object('product_id',(SELECT product_id FROM v_catalogo_vendedor WHERE sku='ULT-1'),'quantity',1)),'PIX')"
( psql -X -q -U vendedor_allan  -d "$DB" -c "BEGIN" -c "$VENDA" -c "SELECT pg_sleep(2)" -c "COMMIT" > "$DIR/cc_a.txt" 2>&1; echo "exit=$?" >> "$DIR/cc_a.txt" ) &
sleep 0.5
( psql -X -q -U vendedor_francisco -d "$DB" -c "BEGIN" -c "$VENDA" -c "COMMIT" > "$DIR/cc_b.txt" 2>&1; echo "exit=$?" >> "$DIR/cc_b.txt" ) &
wait
R=$(psql -X -At -U postgres -d "$DB" -c "SELECT reserved||'/'||on_hand||'/'||(SELECT count(*) FROM order_item oi JOIN product p ON p.id=oi.product_id WHERE p.sku='ULT-1') FROM stock_balance b JOIN product p ON p.id=b.product_id WHERE p.sku='ULT-1'")
if [ "$R" = "1/1/1" ] && grep -q "estoque insuficiente" "$DIR/cc_b.txt"; then
  echo "NOTICE:  PASS CC-01 duas reservas simultaneas da ultima unidade: exatamente uma aceita" | tee -a "$LOG" >/dev/null
else
  echo "NOTICE:  FAIL CC-01 resultado reservado/saldo/itens=$R" | tee -a "$LOG" >/dev/null
fi

echo "----"
grep -E "PASS|FAIL|ERROR" "$LOG" | sed 's/^.*NOTICE:  //'
echo "----"
echo "PASS: $(grep -c 'PASS' "$LOG")  FAIL: $(grep -c 'FAIL' "$LOG")  ERROS: $(grep -c 'ERROR' "$LOG" || true)"
