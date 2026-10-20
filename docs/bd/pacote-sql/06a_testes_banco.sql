-- =====================================================================
-- Testes de banco (dono do esquema). Cada caso imprime PASS ou FAIL.
-- Casos que alteram dados rodam em transacao com ROLLBACK.
-- =====================================================================
\set QUIET on
CREATE OR REPLACE FUNCTION pg_temp.ok(p_id text, p_cond boolean, p_info text DEFAULT '') RETURNS void
LANGUAGE plpgsql AS $$ BEGIN
  IF coalesce(p_cond, false) THEN RAISE NOTICE 'PASS %', p_id; ELSE RAISE NOTICE 'FAIL % %', p_id, p_info; END IF;
END $$;

-- BD-01 Meta e realizado do Allan em setembro (CA-10-01)
SELECT pg_temp.ok('BD-01 Allan 68.400 / 120.000 = 57,0%',
       realizado = 68400 AND meta = 120000 AND atingimento_pct = 57.0 AND falta = 51600, row(realizado, atingimento_pct)::text)
  FROM v_desempenho_vendedor WHERE periodo = '2026-09-01' AND vendedor = 'Allan Barros';
-- BD-02 Francisco; Gabriel e gerente e nao tem meta individual
SELECT pg_temp.ok('BD-02 Francisco 51,7% e so dois vendedores com meta',
       count(*) = 2 AND bool_and((vendedor = 'Francisco Ferreira' AND atingimento_pct = 51.7) OR vendedor = 'Allan Barros'))
  FROM v_desempenho_vendedor WHERE periodo = '2026-09-01';
-- BD-03 Resumo da loja (CA-12-01)
SELECT pg_temp.ok('BD-03 Loja 186.400 / 240.000 = 77,7%, falta 53.600',
       faturamento_confirmado = 186400 AND atingimento_pct = 77.7 AND falta = 53600, row(faturamento_confirmado, atingimento_pct)::text)
  FROM v_resumo_loja WHERE periodo = '2026-09-01';
-- BD-04 Evolucao semanal igual ao Guia Visual
SELECT pg_temp.ok('BD-04 Evolucao semanal 38.200/41.600/36.800/42.300/27.500',
       array_agg(realizado ORDER BY inicio) = ARRAY[38200,41600,36800,42300,27500]::numeric[]
       AND array_agg(inicio ORDER BY inicio) = ARRAY['2026-09-01','2026-09-07','2026-09-14','2026-09-21','2026-09-28']::date[],
       array_agg(realizado ORDER BY inicio)::text)
  FROM v_evolucao_semanal_loja WHERE periodo = '2026-09-01';
-- BD-05 Pedido pendente nao entra no realizado (CA-10-03)
SELECT pg_temp.ok('BD-05 Pendentes de 01/10 fora do realizado',
       (SELECT count(*) FROM orders WHERE status = 'PENDING_PAYMENT') = 2
       AND NOT EXISTS (SELECT 1 FROM v_lancamento_realizado WHERE data >= '2026-10-01'));
-- BD-06 Estoque do Guia: iPhone 10/2/8 e AirPods 13/1/12
SELECT pg_temp.ok('BD-06 Saldo, reservado e disponivel',
       bool_and((sku = 'IP-128-PT' AND saldo = 10 AND reservado = 2 AND disponivel = 8)
             OR (sku = 'AP-BR'     AND saldo = 13 AND reservado = 1 AND disponivel = 12)))
  FROM v_posicao_estoque WHERE sku IN ('IP-128-PT','AP-BR');
-- BD-07 Custo nao informado nao vira margem. O seed demo informa o custo de todos os produtos (V11_2), entao o
-- caso usa um produto proprio, sem custo, vendido num pedido pago de setembro; tudo volta no ROLLBACK.
BEGIN;
INSERT INTO product(product_type, name, price, weight, sku) VALUES ('PHYSICAL', 'Teste sem custo', 100, 1, 'TST-SEM-CUSTO');
INSERT INTO order_item(quantity, price, order_id, product_id, unit_cost)
SELECT 1, 100, (SELECT min(o.id) FROM orders o JOIN payment p ON p.order_id = o.id AND p.status = 'APPROVED'
                 WHERE p.payment_date >= '2026-09-01' AND p.payment_date < '2026-10-01'),
       id, NULL FROM product WHERE sku = 'TST-SEM-CUSTO';
