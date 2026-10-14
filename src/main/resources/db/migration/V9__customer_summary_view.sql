-- G-05 (docs/decisoes/evolucao.md): indicadores do cliente para a tela de perfil.
-- Mesma regra do faturamento (v_lancamento_realizado): venda = pagamento APPROVED, valor = itens menos desconto;
-- estorno pela tabela refund. Grao cliente x vendedor, para o servico somar a loja toda (GERENTE) ou so o
-- vendedor da sessao (VENDEDOR). Pedidos pendentes e cancelados ficam fora dos totais.
CREATE OR REPLACE VIEW v_cliente_resumo AS
WITH venda AS (
    SELECT o.customer_id,
           o.seller_id,
           count(*)            AS pedidos_pagos,
           sum((SELECT sum(oi.price * oi.quantity - oi.discount) FROM order_item oi WHERE oi.order_id = o.id)) AS bruto,
           min(p.payment_date) AS primeira_compra,
           max(p.payment_date) AS ultima_compra
      FROM orders o
      JOIN payment p ON p.order_id = o.id
     WHERE p.status = 'APPROVED' AND o.customer_id IS NOT NULL
     GROUP BY o.customer_id, o.seller_id
), estorno AS (
    SELECT o.customer_id, o.seller_id, sum(r.amount) AS estornos
      FROM refund r
      JOIN payment p ON p.id = r.payment_id
      JOIN orders o  ON o.id = p.order_id
     WHERE o.customer_id IS NOT NULL
     GROUP BY o.customer_id, o.seller_id
)
SELECT coalesce(v.customer_id, e.customer_id)               AS customer_id,
       coalesce(v.seller_id, e.seller_id)                   AS seller_id,
       coalesce(v.pedidos_pagos, 0)                         AS pedidos_pagos,
       coalesce(v.bruto, 0)                                 AS bruto,
       coalesce(e.estornos, 0)                              AS estornos,
       coalesce(v.bruto, 0) - coalesce(e.estornos, 0)       AS liquido,
       v.primeira_compra,
       v.ultima_compra
  FROM venda v
  FULL JOIN estorno e ON e.customer_id = v.customer_id AND e.seller_id IS NOT DISTINCT FROM v.seller_id;
