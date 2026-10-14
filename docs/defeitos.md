# Relatório de defeitos

Versão 0.1 (rascunho), 09/10/2026. Preparado por Matheus Siqueira para o card 16. **Responsável: Allan. Revisor: Matheus.** Formato: [templates/defeito.md](templates/defeito.md), com os 13 campos da seção 15 do enunciado.

Fontes: card 16 do Trello (D003–D016, registrados em 08/10), revisão dos PRs #6, #8 e #9 (D017–D021), relatórios em `reports/execucoes/`. Severidade = impacto; prioridade = urgência (escalas no template).

## Resumo

| ID | Título | Severidade | Prioridade | Status | Correção |
| --- | --- | --- | --- | --- | --- |
| H-01 | Quantidade multiplicada duas vezes no total do pedido | Crítica | Alta | Corrigido e retestado | `main` (03/10) |
| H-02 | Erro genérico devolvia HTTP 500 com `status: 400` no corpo | Média | Média | Corrigido e retestado | `main` (04/10) |
| D003 | Boleto vencido marca o pedido como PAID | Alta | Alta | Corrigido, aguardando merge | PR #5 |
| D004 | Pagamento não PENDING é processado de novo | Alta | Alta | Corrigido, aguardando merge | PR #5 |
| D006 | Boleto sem vencimento aceito; processamento lança NullPointerException | Alta | Alta | Corrigido, aguardando merge | PR #5 |
| D008 | Cliente sem CPF/CNPJ lança `ProductException` | Baixa | Baixa | Corrigido, aguardando merge | PR #8 |
| D010 | Status do pedido regride (ex.: DELIVERED → PENDING_PAYMENT) | Alta | Alta | Corrigido, aguardando merge | PR #8 |
| D011 | DELETE de recurso inexistente não retorna 404 | Média | Média | Corrigido, aguardando merge | PR #8 |
| D012 | Pagamento com valor diferente do total do pedido é aceito | Crítica | Alta | Corrigido, aguardando merge | PR #5 |
| D016 | Tipo de pagamento depende do locale da JVM | Média | Média | Corrigido, aguardando merge | PR #5 |
| D017 | Valor abaixo de meio centavo é aceito e gravado como 0,00 | Alta | Alta | Corrigido em branch local | `fix/FIX-03` |
| D018 | Pagamento tardio volta pedido enviado/entregue para PAID | Alta | Alta | Corrigido em branch local | `fix/FIX-03` |
| D019 | Requisições simultâneas passam pela validação de status | Média | Alta | Corrigido em branch local | `fix/FIX-03` |
| D020 | Migração V3 arredonda antes de conferir o limite da coluna | Baixa | Média | Corrigido em branch local | `fix/FIX-03` |
| D021 | Senha do banco ausente gera erro de autenticação confuso | Baixa | Média | Corrigido em branch local | PR #6 (commit novo) |
| D022 | Rota legada de pagamento grava o número completo do cartão | Alta | Média | Aberto | ETAPA 2C |

IDs D001, D002, D005, D007, D009 e D013–D015 não aparecem no repositório nem na descrição do card 16. **Allan deve conferir no quadro** se foram descartados (duplicata, erro de teste) e registrar o motivo aqui. H-01 e H-02 são defeitos de 03–04/10 que nunca receberam ID; se D001/D002 estiverem livres, podem ser atribuídos a eles.

Nenhum defeito está **Encerrado**: o encerramento exige correção na `main`, regressão verde e reteste por quem não implementou (template, seção "Ciclo exigido").

## Métricas (09/10/2026)

| Indicador | Valor | Observação |
| --- | --- | --- |
| Defeitos encontrados | 15 | 13 com ID + 2 históricos |
| Por severidade | Crítica 2 · Alta 6 · Média 4 · Baixa 3 | |
| Corrigidos (código pronto) | 15 | 2 na `main`, 8 em PR aberto, 5 em branch local |
| Encerrados | 0 | Falta merge e/ou reteste independente |
| Abertos sem correção | 0 | |
| Densidade | 6,2 defeitos/KLOC | 15 ÷ 2,43 KLOC (linhas de `src/main/java` sem branco, comentário, `import` e `package`, branch FIX-03) |
| Origem | 8 por testes novos (07/10), 5 por revisão de código dos PRs, 2 por testes de caso | Revisão de código encontrou 1/3 dos defeitos |