SELECT pg_temp.ok('BD-07 Margem NULL quando ha item sem custo',
       margem_pct IS NULL AND custo IS NULL AND itens_sem_custo = 1, row(margem_pct, itens_sem_custo)::text)
  FROM v_margem_produto WHERE periodo = '2026-09-01' AND produto = 'Teste sem custo';
ROLLBACK;
-- BD-07b Com o custo da carga inicial (V11_2), iPhone e AirPods de setembro tem margem calculada
SELECT pg_temp.ok('BD-07b iPhone e AirPods de setembro com custo e margem',
       count(*) = 2 AND bool_and(margem_pct IS NOT NULL AND itens_sem_custo = 0), count(*)::text)
  FROM v_margem_produto WHERE periodo = '2026-09-01' AND produto IN ('iPhone 128 GB Preto', 'AirPods Branco');
SELECT pg_temp.ok('BD-08 Margem calculada quando custo conhecido (Apple Watch 32,1%)',
       margem_pct = 32.1, margem_pct::text)
  FROM v_margem_produto WHERE periodo = '2026-09-01' AND produto = 'Apple Watch 41 mm';
-- BD-09 Alerta de estoque minimo
SELECT pg_temp.ok('BD-09 Apple Watch abaixo do minimo e iPhone fora do alerta',
       EXISTS (SELECT 1 FROM v_alerta_estoque WHERE sku = 'AW-41')
       AND NOT EXISTS (SELECT 1 FROM v_alerta_estoque WHERE sku = 'IP-128-PT'));

-- Valores-limite e integridade (cada bloco reverte a propria alteracao)
DO $$ BEGIN
  INSERT INTO order_item(quantity, price, order_id, product_id) SELECT 0, 1, min(id), (SELECT min(id) FROM product) FROM orders;
  RAISE NOTICE 'FAIL BD-10 quantidade 0 aceita';
EXCEPTION WHEN check_violation THEN RAISE NOTICE 'PASS BD-10 quantidade 0 rejeitada';
END $$;
DO $$ DECLARE v bigint; BEGIN
  INSERT INTO order_item(quantity, price, order_id, product_id) SELECT 1, 1, min(id), (SELECT min(id) FROM product) FROM orders RETURNING id INTO v;
  RAISE NOTICE 'PASS BD-11 quantidade 1 aceita';
  RAISE EXCEPTION USING ERRCODE = 'LJ000';
EXCEPTION WHEN SQLSTATE 'LJ000' THEN NULL;
END $$;
DO $$ BEGIN
  UPDATE product SET price = 0.00 WHERE sku = 'PL-VID';
  RAISE NOTICE 'FAIL BD-12 preco 0,00 aceito';
EXCEPTION WHEN check_violation THEN RAISE NOTICE 'PASS BD-12 preco 0,00 rejeitado (minimo 0,01)';
END $$;
DO $$ BEGIN
  INSERT INTO promotion(name, kind, value, starts_at, ends_at, created_by)
  SELECT 'x', 'PERCENT', 100.01, now(), now() + interval '1 day', id FROM app_users WHERE db_role = 'gerente_matheus';
  RAISE NOTICE 'FAIL BD-13 desconto de 100,01%% aceito';
EXCEPTION WHEN check_violation THEN RAISE NOTICE 'PASS BD-13 desconto 100,01%% rejeitado';
END $$;
DO $$ BEGIN
  INSERT INTO promotion(name, kind, value, starts_at, ends_at, created_by)
  SELECT 'x', 'PERCENT', 10, now(), now() + interval '1 day', id FROM app_users WHERE email = 'allanbarros@sbi.tech';
  RAISE NOTICE 'FAIL BD-14 promocao criada por vendedor';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS BD-14 promocao exige gerente';
END $$;
DO $$ BEGIN
  UPDATE stock_balance SET reserved = on_hand + 1 WHERE product_id = (SELECT id FROM product WHERE sku = 'AW-41');
  RAISE NOTICE 'FAIL BD-15 reservado acima do saldo';
EXCEPTION WHEN check_violation THEN RAISE NOTICE 'PASS BD-15 reservado <= saldo garantido pelo banco';
END $$;
-- BD-16 Soma das metas diferente da meta da loja (CA-09-02)
DO $$ BEGIN
  INSERT INTO monthly_goal(period, amount, created_by) SELECT '2026-11-01', 240000, id FROM app_users WHERE db_role = 'gerente_matheus';
  INSERT INTO seller_goal(monthly_goal_id, seller_id, amount)
  SELECT g.id, u.id, 70000 FROM monthly_goal g, app_users u WHERE g.period = '2026-11-01' AND u.role = 'VENDEDOR';
  SET CONSTRAINTS ALL IMMEDIATE;
  RAISE NOTICE 'FAIL BD-16 distribuicao 140.000 aceita para meta 240.000';
