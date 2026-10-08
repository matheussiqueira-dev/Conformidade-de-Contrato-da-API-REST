# BD-02 — migrações PostgreSQL

Atualizado em 04/10/2026. Implementação e documentação: **Matheus Siqueira**. Revisão solicitada a Allan no card BD-02.

## Estado e escopo

`V1__legacy_schema.sql` recria as sete tabelas anteriores à autoria de pedidos em um esquema vazio. `V2__users_and_order_authorship.sql` adiciona `app_users`, `orders.seller_id`, índice e FK, preservando `seller_id = NULL` para pedidos antigos. Nenhuma migração insere contas, senhas ou pedidos.

O perfil `migration` ativa Flyway e coloca Hibernate em `validate`. O perfil normal continua em `ddl-auto=update` com Flyway desativado até que cada banco existente seja auditado. O perfil `integration` mantém `create-drop` em PostgreSQL descartável. Não combinar `migration` e `integration` no mesmo processo.

## Reproduzir em banco descartável

1. Iniciar somente o serviço `a3-tests` de `docker-compose.test.yml`.
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
