# Criterios de aceitacao por requisito

Data: 03/10/2026.

Este documento inicia a matriz RF -> CA -> CT -> execucao -> defeito. Os casos de teste e execucoes ainda serao detalhados no catalogo e nos relatorios. IDs marcados como `Pendente` nao devem ser tratados como aprovados.

## Criterios em formato Dado/Quando/Entao

### RF-01 e RF-02 - Autenticacao e autorizacao

| ID | Criterio |
| --- | --- |
| CA-01-01 | Dado um usuario sem sessao, quando acessar rota protegida, entao a API retorna 401 e nao revela dados de negocio. |
| CA-01-02 | Dado um vendedor autenticado, quando tentar acessar rota de gerente, entao a API retorna 403 e nao inclui custo, margem ou resultado de colegas no JSON. |
| CA-01-03 | Dado um gerente autenticado, quando acessar dados consolidados, entao a API retorna loja, equipe, metas e estoque conforme o periodo solicitado. |

### RF-03 e RF-04 - Clientes e produtos do baseline

| ID | Criterio |
| --- | --- |
| CA-03-01 | Dado cliente individual sem CPF, quando criar cliente, entao a API rejeita a requisicao com erro 400 explicito. |
| CA-03-02 | Dado cliente corporativo sem CNPJ, quando criar cliente, entao a API rejeita a requisicao com erro 400 explicito. |
| CA-04-01 | Dado produto fisico sem peso, quando criar produto, entao a API rejeita a requisicao. |
| CA-04-02 | Dado produto digital sem link de download, quando criar produto, entao a API rejeita a requisicao. |

### RF-05, RF-06 e RF-07 - Pedido, vendedor e total

| ID | Criterio |
| --- | --- |
| CA-05-01 | Dado cliente existente e itens validos, quando criar pedido, entao a API cria pedido com status `PENDING_PAYMENT`, endereco e pagamento pendente. |
| CA-05-02 | Dado cliente novo valido, quando criar pedido, entao a API persiste o cliente junto com o pedido. |
| CA-05-03 | Dado pedido sem cliente existente nem dados de cliente novo, quando criar pedido, entao a API retorna erro 400. |
| CA-06-01 | Dado vendedor autenticado, quando criar pedido, entao o vendedor historico do pedido e atribuido pela sessao. |
| CA-06-02 | Dado payload com campo de vendedor manipulado, quando criar pedido, entao o servidor ignora ou rejeita o campo e preserva a identidade da sessao. |
| CA-07-01 | Dado item de R$ 4.500 e item de R$ 1.200 com quantidade 1, quando criar pedido sem desconto, entao subtotal e total retornam R$ 5.700. |
| CA-07-02 | Dado produto com quantidade 2 e preco unitario R$ 100, quando criar pedido, entao o total do item e R$ 200, nao R$ 400. |
| CA-07-03 | Dado alteracao posterior no catalogo, quando consultar venda passada, entao o preco unitario historico do item permanece inalterado. |

### RF-08, RF-09 e RF-10 - Pagamentos, metas e realizado

| ID | Criterio |
| --- | --- |
| CA-08-01 | Dado pagamento pendente, quando processar com dados validos, entao status do pagamento fica aprovado e pedido fica pago. |
| CA-08-02 | Dado evento repetido de confirmacao de pagamento, quando reenviado, entao realizado e estoque nao duplicam efeitos. |
| CA-08-03 | Dado estorno aprovado, quando calcular realizado do periodo, entao o valor estornado e subtraido. |
| CA-09-01 | Dado meta da loja de R$ 240.000 e tres vendedores com R$ 80.000, quando salvar distribuicao, entao a soma dos vendedores corresponde a meta da loja. |
| CA-09-02 | Dado distribuicao diferente da meta da loja, quando salvar, entao a API rejeita a configuracao ou registra pendencia explicita. |
| CA-10-01 | Dado Ana com meta R$ 80.000 e realizado R$ 68.400, quando consultar desempenho, entao o atingimento retorna 85,5%. |
| CA-10-02 | Dado vendedor com meta zero, quando consultar desempenho, entao a resposta exibe estado `Sem meta` e nao divide por zero. |
| CA-10-03 | Dado pedido com pagamento pendente, quando calcular realizado, entao o pedido nao entra no total. |

### RF-11 e RF-12 - Meu desempenho e Gestao