EXCEPTION WHEN check_violation THEN RAISE NOTICE 'PASS BD-16 distribuicao divergente rejeitada';
END $$;
-- BD-17 Meta zero exibe "Sem meta" sem dividir por zero (CA-10-02)
BEGIN;
INSERT INTO monthly_goal(period, amount, created_by) SELECT '2026-10-01', 0, id FROM app_users WHERE db_role = 'gerente_matheus';
INSERT INTO seller_goal(monthly_goal_id, seller_id, amount)
SELECT g.id, u.id, 0 FROM monthly_goal g, app_users u WHERE g.period = '2026-10-01' AND u.role = 'VENDEDOR';
SET CONSTRAINTS ALL IMMEDIATE;
SELECT pg_temp.ok('BD-17 Meta zero -> Sem meta e atingimento NULL',
       bool_and(situacao = 'Sem meta' AND atingimento_pct IS NULL))
  FROM v_desempenho_vendedor WHERE periodo = '2026-10-01';
ROLLBACK;
-- BD-18 Custo medio ponderado: 10 x 100 + 10 x 200 = 150
BEGIN;
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE db_role = 'gerente_matheus'), true);
INSERT INTO product(product_type, name, price, weight, sku) VALUES ('PHYSICAL', 'Teste custo', 500, 1, 'TST-CUSTO');
INSERT INTO stock_entry(product_id, quantity, unit_cost) SELECT id, 10, 100 FROM product WHERE sku = 'TST-CUSTO';
INSERT INTO stock_entry(product_id, quantity, unit_cost) SELECT id, 10, 200 FROM product WHERE sku = 'TST-CUSTO';
SELECT pg_temp.ok('BD-18 Custo medio ponderado = 150,00', custo_medio = 150 AND saldo = 20, row(custo_medio, saldo)::text)
  FROM v_posicao_estoque WHERE sku = 'TST-CUSTO';
ROLLBACK;
-- BD-19 Confirmacao idempotente e conversao reserva -> saida (CA-08-02, CA-13-03)
BEGIN;
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'allanbarros@sbi.tech'), true);
SELECT fn_confirmar_pagamento((SELECT id FROM orders WHERE status = 'PENDING_PAYMENT' AND seller_id = app_current_user_id())) AS primeira \gset
SELECT fn_confirmar_pagamento((SELECT id FROM orders WHERE seller_id = app_current_user_id() ORDER BY id DESC LIMIT 1)) AS segunda \gset
SELECT pg_temp.ok('BD-19 Segunda confirmacao sem efeito; iPhone 9/1/8',
       :'primeira'::boolean AND NOT :'segunda'::boolean AND saldo = 9 AND reservado = 1 AND disponivel = 8,
       row(saldo, reservado)::text)
  FROM v_posicao_estoque WHERE sku = 'IP-128-PT';
SELECT pg_temp.ok('BD-20 Realizado de outubro do Allan = 5.700 apos confirmacao',
       (SELECT sum(valor) FROM v_lancamento_realizado l JOIN app_users u ON u.id = l.seller_id
         WHERE u.email = 'allanbarros@sbi.tech' AND l.data >= '2026-10-01') = 5700);
ROLLBACK;
-- BD-21 Cancelamento libera reserva (CA-13-02)
BEGIN;
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'franciscoferreira@sbi.tech'), true);
SELECT fn_cancelar_pedido((SELECT id FROM orders WHERE status = 'PENDING_PAYMENT' AND seller_id = app_current_user_id()));
SELECT pg_temp.ok('BD-21 Cancelamento libera reserva (iPhone reservado 2 -> 1)', reservado = 1 AND disponivel = 9)
  FROM v_posicao_estoque WHERE sku = 'IP-128-PT';
ROLLBACK;
-- BD-22 Estorno reduz o realizado e respeita o limite pago
BEGIN;
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE db_role = 'gerente_matheus'), true);
SELECT fn_estornar(p.id, 1200, 'Devolucao conferida') FROM payment p JOIN orders o ON o.id = p.order_id
  JOIN customer c ON c.id = o.customer_id WHERE c.name = 'Juliana Rocha' AND p.amount = 1200 LIMIT 1;
