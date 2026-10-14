-- =====================================================================
-- Loja Gestao - views analiticas (dashboards de vendas e estoque)
-- Regras: realizado = pagamentos confirmados (APPROVED) menos estornos;
-- pedido pendente nao conta; datas em America/Fortaleza; meta zero = "Sem meta";
-- custo ausente nunca vira zero.
-- =====================================================================

-- Lancamentos que compoem o realizado (venda confirmada +, estorno -).
CREATE OR REPLACE VIEW v_lancamento_realizado AS
SELECT o.seller_id,
       p.payment_date::date AS data,
       o.id                  AS order_id,
       p.payment_type,
       (SELECT sum(oi.price * oi.quantity - oi.discount) FROM order_item oi WHERE oi.order_id = o.id) AS valor,
       'VENDA'::varchar      AS tipo
  FROM orders o JOIN payment p ON p.order_id = o.id
 WHERE p.status = 'APPROVED'
UNION ALL
SELECT o.seller_id,
       (r.created_at AT TIME ZONE 'America/Fortaleza')::date,
       o.id,
       p.payment_type,
       -r.amount,
       'ESTORNO'
  FROM refund r JOIN payment p ON p.id = r.payment_id JOIN orders o ON o.id = p.order_id;

-- Desempenho por vendedor e periodo (base de "Vendedores e metas" e "Meu desempenho").
CREATE OR REPLACE VIEW v_desempenho_vendedor AS
SELECT g.period                                   AS periodo,
       u.id                                       AS seller_id,
       u.name                                     AS vendedor,
       sg.amount                                  AS meta,
       coalesce(sum(l.valor), 0)                  AS realizado,
       CASE WHEN sg.amount > 0
            THEN round(coalesce(sum(l.valor), 0) * 100 / sg.amount, 1) END AS atingimento_pct,
       greatest(sg.amount - coalesce(sum(l.valor), 0), 0) AS falta,
       CASE WHEN sg.amount = 0 THEN 'Sem meta'
            WHEN coalesce(sum(l.valor), 0) >= sg.amount THEN 'Meta atingida'
            ELSE 'Em andamento' END               AS situacao
  FROM monthly_goal g
  JOIN seller_goal sg ON sg.monthly_goal_id = g.id
  JOIN app_users u    ON u.id = sg.seller_id
  LEFT JOIN v_lancamento_realizado l
         ON l.seller_id = u.id AND l.data >= g.period AND l.data < (g.period + interval '1 month')
 GROUP BY g.period, u.id, u.name, sg.amount;

-- Resumo da loja no periodo (cartoes do painel de Gestao).
CREATE OR REPLACE VIEW v_resumo_loja AS
SELECT g.period AS periodo,
       g.amount AS meta,
       coalesce(sum(l.valor), 0) AS faturamento_confirmado,
       CASE WHEN g.amount > 0 THEN round(coalesce(sum(l.valor), 0) * 100 / g.amount, 1) END AS atingimento_pct,
       greatest(g.amount - coalesce(sum(l.valor), 0), 0) AS falta,
       count(DISTINCT l.order_id) FILTER (WHERE l.tipo = 'VENDA') AS pedidos_pagos,
       CASE WHEN count(DISTINCT l.order_id) FILTER (WHERE l.tipo = 'VENDA') > 0
            THEN round(sum(l.valor) FILTER (WHERE l.tipo = 'VENDA')
                       / count(DISTINCT l.order_id) FILTER (WHERE l.tipo = 'VENDA'), 2) END AS ticket_medio
  FROM monthly_goal g
  LEFT JOIN v_lancamento_realizado l ON l.data >= g.period AND l.data < (g.period + interval '1 month')
 GROUP BY g.period, g.amount;

-- Evolucao por semana (segunda a domingo), recortada aos limites do mes.
CREATE OR REPLACE VIEW v_evolucao_semanal AS
SELECT date_trunc('month', l.data)::date AS periodo,
       greatest(date_trunc('week', l.data)::date, date_trunc('month', l.data)::date) AS inicio,
       least((date_trunc('week', l.data) + interval '6 days')::date,
             (date_trunc('month', l.data) + interval '1 month - 1 day')::date) AS fim,
       l.seller_id,
       sum(l.valor) AS realizado
  FROM v_lancamento_realizado l
 GROUP BY 1, 2, 3, 4;

CREATE OR REPLACE VIEW v_evolucao_semanal_loja AS
SELECT periodo, inicio, fim, sum(realizado) AS realizado
  FROM v_evolucao_semanal GROUP BY periodo, inicio, fim;

