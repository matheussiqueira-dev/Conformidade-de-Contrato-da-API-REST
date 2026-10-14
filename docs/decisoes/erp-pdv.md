# Decisões ERP/PDV

Data: 14/10/2026. Registro das decisões da evolução ERP/PDV (RF-17 a RF-23), uma frase cada. Detalhes: [modelo de dados](../bd/modelo-erp-pdv.md), [autenticação e RBAC](autenticacao-sessao-rbac.md), [matriz de acesso](matriz-acesso.md) e contrato `config/openapi/target-loja-gestao-openapi-2026-10-14-v4.json`.

## Decisões pendentes da equipe (opção padrão adotada)

| ID | Decisão |
| --- | --- |
| DEC-ERP-01 | O VENDEDOR mantém a área Meu desempenho (`GET /performance/me`). |
| DEC-ERP-02 | No histórico do cliente, o VENDEDOR vê todos os pedidos com data, status e itens; total e vendedor só nos pedidos dele; nunca custo ou margem. |
| DEC-ERP-03 | Insumo é produto com `sellable = false`: fica fora do catálogo do PDV, e venda que o inclua responde 409. |
| DEC-ERP-04 | Cartão no balcão grava só os 4 últimos dígitos e o código de autorização opcional, nunca o número completo. |
| DEC-ERP-05 | Toda venda exige cliente identificado; o PDV oferece cadastro rápido em painel lateral. |
| DEC-ERP-06 | O VENDEDOR cancela a própria venda pendente; a de outro vendedor só o GERENTE cancela. |
| DEC-ERP-07 | Gestão de usuários fica fora do escopo; as contas vêm do seed demo. |
| DEC-ERP-08 | Dinheiro com 2 casas e HALF_UP até decisão diferente em `docs/decisoes/dinheiro.md`. |

## Reconciliação do contrato v3 → v4

| ID | Decisão |
| --- | --- |
| REC-a | `StockEntryRequest.salePrice` passa a opcional: `stock_entry` continua sem preço, e um `salePrice` diferente do atual altera o preço do produto com histórico e motivo "Entrada de estoque". |
| REC-b | `StockProduct` segue a seção 6 (`onHand`, `reserved`, `available`, `minLevel`, `belowMin`, `avgCost`, paginado) e substitui `physicalBalance`/`salePrice` do v3. |
| REC-c | `SalesOrderRequest` fica fechado: `customerId` obrigatório, `payment` em `oneOf` por `type`, `fulfillment` obrigatório e `shippingAddress` exigido só em `DELIVERY`. |
| REC-d | O erro passa a se chamar `ErrorResponse` no OpenAPI (sai `ApiErrorResponse`), com `timestamp` `date-time` com offset, `details` como lista de textos ou `null` e `code` obrigatório. |
| REC-e | `/management/summary` continua sendo o resumo de metas, agora fechado e sem `stockAlerts`; faturamento vai para `/management/revenue` (DELTA-05). |
| REC-f | `UserSession` ganha `areas` e continua em `AuthResponse`; `SellerPerformance.seller` e as demais referências a pessoas passam a `UserRef {id, name}`. |
| REC-g | `POST /orders/{id}/cancel` mantém `x-roles [GERENTE, VENDEDOR]`, com o dono verificado no serviço, e passa a responder `SalesOrderResponse`. |

## Contrato

| ID | Decisão |
| --- | --- |
| C-01 | O v4 documenta só as rotas-alvo; as rotas legadas continuam descritas nos baselines e aparecem na matriz de acesso. |
| C-02 | Toda operação do rascunho v4 nasce `x-status: planned` e só vira `implemented` na etapa que a entrega com teste. |
| C-03 | O v4 usa `x-contract-status: target-v4`; v2 e v3 continuam com `target-planned-not-implemented` e só aceitam `planned`. |
| C-04 | `scripts/audit-contract.mjs` cruza `x-roles` do v4 com `access-matrix.csv` e exige objetos fechados em todo schema do v4. |
| C-05 | Valores monetários usam `Money {amount, currency: "BRL"}` também nas requisições. |
| C-06 | Cancelar pedido já `CANCELED` responde 200 sem efeito, simétrico à confirmação repetida; pedido pago responde 409 `STATE_CONFLICT`. |
| C-07 | Estorno responde 201 com `RefundResponse` (o v3 previa 200 com `ActionResult`), porque cria um recurso. |
| C-08 | A promoção entra no item só por `promotionId` explícito, que o frontend pré-preenche com `activePromotion`; promoção inválida ou vencida responde 409 `STATE_CONFLICT`. |
| C-09 | `activePromotion` é a promoção vigente de maior desconto para uma unidade; empate fica com o menor id. |
| C-10 | No `GET /sales/orders`, VENDEDOR que envia `sellerId` diferente do próprio recebe 403. |
| C-11 | A busca de clientes remove pontuação de `q`; se sobrarem só dígitos, busca documento exato; senão, prefixo do nome. |
| C-12 | O GERENTE recebe o documento formatado completo; o VENDEDOR, mascarado (`***.456.789-**`, `**.345.678/0001-**`). |
| C-13 | Na cotação, falta de estoque responde 200 com `available`; produto inativo ou insumo, 409; produto inexistente, 404. |
| C-14 | Venda e cotação aceitam de 1 a 50 itens com quantidade de 1 a 999; `productId` repetido responde 400 pela validação Bean Validation. |
| C-15 | Ajuste de entrada (`IN`) só aceita o motivo `INVENTARIO`. |
| C-16 | O SKU é editável no `PUT /products/{id}`, gravado em maiúsculas; SKU ou nome de categoria duplicado responde 409 `STATE_CONFLICT`. |
| C-17 | Produto mantém o tipo `PHYSICAL`/`DIGITAL` em `oneOf`; preço muda só por `PATCH /price` e situação só por `PATCH /status`. |
| C-18 | Paginação começa em `page = 0`, com `size` de 1 a 100 e padrão 20. |
| C-19 | `SalesOrderResponse` inclui `shippingAddress` (nulo em retirada). |
| C-20 | Lucro bruto = faturamento líquido − custo conhecido; fica nulo se algum item vendido no período não tiver custo, e a margem é lucro ÷ líquido. |
| C-21 | Períodos de relatório são datas fechadas em America/Fortaleza. |
| C-22 | A mudança de `timestamp` do erro para `date-time` com offset quebra a checagem HTTP atual do baseline de 06/10 e é tratada na ETAPA 2B, junto com o handler. |

