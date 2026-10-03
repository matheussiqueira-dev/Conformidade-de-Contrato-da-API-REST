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

## Regras para atualizar a matriz

- Registrar o commit, ambiente, massa de dados e comando de cada execucao.
- Quando um criterio falhar, criar ou vincular defeito antes de marcar como tratado.
- Nao encerrar criterio dependente de autenticacao, estoque ou frontend antes de existir implementacao executavel.
- Manter IDs estaveis; se o texto mudar, registrar a decisao em `docs/validacao-requisitos.md`.
