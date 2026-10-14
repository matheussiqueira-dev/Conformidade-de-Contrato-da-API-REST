# Catálogo de casos de teste

Versão 0.1 (rascunho), 09/10/2026. Preparado por Matheus Siqueira para o card 10. **Responsável: Allan. Revisor: Gabriel.** Formato: [templates/caso-de-teste.md](templates/caso-de-teste.md), com os 10 campos da seção 11 do enunciado e mais técnica, nível e risco.

**43 casos** (o mínimo exigido é 30). Os casos reaproveitam os IDs que os testes automatizados já usam nos nomes ([CT-...], [Dxxx]) e os IDs planejados em [plano-testes.md](plano-testes.md). Casos de regressão de defeito usam CT-REG-Dxxx. Cada caso aponta o teste automatizado que o executa; nenhum depende de execução manual.

## Execuções de referência

| Código | Execução | Resultado |
| --- | --- | --- |
| E1 | 09/10/2026, `scripts/test.ps1 -Mode Integration`, branch `fix/FIX-03-pagamento-dinheiro` @ `c178204` (contém os PRs #5, #8 e #9), Windows 11, JDK 25, PostgreSQL 18 | 176 testes, 0 falhas, 0 erros |
| E2 | 09/10/2026, `scripts/test-contract.ps1 -HttpOnly` no mesmo código | 6 casos de contrato e 6 de sessão aprovados |
| E3 | CI GitHub Actions, run [37960769072](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37960769072), commit `ce3f222` (PR #9), 09/10/2026 | success (inclui E2E Playwright) |

Enquanto os PRs não forem mesclados, o status "Passou" vale para a branch indicada. Depois do merge, refazer E1 na `main` (medição R1, QA-10) e atualizar a coluna. Detalhes do antes × depois do FIX-03: `reports/execucoes/run-2026-10-09-fix-03.md` (na branch do FIX-03).

## Resumo

| ID | Título | Requisito | Técnica | Nível | Risco | Status |
| --- | --- | --- | --- | --- | --- | --- |
| [CT-MONEY-001](#ct-money-001) | Total da venda demonstrativa | RF-07 / CA-07-01 | Baseado em requisitos | Unitário, Integração | R02 | Passou |
| [CT-MONEY-002](#ct-money-002) | Quantidade multiplicada uma única vez | RF-07 / CA-07-02 | Particionamento de equivalência | Unitário, Regressão | R02 | Passou |
| [CT-MONEY-003](#ct-money-003) | Preço histórico preservado após mudança do catálogo | RF-07 / CA-07-03 | Transição de estados | Integração | R02 | Passou |
| [CT-MONEY-004](#ct-money-004) | Pedido sem itens | RF-07 | Análise de valor-limite | Unitário | R02 | Passou |
| [CT-MONEY-005](#ct-money-005) | Normalização para 2 casas com HALF_UP | RNF-03 | Análise de valor-limite | Unitário | R06 | Passou |
| [CT-MONEY-006](#ct-money-006) | Soma de centavos sem erro de ponto flutuante | RNF-03 | Particionamento de equivalência | Unitário | R06 | Passou |
| [CT-MONEY-007](#ct-money-007) | Limites de preço e pagamento na entrada | RNF-03 | Análise de valor-limite | Unitário | R06 | Passou |
| [CT-REG-D017](#ct-reg-d017) | Menor valor monetário aceito | RNF-03 / D017 | Análise de valor-limite | Unitário, Regressão | R06 | Passou |
| [CT-ORDER-001](#ct-order-001) | Pedido válido é criado | RF-05 / CA-05-01 | Baseado em requisitos | Integração | R02 | Passou (parcial) |
| [CT-ORDER-002](#ct-order-002) | Cliente novo persistido com o pedido | RF-05 / CA-05-02 | Particionamento de equivalência | Integração | R02 | Passou |
| [CT-ORDER-003](#ct-order-003) | Pedido sem cliente é rejeitado | RF-05 / CA-05-03 | Testes de casos inválidos | Unitário | R02 | Passou |
| [CT-ORDER-004](#ct-order-004) | Vendedor do pedido vem da sessão | RF-06 / CA-06-01 | Tabela de decisão | Unitário, Integração | R05 | Passou |
| [CT-ORDER-005](#ct-order-005) | clientId inexistente | RF-05 | Testes de casos inválidos | Unitário | R02 | Passou |
| [CT-ORDER-006](#ct-order-006) | Cliente novo sem documento ou de tipo inválido | RF-03, RF-05 | Particionamento de equivalência | Unitário | R09 | Passou |
| [CT-ORDER-007](#ct-order-007) | Produto inexistente no pedido | RF-05 | Testes de casos inválidos | Unitário | R02 | Passou |
| [CT-ORDER-009](#ct-order-009) | Pagamento do pedido sem dado obrigatório | RF-05, RF-08 | Tabela de decisão | Unitário | R01 | Passou |
| [CT-PAY-001](#ct-pay-001) | Pagamento pendente aprovado confirma o pedido | RF-08 / CA-08-01 | Transição de estados | Unitário | R01 | Passou |
| [CT-REG-D012](#ct-reg-d012) | Valor pago diferente do total | RF-08 / D012 | Análise de valor-limite | Unitário, Regressão | R01 | Passou |
| [CT-PAY-011](#ct-pay-011) | Comparação com o total depois do HALF_UP | RF-08, RNF-03 | Análise de valor-limite | Unitário, Regressão | R01 | Passou |
| [CT-BB-BOL-02](#ct-bb-bol-02) | Vencimento do boleto | RF-08 / D003 | Análise de valor-limite | Unitário, Regressão | R01 | Passou |
| [CT-REG-D004](#ct-reg-d004) | Pagamento já processado não é reprocessado | RF-08 / D004 | Transição de estados | Unitário, Regressão | R01 | Passou |
| [CT-REG-D006](#ct-reg-d006) | Boleto sem vencimento | RF-08 / D006 | Testes de casos inválidos | Unitário, Regressão | R01 | Passou |
| [CT-REG-D016](#ct-reg-d016) | Tipo de pagamento independente do locale | RF-08 / D016 | Particionamento de equivalência | Unitário, Regressão | R01 | Passou |
| [CT-REG-D018](#ct-reg-d018) | Pagamento tardio não regride o pedido | RF-08 / D018 | Transição de estados | Unitário, Regressão | R01, R04 | Passou |
| [CT-REG-D019-PAG](#ct-reg-d019-pag) | Processamento simultâneo do mesmo pagamento | RNF-02 / D019 | Baseado em risco | Integração, Regressão | R01 | Passou |
| [CT-PAY-005](#ct-pay-005) | Cada tipo cria a subclasse correta | RF-08 | Particionamento de equivalência | Unitário | R01 | Passou |
| [CT-PAY-009](#ct-pay-009) | Troca de tipo substitui o pagamento | RF-08 | Transição de estados | Unitário | R01 | Passou |
| [CT-WB-PIX-02](#ct-wb-pix-02) | Classificação da chave PIX (caminhos) | RF-08 | Caixa-branca (caminhos independentes) | Unitário | R01 | Passou |
| [CT-BB-FRETE-01](#ct-bb-frete-01) | Frete na fronteira de 2 kg | RF-04, RF-07 | Análise de valor-limite | Unitário | R11 | Passou |
| [CT-ST-DT](#ct-st-dt) | Tabela completa de transições de status | RF-08 / D010 | Tabela de decisão / transição de estados | Unitário, Regressão | R04 | Passou |
| [CT-REG-D019-ST](#ct-reg-d019-st) | Trocas de status simultâneas | RNF-02 / D019 | Baseado em risco | Integração, Regressão | R04 | Passou |
| [CT-ADDR-DT](#ct-addr-dt) | Alteração de endereço por status | RF-05 | Tabela de decisão | Unitário | R12 | Passou |
| [CT-PROD-DT](#ct-prod-dt) | Cadastro de produto por tipo | RF-04 / CA-04-01, CA-04-02 | Tabela de decisão | Unitário | R11 | Passou |
| [CT-CLIENT-001](#ct-client-001) | Cliente sem documento obrigatório | RF-03 / CA-03-01, CA-03-02 / D008 | Particionamento de equivalência | Unitário, Regressão | R09 | Passou |
| [CT-REG-D011](#ct-reg-d011) | Excluir recurso inexistente e existente | D011 | Particionamento de equivalência | Unitário, Regressão | R09, R11, R12 | Passou |
| [CT-BD-001](#ct-bd-001) | Migração V3 de dinheiro legado | RNF-02, RNF-03 | Análise de valor-limite | Integração | R07 | Passou |
| [CT-REG-D020](#ct-reg-d020) | Valor legado logo acima do limite | RNF-02 / D020 | Análise de valor-limite | Integração, Regressão | R07 | Passou |
| [CT-CONTRACT-001](#ct-contract-001) | API ao vivo conforme o contrato OpenAPI | RF-04, RNF-07 | Baseado em requisitos (contrato) | Sistema | R11 | Passou |
| [CT-AUTH-001](#ct-auth-001) | Acesso sem sessão e login inválido | RF-01 / CA-01-01 | Tabela de decisão | Sistema | R05 | Passou |
| [CT-AUTH-002](#ct-auth-002) | Vendedor não lê dados restritos | RF-02 / CA-01-02 | Tabela de decisão | Sistema | R05 | Passou |
| [CT-UI-LOGIN-001](#ct-ui-login-001) | Login e logout pela interface | RF-01, RF-16 | Baseado em requisitos | Aceitação (E2E) | R13 | Passou |
| [CT-UI-LOGIN-002](#ct-ui-login-002) | Erro de senha acessível e tela móvel | RNF-05 | Testes de casos inválidos | Aceitação (E2E) | R13 | Passou |
| [CT-REG-D021](#ct-reg-d021) | Senha do banco ausente | RNF-04 / D021 | Testes de casos inválidos | Unitário, Regressão | R10 | Passou |

### Distribuição

| Técnica | Casos |
| --- | ---: |
| Análise de valor-limite | 10 |
| Particionamento de equivalência | 8 |
| Tabela de decisão | 7 |
| Testes de casos inválidos | 6 |
| Transição de estados | 6 |
| Baseado em requisitos | 3 |
| Baseado em risco | 2 |
| Baseado em requisitos (contrato) | 1 |
| Caixa-branca (caminhos independentes) | 1 |

| Nível | Casos |
| --- | ---: |
| Unitário | 31 |
| Regressão | 16 |
| Integração | 9 |
| Sistema | 3 |
| Aceitação (E2E) | 2 |

Um caso pode ter mais de uma técnica ou nível. As técnicas caixa-preta (particionamento, valor-limite, tabela de decisão, transição de estados, casos inválidos, baseado em requisitos e em risco) passam do mínimo de 3 do enunciado. Lacunas conhecidas: aceitação Dado/Quando/Então das telas de negócio (depende de UI-04 e QA-07), CA-08-02/CA-08-03 (idempotência e estorno, API-06), metas (RF-09/RF-10) e estoque (RF-13), que ainda não existem.

## Casos

### CT-MONEY-001

**Total da venda demonstrativa**

| Campo | Valor |
| --- | --- |
| ID | CT-MONEY-001 |
| Requisito relacionado | RF-07 / CA-07-01 |
| Objetivo | Provar que itens distintos somam o total sem desconto. |
| Pré-condições | Produtos iPhone R$ 4.500,00 e AirPods R$ 1.200,00. |
| Dados de entrada | 1 unidade de cada; pagamento PIX R$ 5.700,00. |
| Passos | 1. Criar pedido com os dois itens. 2. Ler o total. |
| Resultado esperado | Total R$ 5.700,00. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `ProductAndOrderCalculationTest.totalSumsAllItems`, `OrderServiceTest.insertCalculatesDemonstrationSaleTotal`, `OrderPersistenceTest.demonstrationSalePersistsClientAddressPaymentAndTotal` |
| Técnica · Nível · Risco | Baseado em requisitos · Unitário, Integração · R02 |

### CT-MONEY-002

**Quantidade multiplicada uma única vez**

| Campo | Valor |
| --- | --- |
| ID | CT-MONEY-002 |
| Requisito relacionado | RF-07 / CA-07-02 |
| Objetivo | Garantir que o subtotal é preço × quantidade, sem multiplicar de novo (H-01). |
| Pré-condições | Produto R$ 100,00. |
| Dados de entrada | Quantidades 1, 2 e 10. |
| Passos | 1. Criar item com cada quantidade. 2. Ler o subtotal. |
| Resultado esperado | R$ 100,00; R$ 200,00; R$ 1.000,00. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `ProductAndOrderCalculationTest.itemSubtotalMultipliesQuantityOnce`; reteste de H-01 em `reports/execucoes/run-2026-10-03-ct-money-002.md` |
| Técnica · Nível · Risco | Particionamento de equivalência · Unitário, Regressão · R02 |

### CT-MONEY-003

**Preço histórico preservado após mudança do catálogo**

| Campo | Valor |
| --- | --- |
| ID | CT-MONEY-003 |
| Requisito relacionado | RF-07 / CA-07-03 |
| Objetivo | Mostrar que a venda passada guarda o preço unitário da data da venda. |
| Pré-condições | Produto R$ 100,00 gravado no PostgreSQL de teste. |
| Dados de entrada | Pedido com quantidade 2; depois o catálogo muda para R$ 150,00. |
| Passos | 1. Criar pedido. 2. Alterar o preço do produto. 3. Limpar o contexto JPA e recarregar o pedido do banco. |
| Resultado esperado | Item R$ 100,00; total R$ 200,00; produto R$ 150,00. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderPersistenceTest.historicalUnitPriceSurvivesCatalogChangeAndDatabaseReload`, `ProductAndOrderCalculationTest.subtotalKeepsHistoricalUnitPrice` |
| Técnica · Nível · Risco | Transição de estados · Integração · R02 |

### CT-MONEY-004

**Pedido sem itens**

| Campo | Valor |
| --- | --- |
| ID | CT-MONEY-004 |
| Requisito relacionado | RF-07 |
| Objetivo | Fronteira inferior da quantidade de itens. |
| Pré-condições | Pedido sem itens. |
| Dados de entrada | Lista de itens vazia. |
| Passos | 1. Calcular o total. |
| Resultado esperado | Total R$ 0,00, sem exceção. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `ProductAndOrderCalculationTest.emptyOrderTotalsZero` |
| Técnica · Nível · Risco | Análise de valor-limite · Unitário · R02 |

### CT-MONEY-005

**Normalização para 2 casas com HALF_UP**

| Campo | Valor |
| --- | --- |
| ID | CT-MONEY-005 |
| Requisito relacionado | RNF-03 |
| Objetivo | Confirmar a regra de dinheiro da decisão DOM-01. |
| Pré-condições | Nenhuma. |
| Dados de entrada | 1,005; 1,004; 100; 0,015. |
| Passos | 1. Aplicar `Money.of` a cada valor. |
| Resultado esperado | 1,01; 1,00; 100,00; 0,02. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `ProductAndOrderCalculationTest.moneyUsesTwoDecimalsHalfUp` |
| Técnica · Nível · Risco | Análise de valor-limite · Unitário · R06 |

### CT-MONEY-006

**Soma de centavos sem erro de ponto flutuante**

| Campo | Valor |
| --- | --- |
| ID | CT-MONEY-006 |
| Requisito relacionado | RNF-03 |
| Objetivo | Provar que a migração para `BigDecimal` eliminou o erro de `double`. |
| Pré-condições | Itens de R$ 0,10 e R$ 0,20. |
| Dados de entrada | 0,10 + 0,20. |
| Passos | 1. Somar os subtotais. |
| Resultado esperado | Exatamente 0,30. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `ProductAndOrderCalculationTest.itemsWithCentsAddExactly` |
| Técnica · Nível · Risco | Particionamento de equivalência · Unitário · R06 |

### CT-MONEY-007

**Limites de preço e pagamento na entrada**

| Campo | Valor |
| --- | --- |
| ID | CT-MONEY-007 |
| Requisito relacionado | RNF-03 |
| Objetivo | Rejeitar zero, negativo e valor acima de NUMERIC(12,2) antes do banco. |
| Pré-condições | Bean Validation ativa. |
| Dados de entrada | Preço: 0,01; 9.999.999.999,99; 10.000.000.000,00; 0; -1,00. Pagamento: 100,00; 10.000.000.000,00; -0,01. |
| Passos | 1. Validar cada DTO. |
| Resultado esperado | Aceitos: 0,01; 9.999.999.999,99; 100,00. Rejeitados: os demais. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `MoneyValidationTest.productPriceLimits`, `MoneyValidationTest.paymentAmountLimits` |
| Técnica · Nível · Risco | Análise de valor-limite · Unitário · R06 |

### CT-REG-D017

**Menor valor monetário aceito**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D017 |
| Requisito relacionado | RNF-03 / D017 |
| Objetivo | Fronteira de meio centavo nos três DTOs de dinheiro. |
| Pré-condições | Bean Validation ativa. |
| Dados de entrada | 0,00; 0,004; 0,0049; 0,005; 0,01. |
| Passos | 1. Validar produto, pagamento e pagamento do pedido com cada valor. |
| Resultado esperado | Rejeitados: 0,00; 0,004; 0,0049. Aceitos: 0,005 (vira 0,01) e 0,01. |
| Resultado obtido | Antes da correção (`fc58a4b`): 0,004 e 0,0049 aceitos, falha → D017. Depois: conforme o esperado (E1). |
| Status | Passou |
| Evidência | `MoneyValidationTest.subCentValuesThatRoundToZeroAreRejected`; `reports/execucoes/run-2026-10-09-fix-03.md` |
| Técnica · Nível · Risco | Análise de valor-limite · Unitário, Regressão · R06 |

### CT-ORDER-001

**Pedido válido é criado**

| Campo | Valor |
| --- | --- |
| ID | CT-ORDER-001 |
| Requisito relacionado | RF-05 / CA-05-01 |
| Objetivo | Fluxo mínimo de criação com cliente, endereço e pagamento. |
| Pré-condições | Produtos e vendedor sintéticos no banco de teste. |
| Dados de entrada | Venda demonstrativa (CT-MONEY-001). |
| Passos | 1. Criar pedido. 2. Recarregar do banco. |
| Resultado esperado | Pedido com cliente, endereço e pagamento persistidos. |
| Resultado obtido | Conforme o esperado (E1). Lacuna: o teste não confere o status `PENDING_PAYMENT` nem o status do pagamento. |
| Status | Passou (parcial) |
| Evidência | `OrderPersistenceTest.demonstrationSalePersistsClientAddressPaymentAndTotal` |
| Técnica · Nível · Risco | Baseado em requisitos · Integração · R02 |

### CT-ORDER-002

**Cliente novo persistido com o pedido**

| Campo | Valor |
| --- | --- |
| ID | CT-ORDER-002 |
| Requisito relacionado | RF-05 / CA-05-02 |
| Objetivo | Cliente informado no pedido é gravado junto. |
| Pré-condições | Nenhum cliente com o CPF da massa. |
| Dados de entrada | Cliente INDIVIDUAL Marina Costa (dados sintéticos). |
| Passos | 1. Criar pedido com `client` em vez de `clientId`. 2. Recarregar. |
| Resultado esperado | Cliente com ID gerado e vinculado ao pedido. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderPersistenceTest.demonstrationSalePersistsClientAddressPaymentAndTotal` |
| Técnica · Nível · Risco | Particionamento de equivalência · Integração · R02 |

### CT-ORDER-003

**Pedido sem cliente é rejeitado**

| Campo | Valor |
| --- | --- |
| ID | CT-ORDER-003 |
| Requisito relacionado | RF-05 / CA-05-03 |
| Objetivo | Classe inválida: nem `clientId` nem dados de cliente. |
| Pré-condições | Nenhuma. |
| Dados de entrada | Pedido sem `clientId` e sem `client`. |
| Passos | 1. Criar pedido. |
| Resultado esperado | `OrderException` (400). |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderServiceRulesTest.clientIsRequired` |
| Técnica · Nível · Risco | Testes de casos inválidos · Unitário · R02 |

### CT-ORDER-004

**Vendedor do pedido vem da sessão**

| Campo | Valor |
| --- | --- |
| ID | CT-ORDER-004 |
| Requisito relacionado | RF-06 / CA-06-01 |
| Objetivo | O vendedor não é escolhido pelo payload. |
| Pré-condições | Usuário VENDEDOR autenticado. |
| Dados de entrada | Sessão sem usuário; sessão de conta removida; sessão válida. |
| Passos | 1. Criar pedido em cada situação. |
| Resultado esperado | Sem sessão: acesso negado. Conta removida: acesso negado. Sessão válida: `seller` = usuário da sessão. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderServiceTest.unauthenticatedCallerCannotCreateOrder`, `OrderServiceTest.removedUserCannotCreateOrderFromAnOldSession`, `ProductAndOrderCalculationTest.newOrderRequiresSeller`, `OrderPersistenceTest` (seller = sessão) |
| Técnica · Nível · Risco | Tabela de decisão · Unitário, Integração · R05 |

### CT-ORDER-005

**clientId inexistente**

| Campo | Valor |
| --- | --- |
| ID | CT-ORDER-005 |
| Requisito relacionado | RF-05 |
| Objetivo | Referência inválida retorna não encontrado. |
| Pré-condições | Cliente 404 não existe. |
| Dados de entrada | `clientId: 404`. |
| Passos | 1. Criar pedido. |
| Resultado esperado | `ResourceNotFoundException` (404). |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderServiceRulesTest.missingClientId` |
| Técnica · Nível · Risco | Testes de casos inválidos · Unitário · R02 |

### CT-ORDER-006

**Cliente novo sem documento ou de tipo inválido**

| Campo | Valor |
| --- | --- |
| ID | CT-ORDER-006 |
| Requisito relacionado | RF-03, RF-05 |
| Objetivo | Regras do cliente também valem dentro do pedido. |
| Pré-condições | Nenhuma. |
| Dados de entrada | Tipos INDIVIDUAL sem CPF, CORPORATE sem CNPJ, GOVERNO. |
| Passos | 1. Criar pedido com cada cliente. |
| Resultado esperado | Rejeitado, nada salvo. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderServiceRulesTest.newClientRules` |
| Técnica · Nível · Risco | Particionamento de equivalência · Unitário · R09 |

### CT-ORDER-007

**Produto inexistente no pedido**

| Campo | Valor |
| --- | --- |
| ID | CT-ORDER-007 |
| Requisito relacionado | RF-05 |
| Objetivo | Item com produto inválido. |
| Pré-condições | Produto 99 não existe. |
| Dados de entrada | `productId: 99`. |
| Passos | 1. Criar pedido. |
| Resultado esperado | `ResourceNotFoundException` (404). |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderServiceRulesTest.missingProduct` |
| Técnica · Nível · Risco | Testes de casos inválidos · Unitário · R02 |

### CT-ORDER-009

**Pagamento do pedido sem dado obrigatório**

| Campo | Valor |
| --- | --- |
| ID | CT-ORDER-009 |
| Requisito relacionado | RF-05, RF-08 |
| Objetivo | Cada tipo exige seu campo (cartão, chave PIX, código de barras). |
| Pré-condições | Catálogo com produto de R$ 100,00. |
| Dados de entrada | CARD sem número, PIX sem chave, BOLETO sem código, tipo CHEQUE. |
| Passos | 1. Criar pedido com cada pagamento. |
| Resultado esperado | `PaymentException` (400); nada salvo. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderServiceRulesTest.paymentRequiredFields` |
| Técnica · Nível · Risco | Tabela de decisão · Unitário · R01 |

### CT-PAY-001

**Pagamento pendente aprovado confirma o pedido**

| Campo | Valor |
| --- | --- |
| ID | CT-PAY-001 |
| Requisito relacionado | RF-08 / CA-08-01 |
| Objetivo | Caminho feliz do processamento. |
| Pré-condições | Pedido PENDING_PAYMENT de R$ 100,00 com PIX pendente. |
| Dados de entrada | Chave PIX `marina@example.test`. |
| Passos | 1. Processar o pagamento. |
| Resultado esperado | Pagamento APPROVED; pedido PAID. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentServiceTest.approvedPaymentMarksOrderPaid` |
| Técnica · Nível · Risco | Transição de estados · Unitário · R01 |

### CT-REG-D012

**Valor pago diferente do total**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D012 |
| Requisito relacionado | RF-08 / D012 |
| Objetivo | Rejeitar pagamento com valor diferente do total. |
| Pré-condições | Pedido de R$ 100,00. |
| Dados de entrada | 99,99; 100,01; 0,01. |
| Passos | 1. Criar pedido/pagamento com cada valor. |
| Resultado esperado | Rejeitado (400); nada salvo. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderServiceRulesTest.paymentAmountMustEqualOrderTotal`, `PaymentServiceTest.amountMustMatchOrderTotal` |
| Técnica · Nível · Risco | Análise de valor-limite · Unitário, Regressão · R01 |

### CT-PAY-011

**Comparação com o total depois do HALF_UP**

| Campo | Valor |
| --- | --- |
| ID | CT-PAY-011 |
| Requisito relacionado | RF-08, RNF-03 |
| Objetivo | Fronteira de arredondamento na comparação com o total. |
| Pré-condições | Pedido de R$ 100,00. |
| Dados de entrada | 99,995; 100,00; 100,004; 100,005. |
| Passos | 1. Criar pagamento com cada valor. |
| Resultado esperado | 99,995, 100,00 e 100,004 aceitos e gravados como 100,00; 100,005 (vira 100,01) rejeitado. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentServiceTest.amountIsComparedAfterHalfUpRounding`, `PaymentServiceTest.halfCentAboveTotalIsRejected` |
| Técnica · Nível · Risco | Análise de valor-limite · Unitário, Regressão · R01 |

### CT-BB-BOL-02

**Vencimento do boleto**

| Campo | Valor |
| --- | --- |
| ID | CT-BB-BOL-02 |
| Requisito relacionado | RF-08 / D003 |
| Objetivo | Fronteira do vencimento: ontem, hoje e amanhã. |
| Pré-condições | Pedido PENDING_PAYMENT com boleto válido. |
| Dados de entrada | Vencimento hoje − 1, hoje, hoje + 1. |
| Passos | 1. Processar o boleto com cada vencimento. |
| Resultado esperado | Ontem: EXPIRED e pedido continua PENDING_PAYMENT. Hoje e amanhã: APPROVED. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentProcessingTest.dueDateBoundary`, `PaymentServiceTest.expiredBoletoDoesNotMarkOrderPaid` |
| Técnica · Nível · Risco | Análise de valor-limite · Unitário, Regressão · R01 |

### CT-REG-D004

**Pagamento já processado não é reprocessado**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D004 |
| Requisito relacionado | RF-08 / D004 |
| Objetivo | Só PENDING pode ser processado. |
| Pré-condições | Pagamento em APPROVED, DECLINED ou EXPIRED. |
| Dados de entrada | Os três status. |
| Passos | 1. Processar de novo. |
| Resultado esperado | `PaymentException`; nada salvo; pedido inalterado. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentServiceTest.onlyPendingPaymentsCanBeProcessed` |
| Técnica · Nível · Risco | Transição de estados · Unitário, Regressão · R01 |

### CT-REG-D006

**Boleto sem vencimento**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D006 |
| Requisito relacionado | RF-08 / D006 |
| Objetivo | Erro de negócio em vez de NullPointerException. |
| Pré-condições | Pedido existente. |
| Dados de entrada | BOLETO com `dueDate` nulo. |
| Passos | 1. Criar o pagamento. 2. Processar boleto sem vencimento. |
| Resultado esperado | `PaymentException` nas duas etapas. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentServiceTest.boletoRequiresDueDate`, `PaymentProcessingTest.missingDueDateIsABusinessError` |
| Técnica · Nível · Risco | Testes de casos inválidos · Unitário, Regressão · R01 |

### CT-REG-D016

**Tipo de pagamento independente do locale**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D016 |
| Requisito relacionado | RF-08 / D016 |
| Objetivo | Converter o tipo com `Locale.ROOT`. |
| Pré-condições | JVM com locale padrão `tr-TR`. |
| Dados de entrada | `type: "pix"`. |
| Passos | 1. Trocar o locale. 2. Criar pagamento. 3. Restaurar o locale. |
| Resultado esperado | Pagamento PIX criado. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentServiceTest.typeParsingIsLocaleIndependent` |
| Técnica · Nível · Risco | Particionamento de equivalência · Unitário, Regressão · R01 |

### CT-REG-D018

**Pagamento tardio não regride o pedido**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D018 |
| Requisito relacionado | RF-08 / D018 |
| Objetivo | O processamento respeita a tabela de transições (D010). |
| Pré-condições | Pedido SHIPPED ou DELIVERED com PIX pendente. |
| Dados de entrada | Os dois status. |
| Passos | 1. Processar o pagamento. |
| Resultado esperado | `PaymentException`; pedido mantém o status; pagamento continua PENDING; nada salvo. |
| Resultado obtido | Antes da correção (`fc58a4b`): pedido virou PAID → D018. Depois: conforme o esperado (E1). |
| Status | Passou |
| Evidência | `PaymentServiceTest.latePaymentDoesNotMoveOrderBackToPaid` |
| Técnica · Nível · Risco | Transição de estados · Unitário, Regressão · R01, R04 |

### CT-REG-D019-PAG

**Processamento simultâneo do mesmo pagamento**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D019-PAG |
| Requisito relacionado | RNF-02 / D019 |
| Objetivo | Duas requisições simultâneas aprovam o pagamento uma vez só. |
| Pré-condições | PostgreSQL de teste; pedido real confirmado no banco com PIX pendente. |
| Dados de entrada | 2 threads liberadas juntas por `CyclicBarrier`; 10 rodadas. |
| Passos | 1. Chamar `PaymentService.process(id)` nas duas threads. 2. Contar exceções. 3. Ler o pagamento. |
| Resultado esperado | Exatamente 1 `PaymentException`; pagamento APPROVED. |
| Resultado obtido | Antes (`fc58a4b`): 0 exceções, as duas aprovaram → D019. Depois: conforme o esperado (E1 e mais 3 repetições). |
| Status | Passou |
| Evidência | `ConcurrencyTest.concurrentProcessingApprovesOnce` |
| Técnica · Nível · Risco | Baseado em risco · Integração, Regressão · R01 |

### CT-PAY-005

**Cada tipo cria a subclasse correta**

| Campo | Valor |
| --- | --- |
| ID | CT-PAY-005 |
| Requisito relacionado | RF-08 |
| Objetivo | Partição por tipo de pagamento. |
| Pré-condições | Pedido de R$ 100,00. |
| Dados de entrada | CARD, PIX, BOLETO, pix. |
| Passos | 1. Criar pagamento de cada tipo. |
| Resultado esperado | Subclasse correspondente, status PENDING. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentServiceTest.insertCreatesEachPaymentType` |
| Técnica · Nível · Risco | Particionamento de equivalência · Unitário · R01 |

### CT-PAY-009

**Troca de tipo substitui o pagamento**

| Campo | Valor |
| --- | --- |
| ID | CT-PAY-009 |
| Requisito relacionado | RF-08 |
| Objetivo | Editar PIX para BOLETO mantém o vínculo com o pedido. |
| Pré-condições | PIX pendente vinculado ao pedido. |
| Dados de entrada | Novo tipo BOLETO. |
| Passos | 1. Atualizar o pagamento. |
| Resultado esperado | PIX removido; BOLETO criado; pedido aponta para o novo pagamento e vice-versa. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentServiceTest.typeChangeReplacesPayment` |
| Técnica · Nível · Risco | Transição de estados · Unitário · R01 |

### CT-WB-PIX-02

**Classificação da chave PIX (caminhos)**

| Campo | Valor |
| --- | --- |
| ID | CT-WB-PIX-02 |
| Requisito relacionado | RF-08 |
| Objetivo | Exercitar os caminhos independentes de `PixPayment.processPayment`. |
| Pré-condições | Pagamento PIX pendente. |
| Dados de entrada | E-mail, CPF (11 dígitos), telefone (10 dígitos), UUID, `+5581999998888`; chaves inválidas (CT-WB-PIX-01). |
| Passos | 1. Processar cada chave. |
| Resultado esperado | EMAIL, CPF, PHONE, RANDOM, RANDOM; chave inválida → `PaymentException`; tipo já informado não é sobrescrito. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `PaymentProcessingTest` (classe aninhada "7 caminhos independentes", 8 testes) |
| Técnica · Nível · Risco | Caixa-branca (caminhos independentes) · Unitário · R01 |

### CT-BB-FRETE-01

**Frete na fronteira de 2 kg**

| Campo | Valor |
| --- | --- |
| ID | CT-BB-FRETE-01 |
| Requisito relacionado | RF-04, RF-07 |
| Objetivo | Valor-limite do cálculo de frete por peso. |
| Pré-condições | Produto físico. |
| Dados de entrada | Peso 0,0; 1,99; 2,0; 2,01; 3,0 kg. |
| Passos | 1. Calcular o frete. |
| Resultado esperado | R$ 20,00; 20,00; 20,00; 20,08; 28,00. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `ProductAndOrderCalculationTest.shippingValueBoundaryAtTwoKilograms` |
| Técnica · Nível · Risco | Análise de valor-limite · Unitário · R11 |

### CT-ST-DT

**Tabela completa de transições de status**

| Campo | Valor |
| --- | --- |
| ID | CT-ST-DT |
| Requisito relacionado | RF-08 / D010 |
| Objetivo | Todas as 25 combinações de status atual × novo. |
| Pré-condições | Nenhuma. |
| Dados de entrada | 5 × 5 status. |
| Passos | 1. Consultar `canMoveTo` para cada par. |
| Resultado esperado | Permitido só avançar ou repetir; DELIVERED é final (15 permitidas, 10 proibidas). |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderStatusTest.transitionTable`; pelo serviço: `OrderServiceRulesTest.statusTransitionTable` (CT-ST-1 a 6) e `statusRegressionIsRejected` |
| Técnica · Nível · Risco | Tabela de decisão / transição de estados · Unitário, Regressão · R04 |

### CT-REG-D019-ST

**Trocas de status simultâneas**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D019-ST |
| Requisito relacionado | RNF-02 / D019 |
| Objetivo | Duas trocas simultâneas não fazem o pedido voltar. |
| Pré-condições | Pedido PROCESSING confirmado no banco. |
| Dados de entrada | Thread A: SHIPPED; thread B: PAID; 10 rodadas. |
| Passos | 1. Disparar as duas trocas juntas. 2. Ler o status final. |
| Resultado esperado | Status final SHIPPED; eventual falha só como `OrderException`. |
| Resultado obtido | Antes (`fc58a4b`): status final PAID → D019. Depois: conforme o esperado (E1). |
| Status | Passou |
| Evidência | `ConcurrencyTest.concurrentStatusUpdatesNeverMoveBack` |
| Técnica · Nível · Risco | Baseado em risco · Integração, Regressão · R04 |

### CT-ADDR-DT

**Alteração de endereço por status**

| Campo | Valor |
| --- | --- |
| ID | CT-ADDR-DT |
| Requisito relacionado | RF-05 |
| Objetivo | Endereço pode mudar até o envio. |
| Pré-condições | Pedido em cada status. |
| Dados de entrada | PENDING_PAYMENT, PROCESSING, PAID, SHIPPED, DELIVERED. |
| Passos | 1. Alterar o endereço. |
| Resultado esperado | Permitido nos 3 primeiros; `OrderException` em SHIPPED e DELIVERED. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `OrderServiceRulesTest.addressChangeDecisionTable` |
| Técnica · Nível · Risco | Tabela de decisão · Unitário · R12 |

### CT-PROD-DT

**Cadastro de produto por tipo**

| Campo | Valor |
| --- | --- |
| ID | CT-PROD-DT |
| Requisito relacionado | RF-04 / CA-04-01, CA-04-02 |
| Objetivo | Tipo × peso × link de download. |
| Pré-condições | Nenhuma. |
| Dados de entrada | PHYSICAL 1,5 kg; physical 0,0 kg; PHYSICAL sem peso; DIGITAL com link; DIGITAL com link em branco; DIGITAL sem link; BOOK. |
| Passos | 1. Criar cada produto. |
| Resultado esperado | Criados: físico com peso (inclusive 0) e digital com link. Rejeitados (`ProductException`, nada salvo): os demais. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `CatalogServicesTest.Products.decisionTable` |
| Técnica · Nível · Risco | Tabela de decisão · Unitário · R11 |

### CT-CLIENT-001

**Cliente sem documento obrigatório**

| Campo | Valor |
| --- | --- |
| ID | CT-CLIENT-001 |
| Requisito relacionado | RF-03 / CA-03-01, CA-03-02 / D008 |
| Objetivo | Erro de cliente, com o campo citado. |
| Pré-condições | Nenhuma. |
| Dados de entrada | INDIVIDUAL com CPF em branco; CORPORATE sem CNPJ. |
| Passos | 1. Criar o cliente. |
| Resultado esperado | `ClientException` mencionando `cpf`/`cnpj`. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `CatalogServicesTest.Clients.missingDocumentIsAClientError` |
| Técnica · Nível · Risco | Particionamento de equivalência · Unitário, Regressão · R09 |

### CT-REG-D011

**Excluir recurso inexistente e existente**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D011 |
| Requisito relacionado | D011 |
| Objetivo | DELETE coerente com GET nos 5 serviços. |
| Pré-condições | IDs inexistentes e existentes. |
| Dados de entrada | Produto, cliente, endereço, pedido e pagamento. |
| Passos | 1. Excluir id inexistente. 2. Excluir id existente. |
| Resultado esperado | 1: `ResourceNotFoundException` (404) sem chamar `deleteById`. 2: `deleteById` chamado. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | Testes `deletingMissing*` e `deletingExisting*` em `CatalogServicesTest`, `OrderServiceRulesTest`, `PaymentServiceTest` |
| Técnica · Nível · Risco | Particionamento de equivalência · Unitário, Regressão · R09, R11, R12 |

### CT-BD-001

**Migração V3 de dinheiro legado**

| Campo | Valor |
| --- | --- |
| ID | CT-BD-001 |
| Requisito relacionado | RNF-02, RNF-03 |
| Objetivo | Converter `double` legado para NUMERIC(12,2) com HALF_UP. |
| Pré-condições | Esquema isolado migrado até V2 com massa legada. |
| Dados de entrada | Preço 1,005; item 0,1 + 0,2; pagamento 1,004. |
| Passos | 1. Migrar para V3. 2. Ler os valores e o tipo da coluna. |
| Resultado esperado | 1,01; 0,30; 1,00; tipo `numeric`. |
| Resultado obtido | Conforme o esperado (E1, 09/10). |
| Status | Passou |
| Evidência | `FlywayMigrationTest.v3ConvertsLegacyDoubleMoneyToNumericHalfUp` |
| Técnica · Nível · Risco | Análise de valor-limite · Integração · R07 |

### CT-REG-D020

**Valor legado logo acima do limite**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D020 |
| Requisito relacionado | RNF-02 / D020 |
| Objetivo | A V3 falha em vez de arredondar para o máximo. |
| Pré-condições | Esquema em V2. |
| Dados de entrada | Preço 9.999.999.999,994 (e 10.000.000.000,00 no teste vizinho). |
| Passos | 1. Migrar para V3. |
| Resultado esperado | Migração falha com "acima de 9999999999.99". |
| Resultado obtido | Antes (`fc58a4b`): migração concluída → D020. Depois: conforme o esperado (E1). |
| Status | Passou |
| Evidência | `FlywayMigrationTest.v3RejectsLegacyValueJustAboveLimitBeforeRounding`, `v3FailsInsteadOfTruncatingValuesAboveColumnLimit` |
| Técnica · Nível · Risco | Análise de valor-limite · Integração, Regressão · R07 |

### CT-CONTRACT-001

**API ao vivo conforme o contrato OpenAPI**

| Campo | Valor |
| --- | --- |
| ID | CT-CONTRACT-001 |
| Requisito relacionado | RF-04, RNF-07 |
| Objetivo | Respostas reais validadas contra OpenAPI 3.1 / JSON Schema 2020-12. |
| Pré-condições | API isolada (perfil `contract`) e banco descartável. |
| Dados de entrada | Produto sintético; produto inválido; id inexistente. |
| Passos | 1. Ler `/v3/api-docs`. 2. Listar, criar e ler produto. 3. Enviar produto inválido. 4. Ler produto inexistente. |
| Resultado esperado | OpenAPI 3.1 válido com esquemas 400/404; corpos conformes; 400 e 404 com `ApiErrorResponse`. |
| Resultado obtido | Conforme o esperado (E2: 6 de 6 casos). |
| Status | Passou |
| Evidência | `scripts/http-contract.mjs`; `reports/contrato/spike-2026-10-04/http-swagger-errors-v1.json` |
| Técnica · Nível · Risco | Baseado em requisitos (contrato) · Sistema · R11 |

### CT-AUTH-001

**Acesso sem sessão e login inválido**

| Campo | Valor |
| --- | --- |
| ID | CT-AUTH-001 |
| Requisito relacionado | RF-01 / CA-01-01 |
| Objetivo | Rota protegida não responde sem sessão. |
| Pré-condições | API isolada com contas sintéticas. |
| Dados de entrada | Sem cookie; login sem CSRF; senha errada. |
| Passos | 1. `GET /auth/me` sem sessão. 2. Login sem token CSRF. 3. Login com senha errada. |
| Resultado esperado | 401; 403; 401 genérico. |
| Resultado obtido | Conforme o esperado (E2). |
| Status | Passou |
| Evidência | `scripts/http-session.mjs`; `reports/contrato/session-2026-10-04/http-session.json` |
| Técnica · Nível · Risco | Tabela de decisão · Sistema · R05 |

### CT-AUTH-002

**Vendedor não lê dados restritos**

| Campo | Valor |
| --- | --- |
| ID | CT-AUTH-002 |
| Requisito relacionado | RF-02 / CA-01-02 |
| Objetivo | Perfil VENDEDOR bloqueado nas rotas legadas de gerente. |
| Pré-condições | Sessão de vendedor. |
| Dados de entrada | Clientes, pagamentos, produtos e pedidos. |
| Passos | 1. Ler cada recurso como vendedor. 2. Mutação sem CSRF como gerente. 3. Logout sem CSRF. |
| Resultado esperado | 403 em todos; logout exige CSRF e invalida o cookie anterior. |
| Resultado obtido | Conforme o esperado (E2). |
| Status | Passou |
| Evidência | `scripts/http-session.mjs` |
| Técnica · Nível · Risco | Tabela de decisão · Sistema · R05 |

### CT-UI-LOGIN-001

**Login e logout pela interface**

| Campo | Valor |
| --- | --- |
| ID | CT-UI-LOGIN-001 |
| Requisito relacionado | RF-01, RF-16 |
| Objetivo | Fluxo real no navegador pelo proxy Next. |
| Pré-condições | Frontend e API isolados; conta sintética. |
| Dados de entrada | Credenciais sintéticas do runner. |
| Passos | 1. Entrar. 2. Recarregar a página. 3. Sair. |
| Resultado esperado | Sessão mantida após recarregar, cookie HttpOnly, logout encerra a sessão. |
| Resultado obtido | Conforme o esperado (E3). |
| Status | Passou |
| Evidência | `frontend/e2e/session.spec.mjs` |
| Técnica · Nível · Risco | Baseado em requisitos · Aceitação (E2E) · R13 |

### CT-UI-LOGIN-002

**Erro de senha acessível e tela móvel**

| Campo | Valor |
| --- | --- |
| ID | CT-UI-LOGIN-002 |
| Requisito relacionado | RNF-05 |
| Objetivo | Estado de erro e layout em viewport pequeno. |
| Pré-condições | Frontend em execução. |
| Dados de entrada | Senha errada; viewport móvel. |
| Passos | 1. Entrar com senha errada. 2. Conferir a mensagem e o formulário. |
| Resultado esperado | Mensagem de erro acessível; formulário cabe na largura. |
| Resultado obtido | Conforme o esperado (E3). |
| Status | Passou |
| Evidência | `frontend/e2e/session.spec.mjs` |
| Técnica · Nível · Risco | Testes de casos inválidos · Aceitação (E2E) · R13 |

### CT-REG-D021

**Senha do banco ausente**

| Campo | Valor |
| --- | --- |
| ID | CT-REG-D021 |
| Requisito relacionado | RNF-04 / D021 |
| Objetivo | Falhar cedo com mensagem clara. |
| Pré-condições | Ambiente sem a variável. |
| Dados de entrada | Sem propriedade; placeholders `${TEST_DB_PASSWORD}`/`${SPRING_DATASOURCE_PASSWORD}` não resolvidos; vazio; espaços; senha resolvida com espaço. |
| Passos | 1. Executar a verificação de inicialização em cada caso. |
| Resultado esperado | Os 5 primeiros interrompem com mensagem citando as duas variáveis; a senha resolvida passa. |
| Resultado obtido | Conforme o esperado (09/10, branch do PR #6 @ `21f06a3`; também verificado executando o jar sem a variável). |
| Status | Passou |
| Evidência | `DatasourcePasswordGuardTest` |
| Técnica · Nível · Risco | Testes de casos inválidos · Unitário, Regressão · R10 |
