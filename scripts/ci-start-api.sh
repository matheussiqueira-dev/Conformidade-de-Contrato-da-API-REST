#!/usr/bin/env bash
# Sobe a API isolada da CI (perfil contract, banco descartavel da porta 15432) e espera o /v3/api-docs.
# Uso: scripts/ci-start-api.sh <porta> <arquivo-de-log>. Exige SPRING_DATASOURCE_PASSWORD e A3_TEST_PASSWORD no ambiente.
set -euo pipefail
port="${1:-18080}"
log="${2:-target/api-ci.log}"
java -jar target/order-management-spring-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=contract \
  --server.address=127.0.0.1 --server.port="$port" \
  --spring.datasource.url=jdbc:postgresql://localhost:15432/order_management_test \
  --spring.datasource.username=postgres_test \
  --a3.contract.disposable-database=true \
  --spring.jpa.show-sql=false > "$log" 2>&1 &
echo $! > target/api-ci.pid
for attempt in {1..90}; do
  if ! kill -0 "$(cat target/api-ci.pid)" 2>/dev/null; then
    cat "$log"
    exit 1
  fi
  if curl --fail --silent "http://127.0.0.1:${port}/v3/api-docs" > /dev/null; then
    exit 0
  fi
  sleep 1
done
cat "$log"
exit 1
