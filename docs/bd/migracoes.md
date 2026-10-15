# BD-02 — migrações PostgreSQL

Atualizado em 04/10/2026 e em 14/10/2026 (ETAPA 2A). Implementação e documentação: **Matheus Siqueira**. Revisão solicitada a Allan no card BD-02.

## Estado e escopo

`V1__legacy_schema.sql` recria as sete tabelas anteriores à autoria de pedidos em um esquema vazio. `V2__users_and_order_authorship.sql` adiciona `app_users`, `orders.seller_id`, índice e FK, preservando `seller_id = NULL` para pedidos antigos. Nenhuma migração insere contas, senhas ou pedidos.

Desde a ETAPA 2A (14/10/2026), o Flyway é a única fonte do esquema em todos os perfis (decisão A2): `spring.flyway.enabled=true`, `out-of-order=false` e `spring.jpa.hibernate.ddl-auto=validate` em `application.properties`, sem `create-drop` em nenhum perfil, runner ou passo da CI. As migrações são:

| Versão | Conteúdo |
| --- | --- |
| V1–V3 | Esquema legado, autoria de pedidos, renomeação client → customer. |
| V4 | Dinheiro em `NUMERIC(12,2)` com conferência do limite antes de arredondar (D020). |
| V5 | Pré-checagem dos dados legados; catálogo, estoque, metas, promoções, papéis, bloqueio de login, auditoria, idempotência e funções `fn_*` ([modelo](modelo-erp-pdv.md)). |
| V6 | 16 views dos painéis, inclusive `v_faturamento_diario`. |
| V6_1, V6_2 (`db/demo`) | Só no perfil `demo`: contas com BCrypt de `SBI_DEMO_PASSWORD` e massa sintética. |
| V11_1 (`db/demo`) | Só no perfil `demo`: a promoção de Acessórios da V6_2 passa a valer até 31/12/2026 (cobre a apresentação de 29/11). Altera só a linha original do seed. |

**Perfil `demo` (A2A-4).** Ele precisa estar ativo desde a primeira migração do banco. Com `out-of-order` desligado, ativá-lo num banco já migrado além da V6 faz o Flyway recusar as versões 6.1 e 6.2; nesse caso o banco é recriado, não completado. Sem `SBI_DEMO_PASSWORD` (12 a 72 bytes), a V6_1 falha e o banco não sobe.

**Testes.** O perfil `integration` usa o mesmo esquema do Flyway. Testes com transação confirmada (`ConcurrencyTest`) e o runner de contrato começam com `TRUNCATE ... RESTART IDENTITY CASCADE` das tabelas de negócio listadas em `config/BusinessTables.java`, nunca de `role` nem de `flyway_schema_history`. As contas sintéticas do contrato exigem `A3_CONTRACT_ISOLATED=1`, `--a3.contract.disposable-database=true` e o banco da porta 15432.

**Banco de desenvolvimento (A2A-2).** Não existe banco não descartável: o `postgres-order` é recriado do zero com Flyway e seed demo, com `pg_dump` antes por precaução, sem baseline manual. Se a pré-checagem da V5 encontrar dado inválido, a migração falha com mensagem clara e nenhum dado é corrigido.

## Reproduzir em banco descartável

1. Definir `TEST_DB_PASSWORD` no terminal e iniciar somente o serviço `postgres-test` de `docker-compose.test.yml` (`docker compose -f docker-compose.test.yml -p a3-tests up -d --wait postgres-test`; `a3-tests` é o nome do projeto Compose, não do serviço).
2. Executar `./mvnw -Pintegration verify` (ou `mvnw.cmd -Pintegration verify` no Windows). `FlywayMigrationTest` cria dois esquemas isolados: um vazio e outro que simula um banco sem histórico Flyway com pedido legado `id=42`. A limpeza de cada esquema ocorre em `finally`.
3. No CI, a aplicação sobe com perfil `migration` em `ci_flyway` e Hibernate valida o esquema V1+V2. O log fica no artefato `target/flyway-ci.log`.

Esses testes verificam migração limpa, baseline explícito, preservação do pedido antigo e rejeição de `seller_id` inexistente. O [CI #22 aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37200555926) executou 17 testes Java (incluindo os dois novos), validou V1/V2 no PostgreSQL 18 e subiu a aplicação com Hibernate `validate` no esquema `ci_flyway`. Eles não equivalem a aprovar a migração de um banco real cujo esquema ainda não foi comparado.

## Adoção em banco existente

1. Exportar backup consistente do banco e ensaiar restauração em cópia. Registrar versão do PostgreSQL, contagens e estrutura (`pg_dump --schema-only`) antes de qualquer alteração.
2. Comparar tabelas, colunas, tipos, nulidade, chaves e índices da cópia com V1 e com [dicionário](dicionario-dados.md). V1 descreve o esquema anterior à autenticação; bancos já alterados por `ddl-auto=update` podem ter `app_users` e `seller_id`. Resolver diferenças identificadas antes de seguir. **Não usar `baseline-on-migrate=true` automaticamente.**
3. Se a cópia for compatível com V1, executar `flyway baseline -baselineVersion=1` explicitamente nela, usando a URL e credenciais da cópia. Executar `flyway migrate` e `flyway validate`; depois subir a API com perfil `migration` e `ddl-auto=validate`.
4. Comparar contagens, IDs, relações, pedidos sem vendedor, usuários e índices antes/depois; testar criação de pedido autenticado e leitura histórica. Registrar comandos, hash do backup e evidências sanitizadas.
5. Somente depois de revisão de Allan e confirmação de restauração repetir no ambiente alvo em janela acordada. O banco normal jamais deve receber `create-drop`.

Para rollback operacional, interromper escritas e restaurar o backup ensaiado. Não executar `flyway clean` nem apagar `app_users` ou `seller_id` manualmente: isso pode apagar autoria e contas. PostgreSQL transaciona a V2, mas uma falha posterior ou uso da aplicação pode criar novos dados que exigem reconciliação antes de restaurar.

## Próximas migrações

- DOM-02: definir `BigDecimal` no Java e precisão/escala de `NUMERIC` antes de converter `price`/`amount`, com relatório de arredondamento dos valores legados.
- BD-02: constraints de quantidade e valores após inspecionar registros atuais; tabelas de estoque/metas somente quando os respectivos modelos forem aprovados.
- SEC-01: projeções e filtros de pedidos por vendedor; esta migração estabelece apenas o vínculo e sua integridade referencial.

Fontes: [Spring Boot 4.1, inicialização do banco](https://docs.spring.io/spring-boot/how-to/data-initialization.html) e [Flyway, baseline](https://documentation.red-gate.com/fd/baseline-277578867.html).
