# SEC-01 — primeira fatia de autoria do pedido

Data: 04/10/2026. Implementação e documentação: **Matheus Siqueira**. Estado: em andamento; revisão de Gabriel pendente.

## Decisão e implementação

Foram aplicadas as orientações de `domain-modeling` e `migration-strategy` do pacote `cto-toolkit-3.0.0-v2.zip` e de `spring-boot-patterns` e `database-design` do pacote `ultrapowers-dev-1.2.0-v2.zip`. A associação `Order.seller` representa o autor persistido. O controller obtém o principal autenticado; o serviço consulta novamente a conta por ID antes de salvar. O corpo da requisição não escolhe o vendedor.

Arquivos alterados: `Order.java`, `OrderService.java`, `OrderController.java`, `OrderServiceTest.java` e `OrderPersistenceTest.java`. A FK `orders.seller_id` admite nulo para pedidos históricos sem autoria comprovada. Não foi atribuída uma conta fictícia a esses registros. O índice `idx_orders_seller_id` prepara consultas por vendedor.

## Verificação executada

- `npm.cmd test`: 23 testes de contrato aprovados, 0 falhas.
- `git diff --check`: executar na revisão final deste commit.
- Maven `test` local: não concluído. JDK 25 falhou com `AccessDeniedException` em `conf/security/java.security`; JDK 27 falhou com `AccessDeniedException` em JARs do cache. A reprodução em pasta temporária de caminho ASCII e com cache Maven copiado apresentou a mesma restrição. Portanto, testes Java novos ainda não estão aprovados.

## Continuidade e critérios de aceite

1. Rodar build, testes unitários e integração PostgreSQL no CI; corrigir qualquer falha antes de solicitar revisão. A execução local requer ambiente sem o bloqueio de acesso descrito acima.
2. Em BD-02, criar migração versionada que adicione FK e índice sem apagar pedidos existentes. Validar instalação limpa, upgrade com pedido antigo e rollback em cópia sintética. Não usar `ddl-auto=update` como prova de migração.
3. Criar endpoint e projeção específicos para vendedor. Com dois vendedores, testar que cada um consulta apenas seus pedidos; gerente mantém acesso permitido sem expor custo/margem ao vendedor.
4. Atualizar contrato OpenAPI executável, testes HTTP e documentação. Gabriel revisa código, migração e evidências antes de mover o card para Em revisão ou Concluído.

Risco atual: a associação JPA pode ser criada pelo `ddl-auto=update` no banco de desenvolvimento, mas não substitui a migração Flyway planejada. Não aplicar esta mudança em banco com dados reais antes da migração e do teste de upgrade.
