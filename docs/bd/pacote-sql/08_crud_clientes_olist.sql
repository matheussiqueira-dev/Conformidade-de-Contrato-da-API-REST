-- =====================================================================
-- Roteiro da apresentacao: CRUD e consultas sobre a massa Olist (clientes e historico de compras).
-- Rodar depois de 07_carga_olist.sql, pelo painel Database do IntelliJ, DBeaver ou psql:
--   psql -h 127.0.0.1 -p 5433 -U sbi -d sbi_demo -f 08_crud_clientes_olist.sql
-- Tudo em transacao terminada em ROLLBACK: a massa fica intacta. As violacoes de integridade sao
-- esperadas e mostradas com SAVEPOINT, para o roteiro seguir depois de cada uma.
-- =====================================================================
BEGIN;
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'matheussiqueira@sbi.tech'), true);

-- ---------------------------------------------------------------------
-- CREATE: cliente pessoa fisica e pessoa juridica (CNPJ alfanumerico)
-- ---------------------------------------------------------------------
INSERT INTO customer(customer_type, name, cpf, email, phone)
VALUES ('INDIVIDUAL', 'Cliente da Apresentacao', '52998224725', 'apresentacao.pf@example.com', '85988887777')
RETURNING id, customer_type, name, cpf, created_by;
INSERT INTO customer(customer_type, name, company_name, cnpj, email, phone)
VALUES ('CORPORATE', 'Empresa da Apresentacao', 'Empresa da Apresentacao Ltda', '12ABC34501DE35', 'apresentacao.pj@example.com', '8533334444')
RETURNING id, customer_type, company_name, cnpj;

-- A integridade recusa: CPF com mascara (o banco guarda so digitos) e tipo incoerente com o documento.
SAVEPOINT violacao_cpf;
INSERT INTO customer(customer_type, name, cpf, email) VALUES ('INDIVIDUAL', 'CPF com mascara', '529.982.247-25', 'mascara@example.com');
ROLLBACK TO SAVEPOINT violacao_cpf;
SAVEPOINT violacao_tipo;
INSERT INTO customer(customer_type, name, cnpj, email) VALUES ('INDIVIDUAL', 'PF com CNPJ', '11222333000181', 'tipo@example.com');
ROLLBACK TO SAVEPOINT violacao_tipo;

-- ---------------------------------------------------------------------
-- READ: historico completo de um cliente recorrente (14 pedidos)
-- ---------------------------------------------------------------------
SELECT o.id AS pedido, o.moment AS data, u.name AS vendedor, p.sku, p.name AS produto, oi.quantity AS qtd,
       oi.price AS preco, oi.price * oi.quantity - oi.discount AS subtotal, pay.payment_type AS forma, pay.status
  FROM customer c
  JOIN orders o       ON o.customer_id = c.id
  JOIN app_users u    ON u.id = o.seller_id
  JOIN order_item oi  ON oi.order_id = o.id
  JOIN product p      ON p.id = oi.product_id
  JOIN payment pay    ON pay.order_id = o.id
 WHERE c.email = 'larissa.cli-001@example.com'
 ORDER BY o.moment, p.sku;

-- Resumo do mesmo cliente pela view usada no perfil (v_cliente_resumo, uma linha por vendedor).
SELECT u.name AS vendedor, r.pedidos_pagos, r.liquido, r.primeira_compra, r.ultima_compra
  FROM v_cliente_resumo r JOIN app_users u ON u.id = r.seller_id
 WHERE r.customer_id = (SELECT id FROM customer WHERE email = 'larissa.cli-001@example.com')
 ORDER BY r.liquido DESC;

-- Ranking dos 10 melhores clientes (pagamentos confirmados menos estornos).
SELECT c.name AS cliente, c.customer_type AS tipo, sum(r.pedidos_pagos) AS pedidos, sum(r.liquido) AS total
  FROM v_cliente_resumo r JOIN customer c ON c.id = r.customer_id
 GROUP BY c.id, c.name, c.customer_type
 ORDER BY total DESC, c.name
 LIMIT 10;

-- Ticket medio por UF (endereco do cliente).
SELECT a.state AS uf, count(DISTINCT c.id) AS clientes, sum(r.pedidos_pagos) AS pedidos,
       round(sum(r.liquido) / nullif(sum(r.pedidos_pagos), 0), 2) AS ticket_medio
  FROM v_cliente_resumo r
  JOIN customer c ON c.id = r.customer_id
  JOIN address a  ON a.id = c.address_id
 GROUP BY a.state
 ORDER BY ticket_medio DESC NULLS LAST;

-- ---------------------------------------------------------------------
-- UPDATE: contato do cliente (com versao) e preco com motivo (historico pelo gatilho)
-- ---------------------------------------------------------------------
UPDATE customer SET phone = '85999990000', email = 'larissa.novo@example.com', version = version + 1, updated_at = now()
 WHERE email = 'larissa.cli-001@example.com'
RETURNING id, name, phone, email, version;

SET LOCAL app.reason = 'Reajuste de tabela na apresentacao';
UPDATE product SET price = price + 5 WHERE sku = (SELECT min(sku) FROM product WHERE sku LIKE 'OL-%')
RETURNING sku, name, price;
SELECT p.sku, h.old_price, h.new_price, u.name AS alterado_por, h.reason, h.changed_at
  FROM product_price_history h JOIN product p ON p.id = h.product_id JOIN app_users u ON u.id = h.changed_by
 WHERE p.sku = (SELECT min(sku) FROM product WHERE sku LIKE 'OL-%');

-- ---------------------------------------------------------------------
-- DELETE: cliente sem pedidos sai; cliente com pedidos e protegido pela FK
-- ---------------------------------------------------------------------
DELETE FROM customer WHERE email = 'apresentacao.pf@example.com' RETURNING id, name;
SAVEPOINT exclusao_com_pedidos;
DELETE FROM customer WHERE email = 'larissa.novo@example.com';
ROLLBACK TO SAVEPOINT exclusao_com_pedidos;

ROLLBACK;
