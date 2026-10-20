#!/usr/bin/env bash
# Massa de demonstracao Olist: sobe a stack demo, espera a API saudavel (Flyway aplicado), carrega
# 07_carga_olist.sql por psql dentro do conteiner do banco e imprime as contagens. Com --check, roda a carga
# de novo (idempotencia), os testes 09 (CT-DEMO-01 a 04) e a verificacao pela API (CT-DEMO-05).
# Uso, na raiz do repositorio:
#   SBI_DEMO_PASSWORD=<senha comum das contas, ou SBI_DEMO_PASSWORD_<NOME>, ou um .env> SBI_DB_PASSWORD=<senha interna do banco> bash docs/bd/pacote-sql/run_demo_olist.sh [--check]
# Banco acessivel depois: jdbc:postgresql://127.0.0.1:5433/sbi_demo (usuario sbi, senha de SBI_DB_PASSWORD).
# Banco externo (ex.: Neon), sem docker compose: roda 07 (e 09 com --check) com psql do conteiner postgres:18.
# A URL fica so em variavel de ambiente; o Flyway ja deve ter rodado (API no perfil demo apontada para esse banco):
#   export SBI_DATABASE_URL='postgresql://<usuario>:<senha>@<host-sem-pooler>/neondb?sslmode=require'
#   bash docs/bd/pacote-sql/run_demo_olist.sh --database-url "$SBI_DATABASE_URL" [--check]
set -euo pipefail
export MSYS_NO_PATHCONV=1
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT"

CHECK=""
DATABASE_URL=""
while [ $# -gt 0 ]; do
  case "$1" in
    --check) CHECK=1 ;;
    --database-url) DATABASE_URL="${2:?--database-url exige a URL (use a variavel SBI_DATABASE_URL)}"; shift ;;
    *) echo "Opcao desconhecida: $1" >&2; exit 2 ;;
  esac
  shift
done

if [ -n "$DATABASE_URL" ]; then
  # A URL chega ao psql por variavel de ambiente do conteiner (-e sem valor), fora da linha de comando do docker.
  export SBI_PSQL_URL="$DATABASE_URL"
  PACOTE="$ROOT/docs/bd/pacote-sql"
  if command -v cygpath > /dev/null 2>&1; then PACOTE="$(cygpath -w "$PACOTE")"; fi
  psql_ext() { docker run --rm -e SBI_PSQL_URL -v "$PACOTE:/pacote-sql:ro" -w /pacote-sql postgres:18 \
    sh -c 'exec psql "$SBI_PSQL_URL" -X -q "$@"' psql "$@"; }
  schema="$(psql_ext -A -t -c "SELECT to_regclass('public.app_users')")" \
    || { echo "Conexao com o banco externo falhou (confira a URL, o host sem pooler e o sslmode)." >&2; exit 1; }
  if [ -z "$(echo "$schema" | tr -d '[:space:]')" ]; then
    echo "Banco externo sem o esquema: suba antes a API no perfil demo apontada para ele (Flyway)." >&2; exit 1
  fi
  psql_ext -v ON_ERROR_STOP=1 -f 07_carga_olist.sql
  if [ -n "$CHECK" ]; then
    psql_ext -v ON_ERROR_STOP=1 -f 07_carga_olist.sql
    result="$(psql_ext -f 09_testes_olist.sql 2>&1 | grep -E "PASS|FAIL|ERROR" | sed 's/^.*NOTICE:  //')"
    echo "$result"
    if [ -z "$result" ] || echo "$result" | grep -qE "^FAIL |ERROR"; then echo "Verificacao da massa Olist falhou." >&2; exit 1; fi
    echo "CT-DEMO-05 (pela API) nao roda neste modo: use scripts/demo-olist-api.mjs com a URL publica da API."
  fi
  echo "Massa Olist carregada no banco externo. Roteiro SQL: docs/bd/pacote-sql/08_crud_clientes_olist.sql"
  exit 0
fi

# O docker compose tambem le o .env da raiz (fora do git); sem ele, as senhas vem do ambiente.
if [ ! -f .env ]; then
  if [ -z "${SBI_DEMO_PASSWORD:-}" ] && { [ -z "${SBI_DEMO_PASSWORD_MATHEUS:-}" ] || [ -z "${SBI_DEMO_PASSWORD_GABRIEL:-}" ]        || [ -z "${SBI_DEMO_PASSWORD_ALLAN:-}" ] || [ -z "${SBI_DEMO_PASSWORD_FRANCISCO:-}" ]; }; then
    echo "Defina SBI_DEMO_PASSWORD ou SBI_DEMO_PASSWORD_<NOME> de cada conta (ou um .env com elas)." >&2; exit 1
  fi
  : "${SBI_DB_PASSWORD:?Defina SBI_DB_PASSWORD (senha interna do banco de demonstracao)}"
fi
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
if [ -n "$CHECK" ]; then
  psql_db -v ON_ERROR_STOP=1 -f 07_carga_olist.sql
  psql_db -f 09_testes_olist.sql 2>&1 | grep -E "PASS|FAIL|ERROR" | sed 's/^.*NOTICE:  //'
  node scripts/demo-olist-api.mjs http://127.0.0.1:3000/api/backend
fi
echo "Banco: jdbc:postgresql://127.0.0.1:5433/sbi_demo (usuario sbi). Roteiro SQL: docs/bd/pacote-sql/08_crud_clientes_olist.sql"
