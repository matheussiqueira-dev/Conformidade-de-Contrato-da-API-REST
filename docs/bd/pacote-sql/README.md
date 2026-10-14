# Pacote SQL da disciplina de Banco de Dados

Versão de 12/10/2026, trazida para o repositório na ETAPA 2A (14/10/2026). Decisões: [modelo-erp-pdv.md](../modelo-erp-pdv.md) e [erp-pdv.md](../../decisoes/erp-pdv.md).

| Arquivo | Situação |
| --- | --- |
| `01_evolucao_schema.sql`, `02_views_dashboards.sql` | Referência histórica. A versão executável está em `src/main/resources/db/migration/V5__catalogo_estoque_metas_promocoes.sql` e `V6__views_dashboards.sql`, com as adaptações P1–P9 e D1–D10. |
| `04_seed_demo.sql` | Referência histórica. A versão executável está em `src/main/resources/db/demo/` (`V6_1__DemoAccounts` e `V6_2__demo_seed.sql`, perfil `demo`, CPFs com dígito verificador válido). |
| `03_rbac.sql` | Executado por `psql` (não entra no Flyway: cria logins de banco sem senha para a demonstração de RLS, decisão A4). Concede também `v_faturamento_diario` ao gerente. |
| `05_crud_exemplos.sql`, `06a/06b/06c_testes_*.sql` | Executados por `psql`; os testes 06 não dependem dos CPFs do seed. |
| `06d_testes_evolucao.sql` | Testes da evolução: CT-BD-30 (o gatilho `keep_one_active_manager` da V8 impede rebaixar ou desativar o último GERENTE ativo; cada caso reverte a própria alteração) BD-31 (a view `v_cliente_resumo` da V9 confere com o seed demo e com o faturamento) e BD-32 (alçadas iniciais e CHECKs do desconto manual da V11). |
| `run_all.sh`, `resultado_testes_2026-10-12.txt` | Execução original de 12/10 (51 PASS), sobre o pacote aplicado direto por `psql`. |
| `run_bd.sh` | Execução atual: sobre um banco que já recebeu as migrações (V1–V11) e a massa demo pelo Flyway, roda 03, 05, 06a/b/c/d e a corrida CC-01; falha em qualquer FAIL, ERROR ou contagem de PASS diferente de 60 (51 do pacote original + 3 do CT-BD-30 + 3 do BD-31 + 3 do BD-32). É o job `bd` da CI. |
| `olist/` | Massa de demonstração do histórico de clientes (recorte do Brazilian E-Commerce Public Dataset by Olist, CC BY-NC-SA 4.0): 4 CSVs, `montar_recorte.py` e `NOTICE.md` com fonte, licença e adaptações. |
| `07_carga_olist.sql` | Carga da massa Olist por SQL puro (`\copy` para `stg_olist`, limpeza idempotente, `INSERT ... SELECT`, conferência), em uma transação. Rodar como superusuário, depois do Flyway, a partir desta pasta. |
| `08_crud_clientes_olist.sql` | Roteiro de CRUD e consultas da apresentação sobre a massa Olist, em `BEGIN ... ROLLBACK`; mostra três recusas de integridade. |
| `09_testes_olist.sql` | Testes CT-DEMO-01 a 04 (contagens e soma, cliente com 14 pedidos, idempotência, estoque). |
| `run_demo_olist.sh` | Sobe a stack demo e carrega a massa Olist (`--check`: carga repetida, testes 09 e CT-DEMO-05 pela API). Equivalente PowerShell: `scripts/demo-olist.ps1`. |
| `run_olist_bd.sh` | Etapa separada do job `bd`, depois de `run_bd.sh`: carga duas vezes, roteiro 08 (CT-DEMO-06), testes 09 e CT-DEMO-05 com a API no perfil demo; espera 10 PASS. Detalhes em `docs/bd/massa-demonstracao.md`. |

Reprodução local (Docker ativo, Git Bash):

```bash
export TEST_DB_PASSWORD="$(openssl rand -hex 12)"
docker compose -f docker-compose.test.yml -p a3-bd up -d --wait
./mvnw -q -DskipTests package
SPRING_DATASOURCE_PASSWORD="$TEST_DB_PASSWORD" SBI_DEMO_PASSWORD="<12 ou mais caracteres>" \
  java -jar target/order-management-spring-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo \
  --spring.main.web-application-type=none \
  --spring.datasource.url=jdbc:postgresql://localhost:15432/order_management_test --spring.datasource.username=postgres_test
CONTAINER=a3-bd-postgres-test-1 DB=order_management_test SUPERUSER=postgres_test bash docs/bd/pacote-sql/run_bd.sh
docker compose -f docker-compose.test.yml -p a3-bd down -v
```

As funções e gatilhos de V5/V6 usam `${flyway:defaultSchema}` no `search_path`; por isso as migrações só rodam pelo Flyway, nunca por `psql` direto.
