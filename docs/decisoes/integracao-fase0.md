# Decisões de integração — Fase 0

Data: 13/10/2026; integração concluída em 14/10/2026. Responsável: Matheus Siqueira.

## Decisões

| ID | Decisão |
| --- | --- |
| INT-01 | Cada branch é atualizada com `git merge origin/main` (sem rebase nem `push --force`) e entra na `main` por **Squash and merge** com título em Conventional Commits, o que descarta os commits de merge e os corpos longos sem reescrever histórico publicado. |
| INT-02 | ~~Ordem de merge: #6 → #5 → #8 → #9 → #10 → #7 → artefatos da rubrica.~~ Substituída pela INT-06. |
| INT-03 | A migração de dinheiro da DOM-02 passa a ser `V4__money_numeric.sql`, porque a `main` já publicou `V3__rename_client_to_customer.sql`; nenhuma migração publicada foi editada. |
| INT-04 | A exclusão `known-defect` do Surefire saiu com a DOM-02, que entrou na `main` dentro do #10: nenhum teste da `main` tem essa tag, e a suíte de integração roda sem grupo excluído. |
| INT-05 | `ClientException` da FIX-02 foi renomeada para `CustomerException` (rótulo `Customer Error`) para seguir o rename client → customer já publicado na `main`. |
| INT-06 | O #9 (DOM-02) foi fechado sem merge, com o comentário "Substituído pelo #10": a branch do #10 (FIX-03) contém todos os commits da DOM-02 e reescreve a `V4__money_numeric.sql` com a pré-checagem D020, e integrar os dois publicaria a V4 de 6 linhas para depois editá-la. Antes do squash, a base do #10 passou de `feat/DOM-02-bigdecimal` para `main`. |

## Ordem final (14/10/2026)

| Ordem | PR | Título do squash | Commit na `main` |
| --- | --- | --- | --- |
| 1 | #6 | `chore: read database passwords from environment variables` | `5bc0736` |
| 2 | #5 | `fix(payment): correct payment defects D003, D004, D006, D012 and D016` | `192f068` |
| 3 | #8 | `fix(order): fix status regression, missing deletion target and customer error (D010, D011, D008)` | `a5e3b51` |
| 4 | #10 | `feat: migrate money to numeric and fix cash payment validation` | `ac064e4` |
| 5 | #7 | `docs(gestao): add remaining-work plan for the A3 project` | `dcf270a` |
| 6 | #11 | `docs(qualidade): add quality plan, risk analysis, test catalog and defect report` | `549d5b3` |
| 7 | #12 | `docs(erp-pdv): model data, access matrix and v4 contract draft` | último da sequência (este registro entra nele) |

Cada branch que ficou atrás da `main` recebeu `git merge origin/main`, a suíte da área tocada e uma nova execução da CI antes do squash.

## Conflitos resolvidos

- **#10.** Conflitos em `PaymentRules.java`, `PaymentService.java`, `OrderServiceRulesTest.java` e `PaymentServiceTest.java`. A branch já continha os commits originais do FIX-01 e do FIX-02, que entraram na `main` em squash; em todos os trechos, a diferença entre os lados era só a evolução DOM-02/FIX-03 (BigDecimal, D018, D019). Ficou a versão do #10.
- **#10, conflito semântico.** Depois do merge, o `FlywayMigrationTest` não compilava: o #6 trocou a constante `PASSWORD` por `password()` (lida de `TEST_DB_PASSWORD`), e os testes novos do D020 ainda usavam a constante. Corrigido em `81f5f8d`.
- **#5, #8, #7, #11 e #12.** Merge da `main` sem conflito.

## Commits fora do padrão da branch `a3-contract-validation` (C6)

Os cinco commits com prefixo fora do Conventional Commits não serão integrados: o conteúdo de cada um já está na `main` com patch idêntico (`git cherry` marca os cinco como equivalentes) e mensagem em Conventional Commits.

| Commit | O que muda | Equivalente na `main` |
| --- | --- | --- |
| `dbea3c0` derive order seller from authenticated session | `Order.seller` derivado da sessão em `OrderController`/`OrderService`; testes de serviço e persistência; card SEC-01 | `18c536c` feat: derive order seller from authenticated session |
| `ae74829` map stale order session to forbidden | Handler de sessão obsoleta → 403 em `GlobalExceptionHandler`; teste em `ErrorResponseTest` | `e2cf4e8` fix: map stale order session to forbidden |
| `97737c8` record verified order authorship CI evidence | Evidência de CI em `reports/execucoes/run-2026-10-04-autoria-pedido.md` e cards | `92ad522` docs: record order authorship ci evidence |
| `dd3e511` introduce opt-in Flyway baseline and seller migration | `V1__legacy_schema.sql`, `V2__users_and_order_authorship.sql`, perfil `migration`, passo de CI e `FlywayMigrationTest` | `853de17` feat: add opt-in flyway baseline and seller migration |
| `ce7de24` record verified Flyway CI evidence | Evidência em `reports/execucoes/run-2026-10-04-bd-02.md` e cards | `6cb7ecc` docs: record flyway ci evidence |

