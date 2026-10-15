# Massa de demonstração: histórico de clientes (Olist)

Objetivo: dar à demonstração um histórico de compras realista e volumoso para o perfil do cliente, sem mexer no seed que os testes usam (V6_1/V6_2 continuam com R$ 186.400,00 em 65 pedidos).

## Fonte e licença

- Brazilian E-Commerce Public Dataset by Olist: pedidos reais anonimizados de 2016 a 2018. <https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce>
- Licença CC BY-NC-SA 4.0: uso acadêmico, não comercial, com atribuição; o recorte derivado segue a mesma licença. Detalhes em `docs/bd/pacote-sql/olist/NOTICE.md`.
- Os CSVs originais (cerca de 50 MB) não são versionados. `docs/bd/pacote-sql/olist/montar_recorte.py` gera o recorte de forma determinística a partir deles.

## O recorte

| Arquivo (`docs/bd/pacote-sql/olist/`) | Conteúdo |
| --- | --- |
| `clientes.csv` | 60 clientes: 40 recorrentes (o maior com 14 pedidos) e 20 com um pedido; 8 pessoas jurídicas |
| `produtos.csv` | 43 produtos Apple em 7 categorias (Smartphones, Tablets, Computadores, Audio, Wearables, Casa inteligente, Acessorios) |
| `pedidos.csv` | 219 pedidos pagos (CARD 167, BOLETO 48, PIX 4; 148 entregas e 71 retiradas) |
| `itens_pedido.csv` | 237 itens; faturamento bruto R$ 442.714,00 |

## Adaptações

- **Pessoas:** a Olist não publica nome nem documento. Os nomes são sintéticos; CPF e CNPJ são gerados a partir de hash, com dígito verificador válido; os e-mails usam o domínio reservado `example.com`. Endereço: cidade, UF e prefixo de CEP do original, com rua e número de demonstração. Nenhum dado pessoal real.
- **Produtos:** a loja vende só Apple. O recorte Olist misturava 37 categorias (cama e banho, pet shop, ferramentas...); `montar_recorte.py` troca cada produto por um item de um catálogo Apple fictício de 43 SKUs (`OL-<modelo>`): o quantil do preço Olist escolhe a faixa (de película e cabo a iPhone Pro e MacBook Pro) e o hash do produto escolhe o item. O preço é o do catálogo, fictício e na ordem de grandeza do varejo brasileiro, então o pedido mantém cliente, data, quantidade, vendedor e pagamento da Olist, mas não o valor. Smartphones, Audio, Wearables e Acessorios são as categorias do seed (a promoção de Acessorios vale também para os acessórios da massa); Tablets, Computadores e Casa inteligente são novas. Categoria fora dessa lista faz a carga falhar. Custo médio sintético de 30% a 45% do preço nos acessórios e de 62% a 73% nos aparelhos, só para margem e lucro aparecerem. Estoque inicial = quantidade vendida + saldo restante de 5 a 20 (determinístico pelo SKU), com entrada datada antes do primeiro pedido.
- **Datas:** comprimidas para os últimos 365 dias; `dias_atras` é relativo à data da carga, então o histórico não envelhece. Nenhum pedido fica no futuro: o horário de hoje é limitado ao instante da carga.
- **Pagamento:** o de maior valor de cada pedido; cartão de crédito ou débito vira CARD (só os 4 últimos dígitos, sintéticos), boleto vira BOLETO (vencimento em 3 dias) e voucher vira PIX. Só pedidos entregues, todos pagos.
- **Vendedor:** `VENDEDOR_1..3` viram Ana, Bruno e Carla, as contas de demonstração.

## Como a carga funciona (SQL puro)

A carga é ela mesma evidência de SQL para a disciplina de Banco de Dados: `\copy`, `INSERT ... SELECT`, laço PL/pgSQL e uma única transação, executada por `psql` no padrão do pacote `docs/bd/pacote-sql`.

`07_carga_olist.sql`, executado como superusuário do banco demo depois do Flyway, faz em uma transação:

1. autoria pelo mecanismo do sistema (`set_config('app.user_id', ...)` do gerente, lido por `app_current_user_id()`);
2. schema `stg_olist` com uma tabela por CSV e `\copy` dos arquivos;
3. limpeza idempotente da carga anterior (pedidos dos clientes da massa e pedidos com produto `OL-%`, pagamentos, itens, movimentos, endereços, entradas, produtos, clientes e categorias que ficaram vazias, inclusive as 36 do catálogo diverso de cargas anteriores), na ordem das FKs;
4. categorias, produtos (o gatilho cria `stock_balance`) e entradas de estoque (o gatilho calcula o custo médio);
5. clientes com endereço próprio e pedidos pagos com itens (custo atribuído do momento), pagamento aprovado, movimento `SAIDA` e baixa de `on_hand`, em ordem crescente de `product_id`;
6. conferência (60 clientes, 43 produtos, 7 categorias, 219 pedidos, 237 itens, R$ 442.714,00): se algo não bater, a transação é desfeita.

`08_crud_clientes_olist.sql` é o roteiro da apresentação, todo em `BEGIN ... ROLLBACK`: cadastro de PF e de PJ com CNPJ alfanumérico; duas recusas do banco (CPF com máscara, tipo incoerente com o documento); histórico completo do cliente recorrente; resumo por `v_cliente_resumo`; ranking dos 10 melhores clientes; ticket médio por UF; atualização de contato com versão; preço com motivo e histórico; exclusão de cliente sem pedidos e recusa da FK para cliente com pedidos.

## Executar

Na raiz do repositório, com Docker:

```powershell
$env:SBI_DEMO_PASSWORD = '<senha das contas demo>'
$env:SBI_DB_PASSWORD = [guid]::NewGuid().ToString('N')
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\demo-olist.ps1 -Check
```

Em bash: `bash docs/bd/pacote-sql/run_demo_olist.sh --check`. Os dois sobem a stack demo, esperam a API saudável (Flyway aplicado), carregam a massa e imprimem as contagens; com a opção de verificação, carregam de novo (idempotência) e rodam os testes CT-DEMO.

O banco fica acessível só nesta máquina, para o painel Database do IntelliJ, o DBeaver ou o `psql`:

| Campo | Valor |
| --- | --- |
| URL | `jdbc:postgresql://127.0.0.1:5433/sbi_demo` |
| Usuário | `sbi` |
| Senha | a de `SBI_DB_PASSWORD` (nunca gravada em arquivo) |

O banco está em `tmpfs`: cada `up` começa limpo e reproduzível. Para recarregar a massa, basta rodar o script de novo.

## Testes

| Caso | Onde | O que verifica |
| --- | --- | --- |
| CT-DEMO-01 | `09_testes_olist.sql` | contagens (inclusive 8 PJ e 7 categorias) e soma de R$ 442.714,00 nos itens e nos pagamentos |
| CT-DEMO-02 | `09_testes_olist.sql` | cliente com 14 pedidos: itens e `v_cliente_resumo` iguais ao CSV |
| CT-DEMO-03 | `09_testes_olist.sql` | carga executada duas vezes sem duplicar SKU, cliente, pedido ou entrada |
| CT-DEMO-04 | `09_testes_olist.sql` | saldo = entrada − vendido, nunca negativo, reservado 0 |
| CT-DEMO-05 | `scripts/demo-olist-api.mjs` | pela API, o VENDEDOR vê CPF e CNPJ da massa mascarados em lista, perfil, histórico e resumo; o GERENTE vê completo e o resumo com 14 pedidos |
| CT-DEMO-06 | `run_olist_bd.sh` | o roteiro 08 roda com exatamente as três recusas esperadas |
| CT-DEMO-07 | `09_testes_olist.sql` | catálogo só Apple: produtos da massa nas 7 categorias Apple, com nome e preço do CSV, e nenhuma categoria do catálogo diverso de cargas anteriores |

Na CI, o job `bd` roda essa etapa (`docs/bd/pacote-sql/run_olist_bd.sh`) depois dos testes 06*, então nenhum número do BD-01 ao BD-32 muda. Os perfis padrão, `integration` e `contract` não recebem a massa.