| ID | Criterio |
| --- | --- |
| CA-11-01 | Dado vendedor Ana autenticado, quando consultar Meu desempenho, entao a resposta traz apenas meta, realizado e vendas da Ana. |
| CA-11-02 | Dado vendedor Ana autenticado, quando tentar consultar desempenho de Bruno por ID, entao a API retorna 403 ou 404 sem vazar dados. |
| CA-12-01 | Dado gerente autenticado, quando consultar Gestao do periodo, entao a resposta traz total da loja, equipe, metas e alertas de estoque. |
| CA-12-02 | Dado periodo sem dados, quando consultar Gestao, entao a interface/API retorna estado vazio claro e sem numeros ficticios. |

### RF-13 e RF-14 - Estoque, reserva e custo

| ID | Criterio |
| --- | --- |
| CA-13-01 | Dado saldo fisico 1 e reservado 0, quando dois pedidos tentam reservar a ultima unidade ao mesmo tempo, entao apenas um reserva com sucesso. |
| CA-13-02 | Dado pedido cancelado antes da confirmacao, quando cancelar, entao a reserva e liberada e o disponivel aumenta. |
| CA-13-03 | Dado pagamento confirmado, quando aplicar efeitos, entao reserva vira saida e nao permanece como reservado. |
| CA-14-01 | Dado gerente autenticado, quando registrar entrada de lote, entao quantidade, custo e preco sao persistidos com auditoria. |
| CA-14-02 | Dado vendedor autenticado, quando listar produtos disponiveis, entao custo e margem nao aparecem no JSON. |
| CA-14-03 | Dado venda passada, quando custo ou preco de catalogo muda, entao o custo/preco atribuido a venda passada permanece historico. |

### RF-15 e RF-16 - Interface e fluxo de venda

| ID | Criterio |
| --- | --- |
| CA-15-01 | Dado cliente existente, quando iniciar venda, entao o fluxo permite selecionar cliente, produtos disponiveis e pagamento antes de criar pedido. |
| CA-15-02 | Dado falta de estoque, quando criar pedido, entao o erro preserva o formulario e informa o motivo sem duplicar pedido. |
| CA-16-01 | Dado gerente autenticado, quando abrir a interface, entao a navegacao mostra Gestao, Vender e Estoque administrativo. |
| CA-16-02 | Dado vendedor autenticado, quando abrir a interface, entao a navegacao mostra Meu desempenho e Vender, sem Estoque administrativo. |
| CA-16-03 | Dado erro de rede ou API, quando carregar qualquer area, entao a interface mostra estado de erro recuperavel. |

### RF-17 a RF-23 - Evolucao ERP/PDV (14/10/2026)

Codigos de erro citados abaixo sao o campo `code` do `ErrorResponse` do contrato v4.