Interpretação: a densidade alta é esperada num sistema legado que nunca teve testes de regra. A concentração em pagamento (8 de 15) confirma o risco R01 da [análise de risco](analise-risco.md). A revisão dos PRs achou 5 defeitos que a suíte não pegava (fronteira de meio centavo, concorrência, migração). Por isso a inspeção formal (card 08) vale o esforço.

## Registros

### H-01 — Quantidade multiplicada duas vezes no total do pedido

| Campo | Valor |
| --- | --- |
| ID | H-01 (sem ID no quadro) |
| Título | Quantidade multiplicada duas vezes no total do pedido |
| Descrição | `OrderService.buildItems` gravava `preço × quantidade` como preço do item, e `OrderItem.subTotal()` multiplicava de novo. Viola RF-07 / CA-07-02. |
| Passos para reprodução | 1. Produto de R$ 100,00. 2. Criar pedido com quantidade 2. 3. Ler o preço do item e o total. |
| Resultado esperado | Preço do item R$ 100,00; total R$ 200,00. |
| Resultado obtido | Preço do item R$ 200,00; total R$ 400,00 (`expected: 100.0 but was: 200.0`). |
| Severidade | Crítica |
| Prioridade | Alta |
| Evidência | `reports/execucoes/run-2026-10-03-ct-money-002.md`; teste `CT-MONEY-002` (`ProductAndOrderCalculationTest.itemSubtotalMultipliesQuantityOnce`, `OrderServiceTest`) |
| Responsável | Correção e reteste em 03/10: Matheus. Reteste independente: Allan. |
| Status | Corrigido e retestado; encerramento aguarda reteste independente |
| Correção | `OrderService.buildItems` passa o preço unitário; a multiplicação fica só em `OrderItem.subTotal()` |
| Reteste | 03/10: `mvnw test` 2/2 aprovados; teste mantido na suíte (176 testes, 0 falhas em 09/10) |

### H-02 — Erro genérico com HTTP 500 e `status: 400` no corpo

| Campo | Valor |
| --- | --- |
| ID | H-02 (sem ID no quadro) |
| Título | Erro genérico devolvia HTTP 500 com `status: 400` no corpo |
| Descrição | O handler de erro inesperado montava o corpo com status 400, contradizendo o status HTTP e o contrato de erros (`ApiErrorResponse`). |
| Passos para reprodução | 1. Provocar exceção não tratada em um controller. 2. Comparar o status HTTP com `body.status`. |
| Resultado esperado | HTTP 500 e `status: 500`, sem detalhes internos. |
| Resultado obtido | HTTP 500 e `status: 400`. |
| Severidade | Média |
| Prioridade | Média |
| Evidência | `docs/execucao-sessao.md`; `reports/execucoes/run-2026-10-04-sessao-nextjs.md`; teste `ErrorResponseTest.genericErrorBodyMatchesHttpStatusAndDoesNotLeakException` |
| Responsável | Correção: Matheus (04/10). Reteste independente: Allan. |
| Status | Corrigido e retestado; encerramento aguarda reteste independente |
| Correção | `GlobalExceptionHandler`: corpo do erro genérico com status 500 |
| Reteste | Teste de regressão na suíte desde 04/10 (CI de sessão de 04/10, run 37177644692, citado no card 16); aprovado na suíte de 09/10 (176 testes, 0 falhas) |

### D003 — Boleto vencido marca o pedido como PAID

