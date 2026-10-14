-- =====================================================================
-- Testes da carga Olist (CT-DEMO-01 a CT-DEMO-04). Cada caso imprime PASS ou FAIL.
-- Rodar como superusuario depois de 07_carga_olist.sql executado DUAS vezes (CT-DEMO-03).
-- =====================================================================
\set QUIET on
CREATE OR REPLACE FUNCTION pg_temp.ok(p_id text, p_cond boolean, p_info text DEFAULT '') RETURNS void
LANGUAGE plpgsql AS $$ BEGIN
  IF coalesce(p_cond, false) THEN RAISE NOTICE 'PASS %', p_id; ELSE RAISE NOTICE 'FAIL % %', p_id, p_info; END IF;
END $$;

-- CT-DEMO-01 contagens e soma
SELECT pg_temp.ok('CT-DEMO-01a 60 clientes (8 PJ), 220 produtos em 37 categorias, 219 pedidos e 238 itens',
       (SELECT count(*) FROM customer WHERE email IN (SELECT email FROM stg_olist.clientes)) = 60
       AND (SELECT count(*) FROM customer WHERE email IN (SELECT email FROM stg_olist.clientes) AND customer_type = 'CORPORATE') = 8
       AND (SELECT count(*) FROM product WHERE sku LIKE 'OL-%') = 220
       AND (SELECT count(DISTINCT category_id) FROM product WHERE sku LIKE 'OL-%') = 37
       AND (SELECT count(*) FROM orders o JOIN customer c ON c.id = o.customer_id
             WHERE c.email IN (SELECT email FROM stg_olist.clientes) AND o.status = 'PAID') = 219
       AND (SELECT count(*) FROM order_item oi JOIN product p ON p.id = oi.product_id WHERE p.sku LIKE 'OL-%') = 238);
SELECT pg_temp.ok('CT-DEMO-01b faturamento bruto 26.040,01 e pagamentos aprovados com o mesmo total',
       (SELECT sum(oi.price * oi.quantity) FROM order_item oi JOIN product p ON p.id = oi.product_id WHERE p.sku LIKE 'OL-%') = 26040.01
       AND (SELECT sum(pay.amount) FROM payment pay JOIN orders o ON o.id = pay.order_id JOIN customer c ON c.id = o.customer_id
             WHERE c.email IN (SELECT email FROM stg_olist.clientes) AND pay.status = 'APPROVED') = 26040.01);

-- CT-DEMO-02 cliente recorrente com 14 pedidos: historico completo e resumo coerente com o CSV
SELECT pg_temp.ok('CT-DEMO-02 CLI-001 com 14 pedidos pagos, itens e resumo iguais ao CSV',
       (SELECT count(*) FROM orders WHERE customer_id = c.id) = 14
       AND (SELECT count(*) FROM order_item oi JOIN orders o ON o.id = oi.order_id WHERE o.customer_id = c.id)
         = (SELECT count(*) FROM stg_olist.itens_pedido i JOIN stg_olist.pedidos p ON p.codigo = i.pedido WHERE p.cliente = 'CLI-001')
       AND (SELECT sum(pedidos_pagos) FROM v_cliente_resumo WHERE customer_id = c.id) = 14
       AND (SELECT sum(liquido) FROM v_cliente_resumo WHERE customer_id = c.id)
         = (SELECT sum(i.quantidade * i.preco_unitario) FROM stg_olist.itens_pedido i JOIN stg_olist.pedidos p ON p.codigo = i.pedido
             WHERE p.cliente = 'CLI-001'),
       c.id::text)
  FROM customer c WHERE c.email = 'larissa.cli-001@example.com';

-- CT-DEMO-03 rodar a carga duas vezes nao duplica
SELECT pg_temp.ok('CT-DEMO-03 carga repetida sem duplicar SKU, cliente, pedido ou entrada de estoque',
       (SELECT count(*) = count(DISTINCT sku) FROM product WHERE sku LIKE 'OL-%')
       AND (SELECT count(*) FROM customer WHERE email IN (SELECT email FROM stg_olist.clientes))
         = (SELECT count(DISTINCT email) FROM stg_olist.clientes)
       AND (SELECT count(*) FROM stock_entry e JOIN product p ON p.id = e.product_id WHERE p.sku LIKE 'OL-%') = 220);

-- CT-DEMO-04 estoque nunca negativo e sem reserva depois da carga
SELECT pg_temp.ok('CT-DEMO-04 saldo = entrada - vendido, nunca negativo, reservado 0',
       bool_and(b.on_hand >= 0 AND b.reserved = 0
                AND b.on_hand = (SELECT sum(quantity) FROM stock_entry e WHERE e.product_id = b.product_id)
                              - coalesce((SELECT sum(quantity) FROM stock_movement m WHERE m.product_id = b.product_id AND m.kind = 'SAIDA'), 0)))
  FROM stock_balance b JOIN product p ON p.id = b.product_id WHERE p.sku LIKE 'OL-%';