## Banco

| ID | Decisão |
| --- | --- |
| B-01 | `app_users.db_role` do pacote é mantido, porque `app_current_user_id()` e a RLS demonstrada via `psql` dependem dele. |
| B-02 | A V5 começa com uma pré-checagem que aborta, listando ids, se documentos colidirem após a normalização ou se dados legados violarem os novos CHECKs. |
| B-03 | As funções `fn_*` percorrem itens em ordem crescente de `product_id`, exigem `sellable` e gravam `fulfillment = 'PICKUP'`. |
| B-04 | `v_catalogo_vendedor` passa a filtrar `sellable`. |
| B-05 | O final do cartão vai para a nova coluna `card_last4`, porque `card_number` guarda o número completo enviado pelo `/payment` legado. |
| B-06 | `client_request_id` e `request_hash` são preenchidos juntos, e o hash tem 64 dígitos hexadecimais minúsculos. |
| B-07 | `ix_stock_movement_created_at` atende o extrato de movimentos sem filtro de produto. |
| B-08 | `customer.created_by` tem `DEFAULT app_current_user_id()`, a entidade o preenche com o usuário da sessão, e os clientes legados ficam com autor nulo. |
| B-09 | Os painéis leem as views por `JdbcClient` com `record`, sem mapear views como entidades JPA. |
| B-10 | `orders.moment` e as datas de pagamento vêm de um `Clock` com zona America/Fortaleza, porque as colunas são `timestamp` sem fuso e a CI roda em UTC. |
| B-11 | O seed demo fica em `classpath:db/demo` como `V6_1` (contas, migração Java com BCrypt de `SBI_DEMO_PASSWORD`) e `V6_2` (massa SQL), entre a V6 e qualquer V7; o perfil `demo` precisa estar ativo desde a primeira migração do banco (A2A-4). |
| B-12 | A massa demo usa CPFs fictícios com dígito verificador válido (`10000000108` a `10000000604`). |
| B-13 | Sem índice em `promotion(product_id)`, `promotion(category_id)` e `customer(created_by)` até um `EXPLAIN` sobre o seed mostrar necessidade. |

## Conflitos novos encontrados (fora dos DELTA conhecidos)

| ID | Conflito | Tratamento |
| --- | --- | --- |
| N-01 | A `main` ainda tem `ddl-auto=update` e Flyway desligado no `application.properties`, contra a decisão A2. | Corrigido na ETAPA 2A, como já previsto. |
| N-02 | O `ck_payment_status` do pacote não aceita `EXPIRED`, que o Java usa. | D10. |
| N-03 | `v_catalogo_vendedor` e `fn_criar_pedido` do pacote não conhecem `sellable`. | B-03 e B-04. |
| N-04 | As funções do pacote reservam na ordem do pedido, não em ordem crescente de `product_id`. | B-03. |
| N-05 | O `/payment` legado grava o número completo do cartão em `card_number`, contra a DEC-ERP-04. | Registrado como defeito D022 em [defeitos.md](../defeitos.md); correção na ETAPA 2C (A2A-3). |
| N-06 | O `POST /customers` legado salva o documento como recebido; com os CHECKs da D3, CPF com máscara cairia no handler genérico de integridade com a mensagem "This order already has a payment.". | Normalizar na ETAPA 2A. |
| N-07 | O `POST /orders` legado não informa `fulfillment`; pedidos com endereço receberiam o padrão `PICKUP`. | Mapear `fulfillment` na entidade na ETAPA 2A. |
| N-08 | `Order.moment` usa o fuso da JVM, e as views assumem America/Fortaleza. | B-10. |
| N-09 | O SBI-EXECUCAO cita o rewrite `/api/:path*` e `API_ORIGIN`; o código usa `/api/backend/:path*` e `BACKEND_ORIGIN`. | Vale o código (DELTA já citado no prompt da etapa). |