| ID | Criterio |
| --- | --- |
| CA-17-01 | Dado gerente autenticado, quando acessar qualquer operacao liberada ao vendedor, entao a API nao retorna 401 nem 403, tanto na regra de URL quanto na regra de metodo. |
| CA-17-02 | Dado vendedor autenticado, quando acessar operacao exclusiva do gerente, entao a API retorna 403 `FORBIDDEN`. |
| CA-17-03 | Dado usuario autenticado, quando consultar `GET /auth/me`, entao `areas` lista VENDER, VENDAS, CLIENTES e MEU_DESEMPENHO para o vendedor e acrescenta GESTAO, PRODUTOS, ESTOQUE, PROMOCOES, ANALISES e USUARIOS para o gerente; com senha temporaria, `areas` traz apenas TROCAR_SENHA. |
| CA-18-01 | Dada conta ativa, quando ocorrerem 4 falhas seguidas, entao a conta nao e bloqueada; na 5a falha, fica bloqueada por 15 minutos. |
| CA-18-02 | Dada conta bloqueada, quando a senha correta for enviada, entao a API retorna 401 com corpo igual (salvo timestamp) ao de usuario inexistente. |
| CA-18-03 | Dada conta bloqueada ha 15 minutos, quando a senha correta for enviada, entao a API retorna 200 e zera o contador de falhas. |
| CA-18-04 | Dada conta inativa, quando a senha correta for enviada, entao a API retorna o mesmo 401. |
| CA-18-05 | Dado qualquer um dos quatro casos de falha, quando o login for processado, entao a comparacao BCrypt e executada e um `audit_event` e gravado sem senha e com o e-mail apenas como SHA-256. |
| CA-24-01 | Dado gerente, quando cadastrar usuario, entao o e-mail e gravado em minusculas, a conta exige troca de senha e e-mail repetido em outra caixa retorna 409 `STATE_CONFLICT`; senha fora da politica retorna 400 `VALIDATION_FAILED`. |
| CA-24-02 | Dado vendedor, quando acessar qualquer rota de `/users`, entao a API retorna 403 `FORBIDDEN`. |
| CA-24-03 | Dada alteracao de papel, situacao, senha ou desbloqueio de um usuario, quando ele fizer a proxima requisicao com a sessao antiga, entao a API retorna 401 `SESSION_REVOKED`. |
| CA-24-04 | Dado gerente, quando tentar alterar o proprio papel, a propria situacao ou redefinir a propria senha, entao a API retorna 409 `SELF_CHANGE_FORBIDDEN`; quando a operacao deixaria a loja sem gerente ativo, 409 `LAST_MANAGER`. |
| CA-24-05 | Dada senha provisoria, quando o usuario entrar, entao `areas` traz apenas TROCAR_SENHA e as demais rotas retornam 403 `PASSWORD_CHANGE_REQUIRED` ate a troca; senha atual errada na troca retorna 401 e conta para o bloqueio. |
| CA-24-06 | Dada qualquer operacao de usuario, quando a resposta ou o `audit_event` forem gravados, entao nenhum contem senha ou hash. |
| CA-25-01 | Dado gerente, quando consultar `GET /customers/{id}/summary`, entao total comprado (pagos menos estornos), ticket medio, estornos e formas de pagamento cobrem a loja toda (`scope` STORE), com pendentes e cancelados so como contagem. |
| CA-25-02 | Dado vendedor, quando consultar o resumo, entao total, ticket medio e formas de pagamento cobrem so os pedidos dele (`scope` OWN) e `refundsTotal` e nulo; contagens, datas e produtos mais comprados cobrem a loja toda. |
| CA-25-03 | Dado cliente sem compras, quando consultar o resumo, entao contagens e total sao zero, ticket medio e datas sao nulos e as listas vem vazias. |
| CA-20-06 | Dado CNPJ alfanumerico valido (ex.: 12.ABC.345/01DE-35), quando cadastrar pessoa juridica, entao a API grava sem mascara em maiusculas, mascara as pontas para o vendedor e acha o cliente pela busca exata; digito verificador errado ou letra minuscula no corpo retorna 400 `VALIDATION_FAILED`; repetido retorna 409 `DUPLICATE_DOCUMENT`. |
| CA-26-01 | Dado vendedor, quando cotar ou vender com desconto manual ate 5% e motivo, entao a API calcula o valor sobre o subtotal ja promocionado (HALF_UP), rateia nos itens e grava o mesmo total no pedido e no pagamento. |
| CA-26-02 | Dado desconto acima da alcada do papel da sessao, quando cotar ou vender, entao a API retorna 403 `DISCOUNT_ABOVE_LIMIT` com o limite em `details` e nao cria pedido nem reserva. |
| CA-26-03 | Dado item que ficaria abaixo do custo medio conhecido, quando a politica nao permitir, entao a API retorna 409 `BELOW_COST` sem revelar o custo. |
| CA-26-04 | Dado pedido pendente, quando o gerente aprovar um desconto ate 20%, entao total, itens e pagamento sao recalculados, o aprovador fica registrado e a auditoria grava `DISCOUNT_APPROVED`; pedido pago retorna 409. |
| CA-26-05 | Dado relatorio de vendas do mes, quando houver descontos manuais em pedidos pagos, entao `manualDiscounts` mostra por vendedor pedidos, valor concedido e aprovados pelo gerente, e o uso de promocoes nao inclui a parte manual. |
| CA-19-01 | Dado vendedor no PDV, quando buscar por SKU ou prefixo do nome, entao o SKU exato vem primeiro e a resposta nao contem custo, saldo fisico nem margem. |
| CA-19-02 | Dado carrinho, quando cotar, entao preco, desconto e total vem do servidor sem efeito colateral; corpo de venda com vendedor, preco, desconto ou total retorna 400 sem criar pedido. |
| CA-19-03 | Dada a mesma `Idempotency-Key` e o mesmo corpo, quando a venda for reenviada, entao a API retorna 200 com o mesmo pedido; com corpo diferente, 409 `IDEMPOTENCY_CONFLICT`. |
| CA-19-04 | Dado item sem estoque suficiente, quando finalizar a venda, entao a API retorna 409 `STOCK_INSUFFICIENT` e nenhuma reserva do pedido permanece. |
| CA-19-05 | Dada venda pendente, quando o pagamento for confirmado duas vezes, entao o estoque baixa uma unica vez e a segunda chamada retorna 200 sem efeito. |
| CA-19-06 | Dada a ultima unidade, quando 10 vendas simultaneas tentarem reserva-la, entao exatamente uma e criada e nove recebem 409; o disponivel nunca fica negativo. |
| CA-19-07 | Dada venda pendente do proprio vendedor, quando ele cancelar, entao a reserva e liberada; venda de outro vendedor retorna 404; venda paga retorna 409 `STATE_CONFLICT`. |
| CA-20-01 | Dado CPF ou CNPJ com digito verificador invalido, quando cadastrar, entao a API retorna 400; documento valido com mascara e gravado so com digitos. |
| CA-20-02 | Dado documento ja cadastrado, quando cadastrar outro cliente com ele, entao a API retorna 409 `DUPLICATE_DOCUMENT`. |
| CA-20-03 | Dado vendedor autenticado, quando listar ou abrir cliente, entao o documento vem mascarado. |
| CA-20-04 | Dada edicao de cliente, quando o corpo trouxer `cpf` ou `cnpj`, entao a API retorna 400; com `version` desatualizada, 409 `CONCURRENT_UPDATE`. |
| CA-20-05 | Dado vendedor, quando excluir cliente, entao a API retorna 403; dado gerente e cliente com pedido, 409 `RESOURCE_IN_USE`. |
| CA-21-01 | Dado cliente com pedidos, quando consultar o historico, entao os pedidos vem paginados do mais recente para o mais antigo, com itens. |
| CA-21-02 | Dado vendedor, quando consultar o historico, entao total e vendedor aparecem so nos pedidos dele, e nenhum pedido traz custo ou margem. |
| CA-22-01 | Dado gerente, quando alterar preco com motivo de 5 a 200 caracteres, entao o historico registra autor e motivo, e itens vendidos antes mantem o preco antigo. |
| CA-22-02 | Dada entrada de estoque com custo, quando registrada, entao o custo medio ponderado e recalculado pelo gatilho; entrada sem custo deixa o custo medio desconhecido. |
| CA-22-03 | Dado ajuste de saida acima do disponivel, quando registrado, entao a API retorna 409 `STOCK_INSUFFICIENT`; ajuste sem motivo retorna 400; ajuste de entrada nao altera o custo medio. |
| CA-22-04 | Dado produto ja vendido, quando o gerente tentar exclui-lo, entao a API retorna 409; inativado, ele some do PDV. |
| CA-22-05 | Dado insumo (`sellable = false`), quando buscar no PDV, entao ele nao aparece; venda que o inclua retorna 409 `STATE_CONFLICT`. |
| CA-22-06 | Dado extrato de movimentos, quando filtrar por produto, tipo e periodo, entao cada movimento traz tipo, quantidade, motivo, pedido e autor. |
| CA-22-07 | Dado estoque minimo definido, quando o disponivel ficar igual ou abaixo dele, entao a posicao marca `belowMin` e o painel conta o produto no alerta. |
| CA-23-01 | Dado gerente, quando consultar faturamento do periodo, entao o liquido e vendas pagas menos estornos do periodo, lido das views do V6. |
| CA-23-02 | Dado item vendido sem custo no periodo, quando consultar faturamento, entao lucro bruto e margem vem nulos e a cobertura de custo e informada. |
| CA-23-03 | Dado painel de gestao aberto, quando a aba estiver visivel, entao os dados sao recarregados a cada 15 s; com a aba oculta, nao; erro de carga nunca aparece como zero. |