| Campo | Valor |
| --- | --- |
| ID | D003 |
| Título | Boleto vencido marca o pedido como PAID |
| Descrição | O processamento aprovava o pedido mesmo com o boleto expirado. Viola RF-08 / CA-08-01. |
| Passos para reprodução | 1. Pedido PENDING_PAYMENT com boleto de vencimento ontem. 2. `POST /payment/{id}/process`. |
| Resultado esperado | Pagamento EXPIRED; pedido continua PENDING_PAYMENT. |
| Resultado obtido | Pedido PAID. |
| Severidade | Alta |
| Prioridade | Alta |
| Evidência | Execução local de 07/10 (111 testes, 20 falhas + 1 erro); `PaymentServiceTest.expiredBoletoDoesNotMarkOrderPaid`; `PaymentProcessingTest` (vencimento hoje ± 1 como valor-limite) |
| Responsável | Card FIX-01: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido, aguardando revisão e merge do PR #5 |
| Correção | `ded3e4d` (PR #5): só pagamento aprovado confirma o pedido; vencimento comparado com data injetável |
| Reteste | Branch do PR #5 em 09/10: `scripts/test.ps1` 104 testes, 0 falhas; CI do PR verde. Reteste formal pós-merge pendente. |

### D004 — Pagamento não PENDING é processado de novo

| Campo | Valor |
| --- | --- |
| ID | D004 |
| Título | Pagamento não PENDING é processado de novo |
| Descrição | `processAndSave` não conferia o status; pagamento já aprovado/recusado/expirado era reprocessado e podia lançar NullPointerException. Viola RF-08. |
| Passos para reprodução | 1. Processar um pagamento (fica APPROVED). 2. Chamar `POST /payment/{id}/process` de novo. |
| Resultado esperado | Erro de negócio (`PaymentException`, 400) sem alterar o pagamento. |
| Resultado obtido | Reprocessamento ou NullPointerException (500). |
| Severidade | Alta |
| Prioridade | Alta |
| Evidência | `PaymentServiceTest.onlyPendingPaymentsCanBeProcessed` (APPROVED, DECLINED, EXPIRED) |
| Responsável | Card FIX-01: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido, aguardando merge do PR #5 |
| Correção | `ded3e4d`: guarda de status PENDING em `processAndSave` |
| Reteste | Igual a D003 |

### D006 — Boleto sem vencimento aceito

| Campo | Valor |
| --- | --- |
| ID | D006 |
| Título | Boleto sem vencimento aceito; processamento lança NullPointerException |
| Descrição | `dueDate` não era obrigatório para BOLETO; o processamento comparava data nula. Viola RF-08. |
| Passos para reprodução | 1. `POST /payment` tipo BOLETO sem `dueDate`. 2. Processar. |
| Resultado esperado | 400 na criação, com mensagem `dueDate is required`. |
| Resultado obtido | Criação aceita; NullPointerException no processamento. |
| Severidade | Alta |
| Prioridade | Alta |
| Evidência | `PaymentServiceTest.boletoRequiresDueDate`; `PaymentProcessingTest` [D006] |
| Responsável | Card FIX-01: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido, aguardando merge do PR #5 |
| Correção | `ded3e4d`: `PaymentRules.requireDueDate` antes de salvar (pedido e pagamento) |
| Reteste | Igual a D003 |

### D008 — Cliente sem documento lança exceção de produto

| Campo | Valor |
| --- | --- |
| ID | D008 |
| Título | Cliente sem CPF/CNPJ lança `ProductException` |
| Descrição | `ClientService.buildClient` usava a exceção de produto para erro de cliente; a mensagem e a classificação ficavam erradas. CA-03-01/02. |
| Passos para reprodução | 1. `POST /clients` tipo INDIVIDUAL sem CPF. |
| Resultado esperado | 400 com erro de cliente mencionando `cpf`. |
| Resultado obtido | `ProductException`. |
| Severidade | Baixa |
| Prioridade | Baixa |
| Evidência | `CatalogServicesTest.missingDocumentIsAClientError` |
| Responsável | Card FIX-02: Francisco (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido, aguardando merge do PR #8 |
| Correção | `f346e53` (PR #8): `ClientException` |
| Reteste | Branch do PR #8 em 09/10: `scripts/test.ps1` 130 testes, 0 falhas |

### D010 — Status do pedido regride

| Campo | Valor |
| --- | --- |
| ID | D010 |
| Título | Status do pedido regride (DELIVERED → PENDING_PAYMENT, SHIPPED → PAID, PAID → PENDING_PAYMENT) |
| Descrição | `updateStatus` aceitava qualquer status. RF-08. |
| Passos para reprodução | 1. Pedido DELIVERED. 2. `PUT /orders/{id}/status` com `PENDING_PAYMENT`. |
| Resultado esperado | 400 (`OrderException`); status mantido. |
| Resultado obtido | Status alterado para PENDING_PAYMENT. |
| Severidade | Alta |
| Prioridade | Alta |
| Evidência | `OrderServiceRulesTest.statusRegressionIsRejected`; `OrderStatusTest.transitionTable` (25 combinações) |
| Responsável | Card FIX-02: Francisco (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido, aguardando merge do PR #8 |
| Correção | `f346e53` e `4e9ded0` (PR #8): tabela de transições explícita em `OrderStatus.allowedNext()` |
| Reteste | Branch do PR #8 em 09/10: 130 testes, 0 falhas |

### D011 — DELETE de recurso inexistente não retorna 404

| Campo | Valor |
| --- | --- |
| ID | D011 |
| Título | DELETE de produto, cliente, endereço, pedido ou pagamento inexistente não retorna 404 |
| Descrição | `deleteById` ignorava id inexistente e a API respondia 204, diferente do GET (404). Erro de contrato. |
| Passos para reprodução | 1. `DELETE /products/999999`. |
| Resultado esperado | 404 com `ApiErrorResponse`. |
| Resultado obtido | 204. |
| Severidade | Média |
| Prioridade | Média |
| Evidência | Testes `deletingMissing*Fails` e `deletingExisting*` nos 5 serviços |
| Responsável | Card FIX-02: Francisco (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido, aguardando merge do PR #8 |
| Correção | `f346e53`: `existsById` antes de `deleteById`; `4e9ded0`: testes do caminho positivo |
| Reteste | Branch do PR #8 em 09/10: 130 testes, 0 falhas |

### D012 — Valor do pagamento diferente do total aceito

| Campo | Valor |
| --- | --- |
| ID | D012 |
| Título | Pagamento com valor diferente do total do pedido é aceito |
| Descrição | `POST /orders` e `POST /payment` não comparavam o valor pago com o total. RF-07, RF-08. |
| Passos para reprodução | 1. Pedido de R$ 100,00. 2. Pagamento PIX de R$ 0,01. |
| Resultado esperado | 400; nada gravado. |
| Resultado obtido | Pagamento gravado e aprovável. |
| Severidade | Crítica |
| Prioridade | Alta |
| Evidência | `PaymentServiceTest.amountMustMatchOrderTotal`; `OrderServiceRulesTest.paymentAmountMustEqualOrderTotal`; fronteiras HALF_UP 99,995/100,004/100,005 (`amountIsComparedAfterHalfUpRounding`, `halfCentAboveTotalIsRejected`) |
| Responsável | Card FIX-01: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido, aguardando merge do PR #5 |
| Correção | `ded3e4d`: `PaymentRules.requireAmountMatchesTotal` (comparação em centavos; depois em `BigDecimal` no DOM-02) |
| Reteste | Igual a D003 |

### D016 — Tipo de pagamento depende do locale

| Campo | Valor |
| --- | --- |
| ID | D016 |
| Título | Tipo de pagamento depende do locale da JVM |
| Descrição | `toUpperCase()` sem `Locale`: em `tr-TR`, "pix" vira "PİX" e é rejeitado. Também apontado pelo SpotBugs (DM_CONVERT_CASE) no R0. |
| Passos para reprodução | 1. JVM com locale `tr-TR`. 2. `POST /payment` com `type: "pix"`. |
| Resultado esperado | Pagamento PIX criado. |
| Resultado obtido | `Invalid payment type: pix`. |
| Severidade | Média |
| Prioridade | Média |
| Evidência | `PaymentServiceTest.typeParsingIsLocaleIndependent` |
| Responsável | Card FIX-01: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido, aguardando merge do PR #5 |
| Correção | `ded3e4d`: `PaymentRules.normalizeType` com `Locale.ROOT` |
| Reteste | Igual a D003. Restam 3 `DM_CONVERT_CASE` em cliente/produto/status (triagem no card 09). |

### D017 — Valor abaixo de meio centavo gravado como 0,00

| Campo | Valor |
| --- | --- |
| ID | D017 |
| Título | Valor abaixo de meio centavo é aceito e gravado como 0,00 |
| Descrição | `@Positive` aceitava 0,001–0,004; `Money.of` (HALF_UP) gravava 0,00, permitindo produto de preço zero e pedido de valor zero. RNF-03. Achado na revisão do PR #9. |
| Passos para reprodução | 1. `POST /products` com `price: 0.004`. |
| Resultado esperado | 400 (o menor valor aceito é 0,005, que arredonda para 0,01). |
| Resultado obtido | Produto criado com preço 0,00. |
| Severidade | Alta |
| Prioridade | Alta |
| Evidência | `MoneyValidationTest.subCentValuesThatRoundToZeroAreRejected` (0,00 / 0,004 / 0,0049 / 0,005 / 0,01); falhou em `fc58a4b` |
| Responsável | Card FIX-03: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido em branch local, não publicado |
| Correção | `c007dff`: `@DecimalMin(Money.MIN = "0.005")` nos três DTOs de dinheiro |
| Reteste | `c178204`, 09/10: 176 testes, 0 falhas; OpenAPI ao vivo publica `minimum: 0.005` (`reports/execucoes/run-2026-10-09-fix-03.md`) |

### D018 — Pagamento tardio regride pedido para PAID

| Campo | Valor |
| --- | --- |
| ID | D018 |
| Título | Pagamento aprovado depois do envio volta o pedido para PAID |
| Descrição | `processAndSave` gravava PAID sem consultar a tabela de transições, reabrindo D010 por outro caminho. Achado na revisão do PR #9. |
| Passos para reprodução | 1. Pedido SHIPPED com pagamento PENDING. 2. `POST /payment/{id}/process`. |
| Resultado esperado | 400 (`PaymentException`); pedido continua SHIPPED; pagamento continua PENDING. |
| Resultado obtido | Pedido PAID. |
| Severidade | Alta |
| Prioridade | Alta |
| Evidência | `PaymentServiceTest.latePaymentDoesNotMoveOrderBackToPaid` (SHIPPED, DELIVERED); falhou em `fc58a4b` |
| Responsável | Card FIX-03: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido em branch local, não publicado |
| Correção | `c007dff`: `canMoveTo(PAID)` antes de processar |
| Reteste | `c178204`, 09/10: 176 testes, 0 falhas |

### D019 — Condição de corrida em pagamento e status

| Campo | Valor |
| --- | --- |
| ID | D019 |
| Título | Requisições simultâneas passam pela validação de status |
| Descrição | Dois `/payment/{id}/process` ou duas trocas de status ao mesmo tempo leem o estado antigo e ambos passam. Achado na revisão dos PRs #8 e #9. RNF-02. |
| Passos para reprodução | 1. Pedido PROCESSING. 2. Enviar `SHIPPED` e `PAID` ao mesmo tempo (duas threads, PostgreSQL real). |
| Resultado esperado | Estado final SHIPPED; no pagamento, uma requisição aprovada e a outra recusada. |
| Resultado obtido | Estado final PAID (`expected: SHIPPED but was: PAID`); pagamento aprovado duas vezes (`Expected size: 1 but was: 0`). |
| Severidade | Média |
| Prioridade | Alta (pagamento duplicado afeta o realizado) |
| Evidência | `ConcurrencyTest` (integração, 10 rodadas por teste); falhou em `fc58a4b` |
| Responsável | Card FIX-03: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido em branch local, não publicado |
| Correção | `c007dff`: `SELECT ... FOR UPDATE` (`findWithLockById`) no pedido e no pagamento, com ordem fixa de bloqueio |
| Reteste | `c178204`: suíte completa e `ConcurrencyTest` repetido 3 vezes, 0 falhas |

### D020 — Migração V3 arredonda antes de conferir o limite

| Campo | Valor |
| --- | --- |
| ID | D020 |
| Título | Migração V3 arredonda antes de conferir o limite de NUMERIC(12,2) |
| Descrição | Valor legado 9999999999,994 arredondava para o máximo e passava, contrariando a decisão de falhar sem truncar (`docs/decisoes/dinheiro.md`). Achado na revisão do PR #9. |
| Passos para reprodução | 1. Banco em V2 com `product.price = 9999999999.994`. 2. Migrar para V3. |
| Resultado esperado | Migração falha com mensagem clara; nada é convertido. |
| Resultado obtido | Migração concluída com 9999999999,99. |
| Severidade | Baixa (valor improvável na loja) |
| Prioridade | Média (precisa estar certo antes de migrar um banco real) |
| Evidência | `FlywayMigrationTest.v3RejectsLegacyValueJustAboveLimitBeforeRounding`; falhou em `fc58a4b` |
| Responsável | Card FIX-03: Gabriel (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido em branch local, não publicado |
| Correção | `c007dff`: bloco `DO $$` na V3 confere `abs(valor) > 9999999999.99` antes do `ALTER` |
| Reteste | `c178204`: 176 testes, 0 falhas |

### D021 — Senha do banco ausente gera erro confuso

| Campo | Valor |
| --- | --- |
| ID | D021 |
| Título | Sem `SPRING_DATASOURCE_PASSWORD`/`TEST_DB_PASSWORD`, a API falha com erro de autenticação confuso |
| Descrição | O Spring Boot mantém o placeholder não resolvido como texto literal; a falha aparecia só como autenticação recusada pelo PostgreSQL. Além disso, `test-contract.ps1` passava a senha na linha de comando (quebrava com espaço e ficava visível). Achado na revisão do PR #6. |
| Passos para reprodução | 1. Remover `SPRING_DATASOURCE_PASSWORD` do ambiente. 2. `java -jar target/order-management-spring-0.0.1-SNAPSHOT.jar`. |
| Resultado esperado | Inicialização interrompida com mensagem que cita as variáveis. |
| Resultado obtido | Erro de autenticação do PostgreSQL. |
| Severidade | Baixa |
| Prioridade | Média (bloqueia quem configura o ambiente pela primeira vez) |
| Evidência | `DatasourcePasswordGuardTest` (6 casos); execução manual do jar em 09/10 com a mensagem `Senha do banco ausente: defina SPRING_DATASOURCE_PASSWORD ...` |
| Responsável | Card 19: Francisco (implementação: Matheus). Reteste: Allan. |
| Status | Corrigido em commit novo na branch do PR #6, não publicado |
| Correção | `21f06a3`: `DatasourcePasswordGuard`; senha passada ao processo filho por variável de ambiente |
| Reteste | 09/10: unitário 96 testes e integração (contexto, Flyway, persistência) sem falhas |

### D022 — Número completo do cartão gravado pelo pagamento legado

| Campo | Valor |
| --- | --- |
| ID | D022 (origem: conflito N-05 de [decisoes/erp-pdv.md](decisoes/erp-pdv.md)) |
| Título | `POST /payment` e `POST /orders` com cartão gravam o número completo em `payment.card_number` |
| Descrição | A decisão DEC-ERP-04 proíbe guardar o número completo do cartão; só os 4 últimos dígitos e o código de autorização opcional. A resposta já mostra o número mascarado (`PaymentResponseDTO.cardNumberMasked`), mas `CardPayment.cardNumber` persiste o valor recebido sem corte. Achado na modelagem ERP/PDV (ETAPA 1, 14/10/2026). |
| Passos para reprodução | 1. Como GERENTE, `POST /payment` com `type: CARD` e `cardNumber: 4111111111111111` (cartão de teste público). 2. `SELECT card_number FROM payment ORDER BY id DESC LIMIT 1`. |
| Resultado esperado | Banco sem o número completo; no máximo os 4 últimos dígitos. |
| Resultado obtido | `card_number = 4111111111111111`, conforme o código atual de `CardPayment` e `PaymentService` (não executado como teste). |
| Severidade | Alta (dado sensível de pagamento armazenado sem necessidade) |
| Prioridade | Média (rota legada só do GERENTE, base só com dados sintéticos) |
| Evidência | Leitura de `entities/payment/CardPayment.java` e `dto/PaymentRequestDTO.java`; teste de reprodução a escrever na ETAPA 2C. |
| Responsável | Correção: ETAPA 2C (`feat/business-endpoints`). Reteste: Allan. |
| Status | Aberto |
| Correção | Planejada: o serviço grava só os 4 últimos dígitos; migração mascara os dados existentes; teste de regressão. Sem CHECK em `card_number` na ETAPA 2A. |
| Reteste | Pendente. |
