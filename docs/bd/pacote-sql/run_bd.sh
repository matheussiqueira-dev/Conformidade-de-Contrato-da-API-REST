#!/usr/bin/env bash
# Job bd: roda 03 (RBAC/RLS), 05 (CRUD), 06a/b/c/d (testes) e a corrida CC-01 sobre um banco que ja
# recebeu V1-V11 e a massa demo pelo Flyway (perfil demo). Falha em qualquer FAIL ou ERROR.
# Uso: CONTAINER=<id do conteiner PostgreSQL> DB=<banco> SUPERUSER=<dono do esquema> ./run_bd.sh
# O psql roda dentro do conteiner (socket local), onde os logins de demonstracao sem senha funcionam.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
CONTAINER="${CONTAINER:?informe CONTAINER}"
DB="${DB:?informe DB}"
SUPERUSER="${SUPERUSER:?informe SUPERUSER}"
EXPECTED_PASS="${EXPECTED_PASS:-60}"
OUT="${OUT:-$DIR/resultado_bd.txt}"

psql_as() { local user="$1"; shift; docker exec -i "$CONTAINER" psql -X -q -U "$user" -d "$DB" "$@"; }

psql_as "$SUPERUSER" -v ON_ERROR_STOP=1 < "$DIR/03_rbac.sql" > /dev/null
echo "aplicado: 03_rbac.sql"
psql_as gerente_matheus -v ON_ERROR_STOP=1 < "$DIR/05_crud_exemplos.sql" > /dev/null
echo "executado: 05_crud_exemplos.sql (ROLLBACK)"

FRANCISCO=$(psql_as "$SUPERUSER" -At -c "SELECT o.id FROM orders o JOIN app_users u ON u.id = o.seller_id WHERE u.email = 'franciscoferreira@sbi.tech' AND o.status = 'PENDING_PAYMENT'")
: > "$OUT"
psql_as "$SUPERUSER" < "$DIR/06a_testes_banco.sql" >> "$OUT" 2>&1 || true
psql_as vendedor_allan -v francisco_pedido="$FRANCISCO" < "$DIR/06b_testes_vendedor.sql" >> "$OUT" 2>&1 || true
psql_as gerente_matheus < "$DIR/06c_testes_gerente.sql" >> "$OUT" 2>&1 || true
psql_as "$SUPERUSER" < "$DIR/06d_testes_evolucao.sql" >> "$OUT" 2>&1 || true

# CC-01: duas vendas simultaneas da ultima unidade; so uma pode reservar.
SETUP="SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'matheussiqueira@sbi.tech'), false);"
psql_as "$SUPERUSER" -v ON_ERROR_STOP=1 -c "BEGIN; $SETUP INSERT INTO product(product_type, name, price, weight, sku) VALUES ('PHYSICAL', 'Ultima unidade', 999, 1, 'ULT-1'); INSERT INTO stock_entry(product_id, quantity, unit_cost) SELECT id, 1, 500 FROM product WHERE sku = 'ULT-1'; COMMIT;" > /dev/null
SALE="SELECT fn_criar_pedido((SELECT min(id) FROM customer), jsonb_build_array(jsonb_build_object('product_id', (SELECT product_id FROM v_catalogo_vendedor WHERE sku = 'ULT-1'), 'quantity', 1)), 'PIX')"
CC_A="$(mktemp)"; CC_B="$(mktemp)"
( psql_as vendedor_allan -c "BEGIN" -c "$SALE" -c "SELECT pg_sleep(2)" -c "COMMIT" > "$CC_A" 2>&1 || true ) &
sleep 0.5
( psql_as vendedor_francisco -c "BEGIN" -c "$SALE" -c "COMMIT" > "$CC_B" 2>&1 || true ) &
wait
RESULT=$(psql_as "$SUPERUSER" -At -c "SELECT reserved || '/' || on_hand || '/' || (SELECT count(*) FROM order_item oi JOIN product p ON p.id = oi.product_id WHERE p.sku = 'ULT-1') FROM stock_balance b JOIN product p ON p.id = b.product_id WHERE p.sku = 'ULT-1'")
if [ "$RESULT" = "1/1/1" ] && grep -q "estoque insuficiente" "$CC_B"; then
  echo "NOTICE:  PASS CC-01 duas reservas simultaneas da ultima unidade: exatamente uma aceita" >> "$OUT"
else
  echo "NOTICE:  FAIL CC-01 reservado/saldo/itens=$RESULT" >> "$OUT"
fi
rm -f "$CC_A" "$CC_B"

grep -E "PASS|FAIL|ERROR" "$OUT" | sed 's/^.*NOTICE:  //'
PASS=$(grep -c "PASS" "$OUT" || true)
FAIL=$(grep -c "FAIL" "$OUT" || true)
ERRORS=$(grep -c "ERROR" "$OUT" || true)
echo "PASS: $PASS  FAIL: $FAIL  ERROS: $ERRORS  (esperado: $EXPECTED_PASS PASS)"
[ "$FAIL" -eq 0 ] && [ "$ERRORS" -eq 0 ] && [ "$PASS" -eq "$EXPECTED_PASS" ]