Proposta: não reaplicar nada (já reaplicado). A branch `a3-contract-validation` só é apagada com aprovação da equipe. O commit `7fe1779`, com o mesmo prefixo, já está publicado na `main` e permanece, porque corrigir a mensagem exigiria reescrever histórico.

### Exceções da varredura de termos (14/10/2026)

A varredura de nomes de ferramenta nos arquivos versionados deixa de fora, de propósito, quatro trechos. Nenhum deles é alterado:

| Arquivo | Trecho | Motivo |
| --- | --- | --- |
| `docs/referencias/Manual-Projeto-A3.md` (e o `.pdf`) | Orientação sobre código auxiliado | Cópia fiel do manual da disciplina |
| `docs/gestao/plano-restante-2026-10-07.md` | Tema da aula 19 (segurança e ética) | Título do conteúdo da disciplina |
| `reports/execucoes/run-2026-10-04-bd-02.md` | Mensagem original do `dd3e511` | Cita o prefixo antigo do commit da tabela acima |
| `reports/gestao/sincronizacao-2026-10-04.md` | Prefixo dos commits daquela sincronização | Mesmo prefixo antigo, registrado como fato histórico |

Nas exportações do quadro em `reports/gestao/` (`trello-2026-10-04-*.json` e `historico/revisao-2026-10-01/*.json`), os nomes de produto foram trocados por "ferramenta externa" e o JSON continua válido.

## Pendente

- `docs/decisoes/regras-loja.md` e a DOM-02 usam arredondamento `HALF_UP` (confirmado em 04/10); a arquitetura alvo pede `HALF_EVEN`. A troca, se aprovada, entra na Fase 1 ou 3 com testes próprios.
- Apagar a branch `feat/DOM-02-bigdecimal` depende de confirmação.
- Respostas usadas na ETAPA 2A: [erp-pdv.md](erp-pdv.md#respostas-para-a-etapa-2a-14102026).

## Integração das etapas 2A a 5 (14/10/2026)

O responsável, Matheus Siqueira, revisou os PRs e autorizou a integração em 14/10/2026. A integração foi feita por squash merge, na ordem da pilha e um PR por vez. A mensagem de cada commit é o título do PR, sem corpo.

| Ordem | PR | Branch | Commit na `main` |
| --- | --- | --- | --- |
| 1 | #13 `feat(db): add V5/V6 migrations, demo seed and Flyway-only schema` | `feat/db-v5-v6` | `d33132b` |
| 2 | #14 `feat(security): enforce role hierarchy, login lockout and coded errors` | `feat/security-error-contract` | `d658f03` |
| 3 | #15 `feat(api): implement every v4 operation with live contract verification` | `feat/business-endpoints` | `710294b` |
| 4 | #16 `feat(web): build the typed SBI frontend for point of sale, customers and management` | `feat/web-sbi` | `df07ae3` |
| 5 | #17 `ci: run backend, database, contract and frontend jobs and trace requirements to evidence` | `ci/full-pipeline` | `9018249` |
| 6 | #18 `docs: deliver the SBI with one-command demo, architecture, demo script and final report` | `docs/final-delivery` | `d34570b` |

Depois de cada squash, o PR seguinte passou a ter a `main` como base (`gh pr edit --base main`) e recebeu `git merge origin/main`, sem rebase:

| PR | Merge da `main` |
| --- | --- |
| #14 | `c7229a6` |
| #15 | `7c12b05` |
| #16 | `ee076db` |
| #17 | `8f5c2bb` |
| #18 | `40df8ba` |

O merge do #14 deu conflito, porque o squash e os commits originais do #13 alteravam o mesmo trecho. A resolução usou a estratégia `ours`, depois de conferir duas condições:
- a árvore da `main` era idêntica (`git diff` vazio) à de um commit que já estava na branch;
- esse commit era ancestral da branch (`git merge-base --is-ancestor`).

Os merges do #15 ao #18 usaram a mesma estratégia e a mesma conferência. Nenhum desses merges alterou a árvore da branch. Cada PR só foi integrado com a CI verde (push e pull_request) e o estado `MERGEABLE`/`CLEAN`.

Antes da integração entraram três correções, cada uma propagada por merge para as branches de cima:
- `ab7e79d` tirou dos XML do Surefire versionados as propriedades da JVM (caminhos locais);
- `6c0cf43` trouxe o `SignedMoney` no contrato v4, para líquido, lucro e realizado negativos;
- `82fb022` regenerou os schemas Zod.

O commit `c5694db` da `ci/full-pipeline`, que publicava esse ambiente, foi conferido: só tinha propriedades da JVM (caminhos, versões, nome do usuário), sem senha, token ou segredo. O squash não o levou para a `main`.

Verificação da `main` em `d34570b`:
- CI run `38558059579` (push): `test (backend)`, `bd`, `contract` e `frontend` com success;
- `git ls-tree`: migrações V1 a V7 em `db/migration`, sem versão repetida, e o seed demo `V6_1`/`V6_2` em `db/demo`;
- `.\mvnw.cmd -q clean verify`: exit 0, 233 testes, 0 falhas;
- `npm test` na raiz: 36/36;
- `npm test` em `frontend/`: 20/20.

Nenhuma branch foi apagada.

