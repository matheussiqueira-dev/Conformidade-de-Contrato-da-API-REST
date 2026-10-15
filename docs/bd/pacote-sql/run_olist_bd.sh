#!/usr/bin/env bash
# Etapa separada do job bd (WP-EVO-8): roda DEPOIS de run_bd.sh, para nenhum numero dos testes 06* mudar.
# Carrega a massa Olist duas vezes (CT-DEMO-03), executa o roteiro de CRUD 08 (as tres recusas de integridade sao
# esperadas), os testes 09 (CT-DEMO-01 a 04 e 07) e, com a API no perfil demo, a verificacao CT-DEMO-05.
# Uso: CONTAINER=<id do PostgreSQL> DB=<banco> SUPERUSER=<dono> JAR=<jar da API> JDBC_URL=<url> \
#      SPRING_DATASOURCE_PASSWORD=<senha> SBI_DEMO_PASSWORD=<senha das contas> ./run_olist_bd.sh
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/../../.." && pwd)"
CONTAINER="${CONTAINER:?informe CONTAINER}"
DB="${DB:?informe DB}"
SUPERUSER="${SUPERUSER:?informe SUPERUSER}"
OUT="${OUT:-$DIR/resultado_olist.txt}"
EXPECTED_PASS=11

docker exec "$CONTAINER" rm -rf /tmp/pacote-sql
# No Windows (Git Bash) o docker.exe precisa do caminho no formato do Windows.
SRC="$DIR"; if command -v cygpath > /dev/null 2>&1; then SRC="$(cygpath -w "$DIR")"; fi
docker cp "$SRC" "$CONTAINER:/tmp/pacote-sql"
psql_in() { docker exec -i -w /tmp/pacote-sql "$CONTAINER" psql -X -q -U "$SUPERUSER" -d "$DB" "$@"; }

: > "$OUT"
psql_in -v ON_ERROR_STOP=1 -f 07_carga_olist.sql >> "$OUT" 2>&1
psql_in -v ON_ERROR_STOP=1 -f 07_carga_olist.sql >> "$OUT" 2>&1
CRUD="$(psql_in -f 08_crud_clientes_olist.sql 2>&1)"
if [ "$(grep -c 'ERROR:' <<< "$CRUD")" = "3" ] && grep -q ck_customer_cpf <<< "$CRUD" \
   && grep -q ck_customer_document_type <<< "$CRUD" && grep -q orders_client_id_fkey <<< "$CRUD"; then
  echo "NOTICE:  PASS CT-DEMO-06 roteiro 08: CRUD com as tres recusas esperadas (CHECK de CPF, tipo, FK de pedidos)" >> "$OUT"
else
  echo "NOTICE:  FAIL CT-DEMO-06 roteiro 08 com erro inesperado" >> "$OUT"
  echo "$CRUD" >> "$OUT"
fi
psql_in -f 09_testes_olist.sql >> "$OUT" 2>&1 || true

if [ -n "${JAR:-}" ]; then
  java -jar "$JAR" --spring.profiles.active=demo --server.address=127.0.0.1 --server.port=18081 \
    --spring.datasource.url="${JDBC_URL:?informe JDBC_URL}" --spring.datasource.username="$SUPERUSER" \
    --spring.jpa.show-sql=false > "$DIR/olist-api.log" 2>&1 &
  API_PID=$!
  trap 'kill $API_PID 2>/dev/null || true' EXIT
  for _ in $(seq 1 90); do
    curl -fsS http://127.0.0.1:18081/auth/csrf > /dev/null 2>&1 && break
    sleep 1
  done
  (cd "$ROOT" && node scripts/demo-olist-api.mjs http://127.0.0.1:18081) | grep -E "^(PASS|FAIL) " | sed 's/^/NOTICE:  /' >> "$OUT" || true
fi

grep -E "PASS|FAIL|ERROR" "$OUT" | grep -v "ERROR:  update or delete\|ERROR:  new row" | sed 's/^.*NOTICE:  //'
PASS=$(grep -c "PASS" "$OUT" || true)
FAIL=$(grep -c "FAIL" "$OUT" || true)
ERRORS=$(grep -c "ERROR" "$OUT" || true)
echo "PASS: $PASS  FAIL: $FAIL  ERROS: $ERRORS  (esperado: $EXPECTED_PASS PASS com a API, 7 sem)"
WANT=$EXPECTED_PASS; [ -n "${JAR:-}" ] || WANT=7
[ "$FAIL" -eq 0 ] && [ "$ERRORS" -eq 0 ] && [ "$PASS" -eq "$WANT" ]
