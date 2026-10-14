# Metricas - R1 - ETAPA 4 (ERP/PDV)

Gerado em 2026-10-14T09:44:14.002Z por `node scripts/quality-metrics.mjs` a partir de `target/`.

| Indicador | Valor |
| --- | --- |
| Testes executados | 544 |
| Aprovados / reprovados | 544 / 0 |
| Taxa de aprovacao | 100% |
| Cobertura de instrucoes | 13320/14310 (93.1%) |
| Cobertura de branches | 598/743 (80.5%) |
| Cobertura de linhas | 2168/2398 (90.4%) |
| Complexidade ciclomatica total / media / maxima | 1300 / 1.42 / 18 |
| Metodos com complexidade > 10 | 2 |
| Violacoes PMD | 105 |
| Duplicacoes CPD (blocos / linhas) | 18 / 253 |
| Achados SpotBugs | 83 |

## Suites

| Suite | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| BackofficeApiTest | 10 | 0 | 0 | 0 |
| CustomersApiTest | 10 | 0 | 0 | 0 |
| DocumentFormatTest | 13 | 0 | 0 | 0 |
| GoalsApiTest | 4 | 0 | 0 | 0 |
| ManagementApiTest | 4 | 0 | 0 | 0 |
| RefundAndAnalyticsApiTest | 3 | 0 | 0 | 0 |
| PromotionsApiTest | 3 | 0 | 0 | 0 |
| PricingTest | 9 | 0 | 0 | 0 |
| RequestHashTest | 2 | 0 | 0 | 0 |
| SalesApiTest | 29 | 0 | 0 | 0 |
| SalesConcurrencyTest | 3 | 0 | 0 | 0 |
| DatasourcePasswordGuardTest | 6 | 0 | 0 | 0 |
| MethodSecurityHierarchyTest | 3 | 0 | 0 | 0 |
| MoneyValidationTest | 13 | 0 | 0 | 0 |
| DocumentsTest | 5 | 0 | 0 | 0 |
| OrderStatusTest | 36 | 0 | 0 | 0 |
| OrderFulfillmentTest | 2 | 0 | 0 | 0 |
| PaymentProcessingTest | 17 | 0 | 0 | 0 |
| ProductAndOrderCalculationTest | 19 | 0 | 0 | 0 |
| DataIntegrityMappingTest | 10 | 0 | 0 | 0 |
| FlywayMigrationTest | 9 | 0 | 0 | 0 |
| OrderManagementSpringApplicationTests | 1 | 0 | 0 | 0 |
| AccessMatrixTest | 221 | 0 | 0 | 0 |
| ErrorContractTest | 6 | 0 | 0 | 0 |
| ErrorResponseTest | 2 | 0 | 0 | 0 |
| LoginLockoutIntegrationTest | 3 | 0 | 0 | 0 |
| LoginLockoutPolicyTest | 4 | 0 | 0 | 0 |
| SessionSecurityTest | 5 | 0 | 0 | 0 |
| CardNumberStorageTest | 1 | 0 | 0 | 0 |
| CatalogServicesTest | 23 | 0 | 0 | 0 |
| ConcurrencyTest | 2 | 0 | 0 | 0 |
| OrderPersistenceTest | 2 | 0 | 0 | 0 |
| OrderServiceRulesTest | 31 | 0 | 0 | 0 |
| OrderServiceTest | 5 | 0 | 0 | 0 |
| PaymentServiceTest | 26 | 0 | 0 | 0 |
| DemoAccountsTest | 2 | 0 | 0 | 0 |

## Cobertura por pacote

| Pacote | Instrucoes | Branches |
| --- | ---: | ---: |
| ~.config | 88.9% | 55.9% |
| ~.api.sales | 98.3% | 90.3% |
| ~.entities.enums | 100% | 100% |
| ~.entities.auth | 100% | 100% |
| ~.repositories | 0% | - |
| ~.entities.customer | 89.1% | 100% |
| ~.services | 83.2% | 72.3% |
| ~.entities.payment | 87.7% | 100% |
| ~.entities | 81.5% | 83.3% |
| ~.exceptions | 87.9% | 82.5% |
| ~.dto | 86.1% | 52.8% |
| ~.api.management | 99.9% | 93.2% |
| ~.api.promotions | 99% | 86.7% |
| ~.security | 93% | 85.2% |
| ~.api.common | 100% | 92.9% |
| ~.entities.product | 89.1% | 100% |
| ~.api.backoffice | 98.3% | 85.1% |
| ~.entities.audit | 83.3% | - |
| ~.api.customers | 99.3% | 86.5% |
| ~.controllers | 71.1% | 43.5% |
| db.demo | 55.7% | 80% |
| ~ | 37.5% | - |

## Metodos mais complexos

| Classe.metodo | Complexidade | Branches cobertos |
| --- | ---: | ---: |
| PaymentService.update | 18 | 46.9% |
| ApiErrorController.error | 12 | 29.4% |
| SalesService.create | 10 | 94.4% |
| OrderService.buildPayment | 10 | 81.3% |
| PaymentService.buildPayment | 10 | 62.5% |
| GlobalExceptionHandler.handleDataIntegrityViolation | 10 | 94.4% |
| PaymentResponseDTO.from | 9 | 64.3% |
| DocumentFormat.of | 9 | 93.8% |
| ManagementService.revenue | 8 | 100% |
| StockService.adjust | 8 | 92.9% |

## PMD por regra

- ControlStatementBraces: 44
- UncommentedEmptyConstructor: 14
- CyclomaticComplexity: 12
- UnnecessaryImport: 9
- UnusedFormalParameter: 5
- UseLocaleWithCaseConversions: 4
- MissingOverride: 3
- MissingStaticMethodInNonInstantiatableClass: 2
- LambdaCanBeMethodReference: 2
- LiteralsFirstInComparisons: 2
- UseUtilityClass: 1
- SimplifyBooleanReturns: 1
- GuardLogStatement: 1
- BigIntegerInstantiation: 1
- UnnecessaryFullyQualifiedName: 1
- PreserveStackTrace: 1
- CognitiveComplexity: 1
- ClassNamingConventions: 1

## SpotBugs por tipo

- EI_EXPOSE_REP2: 40
- EI_EXPOSE_REP: 32
- DM_CONVERT_CASE: 4
- NM_CONFUSING: 1
- THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION: 1
- CT_CONSTRUCTOR_THROW: 1
- SE_TRANSIENT_FIELD_NOT_RESTORED: 1
- BC_UNCONFIRMED_CAST: 1
- EQ_DOESNT_OVERRIDE_EQUALS: 1
- NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: 1