## Respostas para a ETAPA 2A (14/10/2026)

| ID | Decisão |
| --- | --- |
| A2A-1 | Somente o PR #10 é integrado (contém a DOM-02 e a pré-checagem D020); o #9 é fechado sem merge, como registrado em [integracao-fase0.md](integracao-fase0.md). |
| A2A-2 | Não existe banco não descartável: o `postgres-order` de desenvolvimento é recriado do zero com Flyway e seed demo, com `pg_dump` antes por precaução, sem baseline manual; se a pré-checagem da V5 encontrar dado inválido, a migração falha com mensagem clara e nenhum dado é corrigido. |
| A2A-3 | O número completo do cartão no `/payment` legado é o defeito D022 (N-05), corrigido na ETAPA 2C (serviço grava só os 4 últimos dígitos, migração mascara os dados existentes, teste); a ETAPA 2A não cria CHECK em `card_number`. |
| A2A-4 | O seed demo fica em `db/demo` como `V6_1`/`V6_2` e exige o perfil `demo` ativo desde a primeira migração do banco, com `outOfOrder` desligado: ativar o perfil num banco já migrado além da V6 faz o Flyway recusar as versões pendentes, e o banco deve ser recriado. |

## Decisões da execução noturna

| ID | Decisão |
| --- | --- |
| D-NOITE-01 | A hora da loja (B-10) vem de `entities/StoreTime`, um `Clock` fixo em America/Fortaleza usado por `Order.moment` e pelas datas de pagamento, em vez de um `Clock` no construtor dos serviços, que quebraria os testes montados com `@InjectMocks`. |
| D-NOITE-02 | As entidades de cliente só removem a máscara do documento (`Documents.digitsOnly`); os serviços que recebem DTO validam o tamanho (`Documents.cpf/cnpj`, 400) e o CHECK da V5 continua como última barreira. |
| D-NOITE-03 | A lista das 17 tabelas de negócio esvaziadas por `TRUNCATE ... RESTART IDENTITY CASCADE` fica em `config/BusinessTables.java`, compartilhada pelos testes com transação confirmada e pelo runner de contrato. |
| D-NOITE-05 | Até a ETAPA 2C trocar o `/customers` legado (que devolve o documento completo) pelas projeções mascaradas, `GET /customers`, `POST /customers`, `GET /customers/{id}` e `PUT /customers/{id}` ficam só com o GERENTE na regra de URL e no `@PreAuthorize` do controlador; o `AccessMatrixTest` lista essas quatro linhas como pendentes da 2C e espera 403 para o VENDEDOR. |
| D-NOITE-06 | As operações de autenticação do v4 continuam `planned` na ETAPA 2B, apesar de testadas, porque a comparação do OpenAPI ao vivo com o v4 (CT-CONTRACT-102) só chega na ETAPA 2C; a troca para `implemented` acontece junto com essa comparação. |
| D-NOITE-07 | A falha de gravação da auditoria de logout é registrada em log e não impede o logout; a sessão é invalidada de qualquer forma. |
| D-NOITE-08 | O `/error` chamado diretamente (sem erro encaminhado pelo container) responde 404 `NOT_FOUND`, e não 500, porque não há erro a relatar. |
| D-NOITE-04 | O job `bd` aplica V1–V6 e a massa demo subindo o jar com o perfil `demo` e `web-application-type=none`, e roda o pacote por `psql` dentro do contêiner do PostgreSQL (`docs/bd/pacote-sql/run_bd.sh`), exigindo exatamente 51 PASS. |
| D-NOITE-09 | No `PUT /customers/{id}`, `companyName`, `birthDate` e `address` ausentes mantêm o valor atual; `companyName` em pessoa física responde 400, e a versão diferente da atual responde 409 `CONCURRENT_UPDATE`. |
| D-NOITE-10 | A ETAPA 2C troca o `/customers` legado pelo `api/customers` do v4 (documento mascarado para o VENDEDOR) e encerra a restrição da D-NOITE-05; do controlador legado sobra só `POST /customers/{id}/address`, que continua com o GERENTE. |
| D-NOITE-11 | Inteiros do corpo não aceitam decimal (`accept-float-as-int=false`): quantidade `1.5` responde 400 em vez de virar 1 (CT-STOCK-101). |

## Processo

| ID | Decisão |
| --- | --- |
| P-01 | A ETAPA 1 foi executada sem a linha final `Execute a ETAPA N`, porque o plano de 14/10 indica a ETAPA 1 como a única que roda antes da V4 entrar na `main`. |
| P-02 | O DDL proposto foi ensaiado em PostgreSQL 18 descartável antes de entrar na documentação (resultado no modelo de dados, seção 7). |