-- Posicao de estoque completa (somente gerente: inclui custo).
CREATE OR REPLACE VIEW v_posicao_estoque AS
SELECT p.id AS product_id, p.sku, p.name AS produto, c.name AS categoria,
       b.on_hand AS saldo, b.reserved AS reservado, b.on_hand - b.reserved AS disponivel,
       b.min_level AS estoque_minimo,
       (b.on_hand - b.reserved) <= b.min_level AS abaixo_do_minimo,
       b.avg_cost AS custo_medio,
       CASE WHEN b.avg_cost IS NULL THEN 'Nao informado' ELSE 'Informado' END AS situacao_custo,
       p.price AS preco_venda,
       b.on_hand * b.avg_cost AS valor_em_estoque_custo,
       b.on_hand * p.price    AS valor_em_estoque_venda,
       p.active AS ativo
  FROM product p
  JOIN stock_balance b ON b.product_id = p.id
  LEFT JOIN category c ON c.id = p.category_id;

CREATE OR REPLACE VIEW v_alerta_estoque AS
SELECT product_id, sku, produto, categoria, disponivel, estoque_minimo
  FROM v_posicao_estoque WHERE ativo AND abaixo_do_minimo;

-- Catalogo para venda (vendedor): sem custo, margem ou saldo fisico.
CREATE OR REPLACE VIEW v_catalogo_vendedor AS
SELECT p.id AS product_id, p.sku, p.name AS produto, c.name AS categoria,
       p.price AS preco, b.on_hand - b.reserved AS disponivel
  FROM product p
  JOIN stock_balance b ON b.product_id = p.id
  LEFT JOIN category c ON c.id = p.category_id
 WHERE p.active;

-- Margem bruta por produto e mes (gerente). Custo ausente -> margem NULL.
CREATE OR REPLACE VIEW v_margem_produto AS
SELECT date_trunc('month', p.payment_date)::date AS periodo,
       pr.id AS product_id, pr.name AS produto,
       sum(oi.quantity) AS unidades,
       sum(oi.price * oi.quantity - oi.discount) AS receita_liquida,
       CASE WHEN count(*) FILTER (WHERE oi.unit_cost IS NULL) = 0
            THEN sum(oi.unit_cost * oi.quantity) END AS custo,
       CASE WHEN count(*) FILTER (WHERE oi.unit_cost IS NULL) = 0
            THEN sum(oi.price * oi.quantity - oi.discount) - sum(oi.unit_cost * oi.quantity) END AS margem_bruta,
       CASE WHEN count(*) FILTER (WHERE oi.unit_cost IS NULL) = 0
             AND sum(oi.price * oi.quantity - oi.discount) > 0
            THEN round((sum(oi.price * oi.quantity - oi.discount) - sum(oi.unit_cost * oi.quantity)) * 100
                       / sum(oi.price * oi.quantity - oi.discount), 1) END AS margem_pct,
       count(*) FILTER (WHERE oi.unit_cost IS NULL) AS itens_sem_custo
  FROM order_item oi
  JOIN orders o   ON o.id = oi.order_id
  JOIN payment p  ON p.order_id = o.id AND p.status = 'APPROVED'
  JOIN product pr ON pr.id = oi.product_id
 GROUP BY 1, 2, 3;

CREATE OR REPLACE VIEW v_vendas_forma_pagamento AS
SELECT date_trunc('month', data)::date AS periodo, payment_type AS forma,
       count(DISTINCT order_id) FILTER (WHERE tipo = 'VENDA') AS pedidos,
       sum(valor) AS valor
  FROM v_lancamento_realizado GROUP BY 1, 2;

CREATE OR REPLACE VIEW v_promocoes_ativas AS
SELECT id AS promotion_id, name AS promocao, kind AS tipo, value AS valor,
       product_id, category_id, starts_at AS inicio, ends_at AS fim
  FROM promotion
 WHERE active AND now() >= starts_at AND now() < ends_at;

CREATE OR REPLACE VIEW v_uso_promocoes AS
SELECT pr.id AS promotion_id, pr.name AS promocao,
       count(oi.id) AS itens, coalesce(sum(oi.discount), 0) AS desconto_total,
       coalesce(sum(oi.price * oi.quantity - oi.discount), 0) AS receita_liquida
  FROM promotion pr LEFT JOIN order_item oi ON oi.promotion_id = pr.id
 GROUP BY pr.id, pr.name;

-- Visoes do vendedor autenticado (filtradas pela identidade da sessao).
CREATE OR REPLACE VIEW v_meu_desempenho WITH (security_barrier = true) AS
SELECT periodo, vendedor, meta, realizado, atingimento_pct, falta, situacao
  FROM v_desempenho_vendedor WHERE seller_id = app_current_user_id();

CREATE OR REPLACE VIEW v_minha_evolucao_semanal WITH (security_barrier = true) AS
SELECT periodo, inicio, fim, realizado
  FROM v_evolucao_semanal WHERE seller_id = app_current_user_id();

-- Executa com os privilegios de quem consulta: sujeita a RLS e a GRANT por coluna.
CREATE OR REPLACE VIEW v_minhas_vendas WITH (security_invoker = true) AS
SELECT o.id AS pedido, o.moment AS data, o.status, o.customer_id,
       p.status AS status_pagamento, p.amount AS valor
  FROM orders o LEFT JOIN payment p ON p.order_id = o.id;
