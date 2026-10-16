# Massa de demonstração: histórico de clientes (Olist)

## Fonte e licença

- **Fonte:** Brazilian E-Commerce Public Dataset by Olist, pedidos reais anonimizados de 2016 a 2018.
  <https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce>
- **Licença:** Creative Commons Atribuição-NãoComercial-CompartilhaIgual 4.0 (CC BY-NC-SA 4.0).
  <https://creativecommons.org/licenses/by-nc-sa/4.0/>
- Os arquivos desta pasta são um derivado do conjunto original e seguem a mesma licença: uso acadêmico, não comercial, com atribuição à Olist.
- Os CSVs originais (cerca de 50 MB) não são versionados. `montar_recorte.py` gera este recorte de forma determinística a partir deles (`python montar_recorte.py`, lendo `raw/` e gravando `demo/`).

## Conteúdo

| Arquivo | Linhas | Conteúdo |
| --- | ---: | --- |
| `clientes.csv` | 60 | 40 clientes recorrentes (até 14 pedidos) e 20 com um pedido; 8 pessoas jurídicas |
| `produtos.csv` | 42 | catálogo Apple em 7 categorias, a preço de mercado de 15/10/2026, com custo médio sintético |
| `pedidos.csv` | 219 | pedidos pagos, com forma de pagamento, entrega e vendedor de demonstração |
| `itens_pedido.csv` | 237 | itens dos pedidos; faturamento bruto do recorte: R$ 449.690,00 |

## Adaptações em relação ao original

- A Olist não publica nome nem documento de cliente. Os nomes são sintéticos; CPF e CNPJ são gerados a partir de hash, com dígito verificador válido; os e-mails usam o domínio reservado `example.com`. Nenhum dado pessoal real.
- Os produtos não têm nome no original, e o recorte misturava 37 categorias (cama e banho, pet shop, ferramentas...). A loja da demonstração vende só Apple, então cada produto Olist vira um item de um catálogo Apple de 42 SKUs (`OL-<modelo>`, em `montar_recorte.py`): o quantil do preço Olist escolhe a faixa (de acessórios a Mac) e o hash do produto escolhe o item dentro dela. O item do pedido usa o preço do catálogo: o de mercado no varejo brasileiro em 15/10/2026 ou, sem oferta encontrada, o da Apple Store Brasil menos 10%; quantidade, cliente, data, vendedor e pagamento continuam os do pedido Olist. Dois produtos Olist que caem no mesmo SKU no mesmo pedido viram um item com a quantidade somada (238 itens Olist, 237 no recorte). `custo_medio_demo` é sintético (30% a 45% do preço nos acessórios, 62% a 73% nos aparelhos), só para margem e lucro aparecerem.
- As datas foram comprimidas para os últimos 365 dias: `dias_atras` é relativo à data da carga, então o histórico não envelhece.
- Pagamento: o de maior valor de cada pedido; `credit_card`/`debit_card` viram CARD (4 últimos dígitos sintéticos), `boleto` vira BOLETO e `voucher` vira PIX. Só pedidos `delivered`, todos como pagos.
- Vendedor: distribuído entre `VENDEDOR_1..3`, mapeados na carga para as contas de demonstração Ana, Bruno e Carla.

A carga no banco está em `../07_carga_olist.sql`; o roteiro de CRUD da apresentação em `../08_crud_clientes_olist.sql`. Documentação: `docs/bd/massa-demonstracao.md`.
