# Decisões de integração — Fase 0

Data: 13/10/2026. Responsável: Matheus Siqueira.

## Decisões

| ID | Decisão |
| --- | --- |
| INT-01 | Cada branch é atualizada com `git merge origin/main` (sem rebase nem `push --force`) e entra na `main` por **Squash and merge** com título em Conventional Commits, o que descarta os commits de merge e os corpos longos sem reescrever histórico publicado. |
| INT-02 | Ordem de merge: #6 (credenciais) → #5 (FIX-01) → #8 (FIX-02) → #9 (DOM-02) → #10 (FIX-03) → #7 (plano restante) → artefatos da rubrica; a FIX-03 foi criada sobre a DOM-02, por isso o #10 tem base `feat/DOM-02-bigdecimal` e só pode entrar depois do #9. |
| INT-03 | A migração de dinheiro da DOM-02 passa a ser `V4__money_numeric.sql`, porque a `main` já publicou `V3__rename_client_to_customer.sql`; nenhuma migração publicada foi editada. |
| INT-04 | A exclusão `known-defect` do Surefire sai no #9: com #5 e #8 incluídos não resta nenhum teste com essa tag, e a suíte de integração passa sem grupo excluído. |
| INT-05 | `ClientException` da FIX-02 foi renomeada para `CustomerException` (rótulo `Customer Error`) para seguir o rename client → customer já publicado na `main`. |

## Commits `autofix:` da branch `a3-contract-validation` (C6)

Os cinco commits não serão integrados: o conteúdo de cada um já está na `main` com patch idêntico (`git cherry` marca os cinco como equivalentes) e mensagem em Conventional Commits.

| Commit `autofix:` | O que muda | Equivalente na `main` |
| --- | --- | --- |
| `dbea3c0` derive order seller from authenticated session | `Order.seller` derivado da sessão em `OrderController`/`OrderService`; testes de serviço e persistência; card SEC-01 | `18c536c` feat: derive order seller from authenticated session |
| `ae74829` map stale order session to forbidden | Handler de sessão obsoleta → 403 em `GlobalExceptionHandler`; teste em `ErrorResponseTest` | `e2cf4e8` fix: map stale order session to forbidden |
| `97737c8` record verified order authorship CI evidence | Evidência de CI em `reports/execucoes/run-2026-10-04-autoria-pedido.md` e cards | `92ad522` docs: record order authorship ci evidence |
| `dd3e511` introduce opt-in Flyway baseline and seller migration | `V1__legacy_schema.sql`, `V2__users_and_order_authorship.sql`, perfil `migration`, passo de CI e `FlywayMigrationTest` | `853de17` feat: add opt-in flyway baseline and seller migration |
| `ce7de24` record verified Flyway CI evidence | Evidência em `reports/execucoes/run-2026-10-04-bd-02.md` e cards | `6cb7ecc` docs: record flyway ci evidence |

Proposta: não reaplicar nada (já reaplicado). A branch `a3-contract-validation` só é apagada com aprovação da equipe. O commit `7fe1779` (prefixo `autofix:`) já está publicado na `main` e permanece, porque corrigir a mensagem exigiria reescrever histórico.

## Pendente para a equipe

- `docs/decisoes/regras-loja.md` e a DOM-02 usam arredondamento `HALF_UP` (confirmado em 04/10); a arquitetura alvo pede `HALF_EVEN`. A troca, se aprovada, entra na Fase 1 ou 3 com testes próprios.
