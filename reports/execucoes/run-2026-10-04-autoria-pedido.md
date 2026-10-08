# SEC-01 — primeira fatia de autoria do pedido

Data: 04/10/2026. Implementação e documentação: **Matheus Siqueira**. Estado: em andamento; revisão de Gabriel pendente.

## Decisão e implementação

Foram aplicadas as orientações de `domain-modeling` e `migration-strategy` do pacote `cto-toolkit-3.0.0-v2.zip` e de `spring-boot-patterns` e `database-design` do pacote `ultrapowers-dev-1.2.0-v2.zip`. A associação `Order.seller` representa o autor persistido. O controller obtém o principal autenticado; o serviço consulta novamente a conta por ID antes de salvar. O corpo da requisição não escolhe o vendedor.

Arquivos alterados: `Order.java`, `OrderService.java`, `OrderController.java`, `GlobalExceptionHandler.java`, `OrderServiceTest.java`, `OrderPersistenceTest.java` e `ErrorResponseTest.java`. A FK `orders.seller_id` admite nulo para pedidos históricos sem autoria comprovada. Não foi atribuída uma conta fictícia a esses registros. O índice `idx_orders_seller_id` prepara consultas por vendedor. Sessão de conta removida retorna 403 sem expor detalhes internos, em vez de cair no handler genérico 500.

## Verificação executada

- `npm.cmd test`: 23 testes de contrato aprovados, 0 falhas.
- `git diff --check`: sem erros de whitespace antes da publicação do código.
- Maven `test` local: não concluído. JDK 25 falhou com `AccessDeniedException` em `conf/security/java.security`; JDK 27 falhou com `AccessDeniedException` em JARs do cache. A reprodução em pasta temporária de caminho ASCII e com cache Maven copiado apresentou a mesma restrição. A aprovação Java foi obtida no CI descrito abaixo.

### CI da revisão publicada

O [CI #20 do commit `ae74829`](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37198467104) terminou com sucesso. O log registra 15 testes Java, zero falhas/erros/ignorados e `BUILD SUCCESS`: 5 de `OrderServiceTest`, 2 de `OrderPersistenceTest`, 5 de `SessionSecurityTest`, 2 de `ErrorResponseTest` e 1 de contexto. Também passaram 23 testes de contrato, 3 testes de cliente, 12 verificações HTTP e 2 E2E, totalizando 55 verificações de escopos distintos. O CI valida o código desta fatia em PostgreSQL descartável; não valida migração Flyway nem rota de vendedor ainda não implementadas. A falha local acima descreve somente a restrição desta sessão e não substitui o resultado do CI.

## Continuidade e critérios de aceite

1. Build, testes unitários e integração PostgreSQL no CI: aprovados no run #20. Gabriel ainda deve revisar o código e as evidências; execução local depende de ambiente sem o bloqueio descrito acima.
2. Em BD-02, criar migração versionada que adicione FK e índice sem apagar pedidos existentes. Validar instalação limpa, upgrade com pedido antigo e rollback em cópia sintética. Não usar `ddl-auto=update` como prova de migração.
3. Criar endpoint e projeção específicos para vendedor. Com dois vendedores, testar que cada um consulta apenas seus pedidos; gerente mantém acesso permitido sem expor custo/margem ao vendedor.
4. Atualizar contrato OpenAPI executável, testes HTTP e documentação. Gabriel revisa código, migração e evidências antes de mover o card para Em revisão ou Concluído.

Risco atual: a associação JPA pode ser criada pelo `ddl-auto=update` no banco de desenvolvimento, mas não substitui a migração Flyway planejada. Não aplicar esta mudança em banco com dados reais antes da migração e do teste de upgrade.