-- Regra adotada (decisao pendente da equipe): o estorno abate o realizado no mes em que e registrado.
SELECT pg_temp.ok('BD-22 Estorno de 1.200 entra como -1.200 na data do estorno; setembro preservado',
       (SELECT sum(valor) FROM v_lancamento_realizado WHERE tipo = 'ESTORNO') = -1200
       AND (SELECT realizado FROM v_desempenho_vendedor WHERE periodo = '2026-09-01' AND vendedor = 'Allan Barros') = 68400);
ROLLBACK;
DO $$ BEGIN
  PERFORM set_config('app.user_id', (SELECT id::text FROM app_users WHERE db_role = 'gerente_matheus'), true);
  PERFORM fn_estornar(p.id, p.amount + 0.01, 'acima do pago') FROM payment p WHERE p.status = 'APPROVED' LIMIT 1;
  RAISE NOTICE 'FAIL BD-23 estorno acima do valor pago aceito';
EXCEPTION WHEN invalid_parameter_value THEN RAISE NOTICE 'PASS BD-23 estorno acima do pago rejeitado';
END $$;
-- BD-24 Historico de preco registra autor e valores
BEGIN;
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE db_role = 'gerente_matheus'), true);
UPDATE product SET price = 4300 WHERE sku = 'IP-128-PT';
SELECT pg_temp.ok('BD-24 Historico de preco com autor gerente',
       old_price = 4500 AND new_price = 4300 AND u.role = 'GERENTE')
  FROM product_price_history h JOIN app_users u ON u.id = h.changed_by ORDER BY h.id DESC LIMIT 1;
-- BD-25 Preco historico do item nao muda com o catalogo (CA-07-03)
SELECT pg_temp.ok('BD-25 Item vendido mantem preco 4.500 apos reajuste',
       bool_and(oi.price = 4500))
  FROM order_item oi JOIN product p ON p.id = oi.product_id WHERE p.sku = 'IP-128-PT';
ROLLBACK;
-- BD-26 Promocao aplicada calcula desconto no servidor (10% em acessorio)
BEGIN;
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'gabrielsousa@sbi.tech'), true);
SELECT fn_criar_pedido((SELECT id FROM customer WHERE name = 'Lucas Ferreira'),
       jsonb_build_array(jsonb_build_object('product_id', (SELECT id FROM product WHERE sku = 'CP-SIL'), 'quantity', 2,
                         'promotion_id', (SELECT id FROM promotion LIMIT 1))), 'PIX') AS pedido \gset
SELECT pg_temp.ok('BD-26 Desconto 10% sobre 2 x 300 = 60; total 540',
       oi.discount = 60 AND p.amount = 540, row(oi.discount, p.amount)::text)
  FROM order_item oi JOIN payment p ON p.order_id = oi.order_id WHERE oi.order_id = :pedido;
ROLLBACK;
DO $$ BEGIN
  PERFORM set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'gabrielsousa@sbi.tech'), true);
  PERFORM fn_criar_pedido((SELECT id FROM customer LIMIT 1),
       jsonb_build_array(jsonb_build_object('product_id', (SELECT id FROM product WHERE sku = 'IP-128-PT'), 'quantity', 1,
                         'promotion_id', (SELECT id FROM promotion LIMIT 1))), 'PIX');
  RAISE NOTICE 'FAIL BD-27 promocao de acessorio aplicada a smartphone';
EXCEPTION WHEN invalid_parameter_value THEN RAISE NOTICE 'PASS BD-27 promocao fora do escopo rejeitada';
END $$;
DO $$ BEGIN
  PERFORM set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'gabrielsousa@sbi.tech'), true);
  PERFORM fn_criar_pedido((SELECT id FROM customer LIMIT 1),
       jsonb_build_array(jsonb_build_object('product_id', (SELECT id FROM product WHERE sku = 'AW-41'), 'quantity', 3)), 'PIX');
  RAISE NOTICE 'FAIL BD-28 venda acima do disponivel aceita';
EXCEPTION WHEN SQLSTATE 'LJ409' THEN RAISE NOTICE 'PASS BD-28 venda acima do disponivel rejeitada (LJ409 -> HTTP 409)';
END $$;
DO $$ BEGIN
  PERFORM set_config('app.user_id', '', true);
  PERFORM fn_criar_pedido((SELECT id FROM customer LIMIT 1),
       jsonb_build_array(jsonb_build_object('product_id', (SELECT id FROM product WHERE sku = 'CP-SIL'), 'quantity', 1)), 'PIX');
  RAISE NOTICE 'FAIL BD-29 pedido sem usuario autenticado';
EXCEPTION WHEN invalid_authorization_specification THEN RAISE NOTICE 'PASS BD-29 pedido sem usuario rejeitado';
END $$;
