# Metricas - R0 - baseline (suite original, antes dos novos testes e correcoes)

Gerado em 2026-10-06T21:35:25.799Z por `node scripts/quality-metrics.mjs` a partir de `target/`.

| Indicador | Valor |
| --- | --- |
| Testes executados | 17 |
| Aprovados / reprovados | 17 / 0 |
| Taxa de aprovacao | 100% |
| Cobertura de instrucoes | 1385/4160 (33.3%) |
| Cobertura de branches | 22/221 (10%) |
| Cobertura de linhas | 349/1070 (32.6%) |
| Complexidade ciclomatica total / media / maxima | 566 / 1.26 / 18 |
| Metodos com complexidade > 10 | 1 |
| Violacoes PMD | 44 |
| Duplicacoes CPD (blocos / linhas) | 9 / 241 |
| Achados SpotBugs | 46 |

## Suites

| Suite | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| FlywayMigrationTest | 2 | 0 | 0 | 0 |
| OrderManagementSpringApplicationTests | 1 | 0 | 0 | 0 |
| ErrorResponseTest | 2 | 0 | 0 | 0 |
| SessionSecurityTest | 5 | 0 | 0 | 0 |
| OrderPersistenceTest | 2 | 0 | 0 | 0 |
| OrderServiceTest | 5 | 0 | 0 | 0 |

## Cobertura por pacote

| Pacote | Instrucoes | Branches |
| --- | ---: | ---: |
| ~.config | 45.4% | 0% |
| ~.entities.enums | 100% | - |
| ~.entities.auth | 100% | - |
| ~.repositories | 0% | - |
| ~.services | 19.1% | 10.2% |
| ~.entities.payment | 15.4% | 0% |
| ~.exceptions | 52% | - |
| ~.entities | 62.1% | 100% |
| ~.dto | 17.5% | 0% |
| ~.security | 95.3% | 100% |
| ~.entities.product | 32.2% | 0% |
| ~.controllers | 47% | 75% |
| ~ | 37.5% | - |
| ~.entities.client | 31.9% | - |

## Metodos mais complexos

| Classe.metodo | Complexidade | Branches cobertos |
| --- | ---: | ---: |
| PaymentService.update | 18 | 0% |
| OrderService.buildPayment | 10 | 18.8% |
| PaymentService.buildPayment | 10 | 0% |
| PaymentResponseDTO.from | 9 | 0% |
| OrderService.buildNewClient | 7 | 27.3% |
| ClientService.buildClient | 7 | 0% |
| PixPayment.processPayment | 7 | 0% |
| ProductService.buildProduct | 6 | 0% |
| ClientService.update | 6 | 0% |
| ContractUsersConfig.lambda$contractUsers$0 | 5 | 0% |

## PMD por regra

- UncommentedEmptyConstructor: 13
- UnnecessaryImport: 7
- UseLocaleWithCaseConversions: 7
- CyclomaticComplexity: 5
- LiteralsFirstInComparisons: 4
- ControlStatementBraces: 3
- UseUtilityClass: 1
- AssignmentInOperand: 1
- UnnecessaryFullyQualifiedName: 1
- PreserveStackTrace: 1
- CognitiveComplexity: 1

## SpotBugs por tipo

- EI_EXPOSE_REP2: 20
- EI_EXPOSE_REP: 14
- DM_CONVERT_CASE: 7
- THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION: 1
- NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: 1
- CT_CONSTRUCTOR_THROW: 1
- DLS_DEAD_LOCAL_STORE: 1
- EQ_DOESNT_OVERRIDE_EQUALS: 1
