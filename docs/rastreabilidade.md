# Rastreabilidade RF → CA → CT → evidência

Situação em 14/10/2026, na branch `ci/full-pipeline`, que empilha as etapas 2A a 4. Os requisitos estão em [requisitos.md](requisitos.md), os critérios de aceitação em [criterios-aceitacao.md](criterios-aceitacao.md) e os casos de teste em [plano-testes.md](plano-testes.md).

## Como ler a evidência

- **Java**: a classe e o método ficam em `src/test/java/com/swee/ordermanagementspring/`. Rodam na suíte de integração (`scripts/test.ps1 -Mode Integration`, 544 testes, 0 falhas) e no job `test (backend)` da CI.
- **E2E**: os arquivos ficam em `frontend/e2e/`, com Playwright e axe. Rodam pelo `scripts/test-contract.ps1 -HttpOnly -Frontend` (14 testes, 0 falhas) e no job `frontend` da CI.
- **Lógica do frontend**: `frontend/tests/` (`npm test`, 20 testes).
- **Contrato**: `scripts/live-contract.mjs` (45 operações, 0 divergências), `scripts/http-contract.mjs` e `scripts/http-session.mjs`, no job `contract`.
- **Banco**: `docs/bd/pacote-sql/run_bd.sh`, com 51 PASS no job `bd`.
- **CI de referência**: run `38543403142`, com os quatro jobs verdes (`test (backend)`, `bd`, `contract`, `frontend`) no commit `8fafed2`. A branch foi integrada à `main` e apagada; as rodadas seguintes ficam em `gh run list --branch main`.

## Matriz