## Matriz inicial de rastreabilidade

| RF | Criterios | Casos de teste planejados | Execucao | Defeito |
| --- | --- | --- | --- | --- |
| RF-01 | CA-01-01 | CT-AUTH-001 | Pendente | - |
| RF-02 | CA-01-02, CA-01-03 | CT-AUTH-002, CT-AUTH-003 | Pendente | - |
| RF-03 | CA-03-01, CA-03-02 | CT-CLIENT-001, CT-CLIENT-002 | Pendente | - |
| RF-04 | CA-04-01, CA-04-02 | CT-PRODUCT-001, CT-PRODUCT-002 | Pendente | - |
| RF-05 | CA-05-01 a CA-05-03 | CT-ORDER-001 a CT-ORDER-003 | Pendente | - |
| RF-06 | CA-06-01, CA-06-02 | CT-ORDER-004, CT-SEC-001 | Pendente | - |
| RF-07 | CA-07-01 a CA-07-03 | CT-MONEY-001 a CT-MONEY-003 | CT-MONEY-002 retestado em 03/10/2026 | Defeito confirmado e corrigido: item gravava preco ja multiplicado pela quantidade. CT-MONEY-001 e CT-MONEY-003 seguem pendentes. |
| RF-08 | CA-08-01 a CA-08-03 | CT-PAY-001 a CT-PAY-003 | Pendente | - |
| RF-09 | CA-09-01, CA-09-02 | CT-GOAL-001, CT-GOAL-002 | Pendente | - |
| RF-10 | CA-10-01 a CA-10-03 | CT-GOAL-003 a CT-GOAL-005 | Pendente | - |
| RF-11 | CA-11-01, CA-11-02 | CT-PERF-001, CT-SEC-002 | Pendente | - |
| RF-12 | CA-12-01, CA-12-02 | CT-MGMT-001, CT-MGMT-002 | Pendente | - |
| RF-13 | CA-13-01 a CA-13-03 | CT-STOCK-001 a CT-STOCK-003 | Pendente | - |
| RF-14 | CA-14-01 a CA-14-03 | CT-COST-001 a CT-COST-003 | Pendente | - |
| RF-15 | CA-15-01, CA-15-02 | CT-E2E-001, CT-E2E-002 | Pendente | - |
| RF-16 | CA-16-01 a CA-16-03 | CT-UI-001 a CT-UI-003 | Pendente | - |
| RF-17 | CA-17-01 a CA-17-03 | CT-SEC-101, CT-SEC-103, CT-SEC-114, CT-WEB-103 | Pendente | - |
| RF-18 | CA-18-01 a CA-18-05 | CT-AUTH-101, CT-AUTH-102 | Pendente | - |
| RF-19 | CA-19-01 a CA-19-07 | CT-SALE-101 a CT-SALE-104, CT-STOCK-101 a CT-STOCK-107, CT-SEC-110 a CT-SEC-113, CT-SEC-116, CT-WEB-101, CT-WEB-104 | Pendente | - |
| RF-20 | CA-20-01 a CA-20-06 | CT-CRM-101, CT-CRM-102, CT-CRM-104, CT-CRM-105, CT-CRM-106, CT-CONTRACT-101, CT-WEB-113 | Pendente | CA-20-06 verde em 14/10/2026 (`AlphanumericCnpjTest`, `CustomersApiTest.alphanumericCnpjIsSupported`, `FlywayMigrationTest.v10AcceptsAlphanumericCnpj`) |
| RF-21 | CA-21-01, CA-21-02 | CT-CRM-103, CT-SEC-113 | Pendente | - |
| RF-22 | CA-22-01 a CA-22-07 | CT-STOCK-108, CT-STOCK-109, CT-SALE-104, CT-WEB-105, CT-WEB-106 | Pendente | - |
| RF-23 | CA-23-01 a CA-23-03 | CT-CONTRACT-102, CT-WEB-102 | Pendente | - |
| RF-26 | CA-26-01 a CA-26-05 | CT-DISC-101 a CT-DISC-107, CT-WEB-114, BD-32 | Verde (14/10/2026) | `ManualDiscountApiTest`, `ManualDiscountPricingTest`, `e2e/discount.spec.ts`, job `bd` |
| RF-25 | CA-25-01 a CA-25-03 | CT-CUS-121 a CT-CUS-123, CT-WEB-111 | Verde (14/10/2026) | `CustomersApiTest` (resumo), BD-31 no job `bd`, `e2e/customers.spec.ts` |
| RF-24 | CA-24-01 a CA-24-06 | CT-USR-101 a CT-USR-107, CT-SEC-121, CT-SEC-122, CT-BD-30, CT-WEB-112 | Verde (14/10/2026) | `UsersApiTest`, `SessionRevocationTest`, `FlywayMigrationTest.v8KeepsOneActiveManager`, `06d_testes_evolucao.sql`, `e2e/users.spec.ts` |

Situacao atual de cada linha, com a evidencia executavel: [rastreabilidade.md](rastreabilidade.md) (14/10/2026).

## Regras para atualizar a matriz

- Registrar o commit, ambiente, massa de dados e comando de cada execucao.
- Quando um criterio falhar, criar ou vincular defeito antes de marcar como tratado.
- Nao encerrar criterio dependente de autenticacao, estoque ou frontend antes de existir implementacao executavel.
- Manter IDs estaveis; se o texto mudar, registrar a decisao em `docs/validacao-requisitos.md`.
