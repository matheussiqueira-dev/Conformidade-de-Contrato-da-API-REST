-- =====================================================================
-- Loja Gestao - roteiro de CRUD e consultas (executar como gerente_matheus)
--   psql -U gerente_matheus -d <banco> -f 05_crud_exemplos.sql
-- Tudo ocorre em transacao e termina em ROLLBACK para preservar a massa.
-- =====================================================================
BEGIN;

-- CREATE
INSERT INTO category(name) VALUES ('Tablets') RETURNING id, name;
INSERT INTO product(product_type, name, price, weight, sku, category_id)
VALUES ('PHYSICAL', 'iPad 10a geracao', 3600.00, 0.48, 'IPD-10', (SELECT id FROM category WHERE name = 'Tablets'))
RETURNING id, sku, name, price;
INSERT INTO stock_entry(product_id, quantity, unit_cost, note)
VALUES ((SELECT id FROM product WHERE sku = 'IPD-10'), 8, 2700.00, 'Entrada de demonstracao');
INSERT INTO promotion(name, kind, value, product_id, starts_at, ends_at)
VALUES ('iPad R$ 200 off', 'AMOUNT', 200, (SELECT id FROM product WHERE sku = 'IPD-10'), now(), now() + interval '7 days');

-- READ
SELECT sku, produto, categoria, saldo, reservado, disponivel, custo_medio, preco_venda
  FROM v_posicao_estoque ORDER BY categoria, produto;
SELECT c.name AS categoria, count(p.id) AS produtos, round(avg(p.price), 2) AS preco_medio
  FROM category c LEFT JOIN product p ON p.category_id = c.id
 GROUP BY c.name ORDER BY produtos DESC;

-- UPDATE (preco com motivo registrado no historico)
SET LOCAL app.reason = 'Reajuste de tabela do fornecedor';
UPDATE product SET price = 3800.00 WHERE sku = 'IPD-10';
SELECT h.changed_at, p.sku, h.old_price, h.new_price, u.name AS alterado_por, h.reason
  FROM product_price_history h JOIN product p ON p.id = h.product_id
  JOIN app_users u ON u.id = h.changed_by
 WHERE p.sku = 'IPD-10';
UPDATE stock_balance SET min_level = 3 WHERE product_id = (SELECT id FROM product WHERE sku = 'IPD-10');

-- DELETE (inativacao preferida para produto com historico; exclusao fisica so sem vinculos)
UPDATE product SET active = false WHERE sku = 'IPD-10';
DELETE FROM promotion WHERE name = 'iPad R$ 200 off';
DELETE FROM category WHERE name = 'Tablets' AND NOT EXISTS
       (SELECT 1 FROM product WHERE category_id = category.id);  -- 0 linhas: categoria em uso

-- Consultas analiticas dos dashboards
SELECT * FROM v_resumo_loja WHERE periodo = '2026-09-01';
SELECT vendedor, meta, realizado, atingimento_pct, falta FROM v_desempenho_vendedor
 WHERE periodo = '2026-09-01' ORDER BY realizado DESC;
SELECT inicio, fim, realizado FROM v_evolucao_semanal_loja WHERE periodo = '2026-09-01' ORDER BY inicio;
SELECT produto, unidades, receita_liquida, margem_pct, itens_sem_custo FROM v_margem_produto
 WHERE periodo = '2026-09-01' ORDER BY receita_liquida DESC;
SELECT * FROM v_alerta_estoque;

-- Plano de execucao (comparar antes/depois de indices em massa maior)
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT o.id, sum(oi.price * oi.quantity) FROM orders o JOIN order_item oi ON oi.order_id = o.id
 WHERE o.seller_id = (SELECT id FROM app_users WHERE email = 'allanbarros@sbi.tech') GROUP BY o.id;

ROLLBACK;