| RF | CA | CT | Evidência | Situação |
| --- | --- | --- | --- | --- |
| RF-01 | CA-01-01 | CT-AUTH-001 | `AccessMatrixTest` (anônimo → 401 em toda rota protegida), `SessionSecurityTest` | Verde |
| RF-02 | CA-01-02, CA-01-03 | CT-AUTH-002, CT-AUTH-003 | `AccessMatrixTest` (VENDEDOR → 403 nas rotas do gerente), `ManagementApiTest`, `GoalsApiTest.managementSummary` | Verde |
| RF-03 | CA-03-01, CA-03-02 | CT-CLIENT-001, CT-CLIENT-002 | `CustomersApiTest.invalidCpfIsRejected`, `duplicateDocumentIsRejected`, `CatalogServicesTest` (CustomerService) | Verde |
| RF-04 | CA-04-01, CA-04-02 | CT-PRODUCT-001, CT-PRODUCT-002 | `BackofficeApiTest.productTypeRequiresItsField`, `CatalogServicesTest.decisionTable` | Verde |
| RF-05 | CA-05-01 a CA-05-03 | CT-ORDER-001 a CT-ORDER-003 | `OrderPersistenceTest.demonstrationSalePersistsCustomerAddressPaymentAndTotal`, `OrderServiceRulesTest` (`existingCustomerIsReused`, `newCustomerRules`, `customerIsRequired`) | Verde |
| RF-06 | CA-06-01, CA-06-02 | CT-ORDER-004, CT-SEC-001 | `ProductAndOrderCalculationTest.newOrderRequiresSeller`, `SalesApiTest.unknownPropertiesAreRejected`, `sellerComesOnlyFromSession` | Verde |
| RF-07 | CA-07-01 a CA-07-03 | CT-MONEY-001 a CT-MONEY-003 | `ProductAndOrderCalculationTest` (`totalSumsAllItems`, `itemSubtotalMultipliesQuantityOnce`), `OrderPersistenceTest.historicalUnitPriceSurvivesCatalogChangeAndDatabaseReload`, `PricingTest`, `SalesApiTest.inactiveOrSupplyIsNotSoldAndPriceIsFrozen` | Verde |
| RF-08 | CA-08-01 a CA-08-03 | CT-PAY-001 a CT-PAY-003 | `SalesApiTest.paymentConfirmationIsIdempotent`, `PaymentServiceTest`, `ConcurrencyTest`, `RefundAndAnalyticsApiTest.refunds` | Verde |
| RF-09 | CA-09-01, CA-09-02 | CT-GOAL-001, CT-GOAL-002 | `GoalsApiTest.monthlyGoalsDistribution` | Verde |
| RF-10 | CA-10-01 a CA-10-03 | CT-GOAL-003 a CT-GOAL-005 | `GoalsApiTest.myPerformance` (85,5%; pendente fora do realizado), `noGoalMeansNoPercentage` | Verde |
| RF-11 | CA-11-01, CA-11-02 | CT-PERF-001, CT-SEC-002 | `GoalsApiTest.myPerformance`, `noGoalMeansNoPercentage`; `AccessMatrixTest` | Verde |
| RF-12 | CA-12-01, CA-12-02 | CT-MGMT-001, CT-MGMT-002 | `ManagementApiTest`, `GoalsApiTest.managementSummary`; E2E `manager.spec.ts` (CT-WEB-102) | Verde |
| RF-13 | CA-13-01 a CA-13-03 | CT-STOCK-001 a CT-STOCK-003 | `SalesConcurrencyTest.lastUnitIsSoldOnce`, `SalesApiTest.cancelReleasesReservation`, `paymentConfirmationIsIdempotent`; job `bd` (CC-01) | Verde |
| RF-14 | CA-14-01 a CA-14-03 | CT-COST-001 a CT-COST-003 | `BackofficeApiTest.stockEntries`, `SalesApiTest.catalogListsOnlySellableAvailableProducts` (sem custo), `historicalCostIsKept` | Verde |
| RF-15 | CA-15-01, CA-15-02 | CT-E2E-001, CT-E2E-002 | E2E `sale.spec.ts` (CT-WEB-101, CT-E2E-002) | Verde |
| RF-16 | CA-16-01 a CA-16-03 | CT-UI-001 a CT-UI-003 | E2E `manager.spec.ts` (CT-UI-001), `sale.spec.ts` (CT-WEB-103/CT-UI-002), `session.spec.ts`; `frontend/tests/http.test.ts` | Verde |
| RF-17 | CA-17-01 a CA-17-03 | CT-SEC-101, CT-SEC-103, CT-SEC-114, CT-WEB-103 | `AccessMatrixTest` (221 casos), `MethodSecurityHierarchyTest`, `BackofficeApiTest`; E2E `sale.spec.ts` | Verde |
| RF-18 | CA-18-01 a CA-18-05 | CT-AUTH-101, CT-AUTH-102 | `LoginLockoutPolicyTest`, `LoginLockoutIntegrationTest` | Verde |
| RF-19 | CA-19-01 a CA-19-07 | CT-SALE-101 a 104, CT-STOCK-101 a 107, CT-SEC-110 a 113, CT-SEC-116, CT-WEB-101, CT-WEB-104 | `PricingTest`, `RequestHashTest`, `SalesApiTest`, `SalesConcurrencyTest`; E2E `sale.spec.ts`, `flows.spec.ts` (CT-WEB-107, 108) | Verde |
| RF-20 | CA-20-01 a CA-20-05 | CT-CRM-101, 102, 104, 105, CT-CONTRACT-101 | `DocumentFormatTest`, `CustomersApiTest` | Verde |
| RF-21 | CA-21-01, CA-21-02 | CT-CRM-103, CT-SEC-113 | `CustomersApiTest.historyHidesOtherSellersTotals`; E2E `sale.spec.ts` (histórico do cliente) | Verde |
| RF-22 | CA-22-01 a CA-22-07 | CT-STOCK-108, CT-STOCK-109, CT-SALE-104, CT-WEB-105, CT-WEB-106 | `BackofficeApiTest`; E2E `flows.spec.ts` (CT-WEB-105, 106) | Verde |
| RF-23 | CA-23-01 a CA-23-03 | CT-CONTRACT-102, CT-WEB-102 | `scripts/live-contract.mjs`; E2E `manager.spec.ts` | Verde |

## Requisitos não funcionais com evidência executável

| Item | Evidência |
| --- | --- |
| Formato único de erro (`ErrorResponse` com `code`) | `ErrorContractTest`, `ErrorResponseTest`, `live-contract.mjs` (erros validados com Ajv) |
| Dinheiro NUMERIC(12,2) com HALF_UP | `MoneyValidationTest`, `PricingTest`, `FlywayMigrationTest` (V4) |
| Flyway como única fonte do esquema | `FlywayMigrationTest` (V1 a V7), passo "Verify Flyway schema with Hibernate validation" do job `test (backend)` |
| Número completo de cartão nunca gravado (D022) | `CardNumberStorageTest`, `FlywayMigrationTest.v7MasksLegacyCardNumbers` |
| WCAG 2.2 AA e 320 px | E2E `sale.spec.ts` e `manager.spec.ts` (CT-WEB-109, axe nas telas dos dois perfis) |
| Schemas do frontend iguais ao contrato | `npm run api:check` (CT-CONTRACT-103) no job `frontend` |

## Lacunas conhecidas

- A edição concorrente de cliente é coberta por versão desatualizada (409); duas edições em paralelo de verdade não são simuladas (CT-CRM-104).
- O faturamento foi conferido contra a própria view no teste; não há conferência automática contra o seed demo, que só existe no perfil `demo`.
