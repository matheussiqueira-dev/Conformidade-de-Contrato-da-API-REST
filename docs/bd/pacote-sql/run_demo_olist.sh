#!/usr/bin/env bash
# Massa de demonstracao Olist: sobe a stack demo, espera a API saudavel (Flyway aplicado), carrega
# 07_carga_olist.sql por psql dentro do conteiner do banco e imprime as contagens. Com --check, roda a carga
# de novo (idempotencia), os testes 09 (CT-DEMO-01 a 04) e a verificacao pela API (CT-DEMO-05).
# Uso, na raiz do repositorio:
#   SBI_DEMO_PASSWORD=<senha das contas> SBI_DB_PASSWORD=<senha interna do banco> bash docs/bd/pacote-sql/run_demo_olist.sh [--check]
# Banco acessivel depois: jdbc:postgresql://127.0.0.1:5433/sbi_demo (usuario sbi, senha de SBI_DB_PASSWORD).
set -euo pipefail
export MSYS_NO_PATHCONV=1
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT"
: "${SBI_DEMO_PASSWORD:?Defina SBI_DEMO_PASSWORD (senha das contas de demonstracao)}"
: "${SBI_DB_PASSWORD:?Defina SBI_DB_PASSWORD (senha interna do banco de demonstracao)}"
COMPOSE=(docker compose -f docker-compose.demo.yml -p sbi-demo)

"${COMPOSE[@]}" up --build -d --wait
# Banco recriado sem a API (ex.: compose alterado): a API e recriada para o Flyway rodar de novo.
if [ -z "$("${COMPOSE[@]}" exec -T db psql -X -A -t -U sbi -d sbi_demo -c "SELECT to_regclass('public.app_users')" | tr -d '[:space:]')" ]; then
  "${COMPOSE[@]}" up -d --force-recreate --wait api
fi
"${COMPOSE[@]}" exec -T db rm -rf /tmp/pacote-sql
"${COMPOSE[@]}" cp docs/bd/pacote-sql db:/tmp/pacote-sql
psql_db() { "${COMPOSE[@]}" exec -T -w /tmp/pacote-sql db psql -X -q -U sbi -d sbi_demo "$@"; }

psql_db -v ON_ERROR_STOP=1 -f 07_carga_olist.sql
if [ "${1:-}" = "--check" ]; then
  psql_db -v ON_ERROR_STOP=1 -f 07_carga_olist.sql
  psql_db -f 09_testes_olist.sql 2>&1 | grep -E "PASS|FAIL|ERROR" | sed 's/^.*NOTICE:  //'
  node scripts/demo-olist-api.mjs http://127.0.0.1:3000/api/backend
fi
echo "Banco: jdbc:postgresql://127.0.0.1:5433/sbi_demo (usuario sbi). Roteiro SQL: docs/bd/pacote-sql/08_crud_clientes_olist.sql"
