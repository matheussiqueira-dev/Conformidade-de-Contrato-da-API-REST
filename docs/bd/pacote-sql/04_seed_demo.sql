-- =====================================================================
-- Loja Gestao - massa sintetica de demonstracao (valores e pessoas ficticios)
-- Reproduz os numeros do Guia Visual: setembro/2026, meta R$ 240.000,
-- Ana R$ 68.400 / Bruno R$ 62.000 / Carla R$ 56.000 (loja R$ 186.400) e
-- evolucao semanal 38.200 / 41.600 / 36.800 / 42.300 / 27.500.
-- Executar como dono do esquema, uma unica vez, em banco descartavel.
-- =====================================================================
BEGIN;

-- Usuarios (senha propositalmente invalida: contas de login da aplicacao
-- devem ser criadas no ambiente de demo com senha vinda de variavel de ambiente).
INSERT INTO app_users(name, email, password_hash, role, db_role) VALUES
 ('Rafael Mendes', 'rafael@loja.test', 'SEM-SENHA-DEMO', 'GERENTE',  'gerente_rafael'),
 ('Ana Souza',     'ana@loja.test',    'SEM-SENHA-DEMO', 'VENDEDOR', 'vendedora_ana'),
 ('Bruno Lima',    'bruno@loja.test',  'SEM-SENHA-DEMO', 'VENDEDOR', 'vendedor_bruno'),
 ('Carla Alves',   'carla@loja.test',  'SEM-SENHA-DEMO', 'VENDEDOR', 'vendedora_carla');

SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'rafael@loja.test'), true);

INSERT INTO category(name) VALUES ('Smartphones'), ('Audio'), ('Wearables'), ('Acessorios');

INSERT INTO product(product_type, name, price, description, weight, sku, category_id) VALUES
 ('PHYSICAL', 'iPhone 128 GB Preto', 4500.00, 'Preco ficticio', 0.17, 'IP-128-PT', (SELECT id FROM category WHERE name = 'Smartphones')),
 ('PHYSICAL', 'AirPods Branco',      1200.00, 'Preco ficticio', 0.05, 'AP-BR',     (SELECT id FROM category WHERE name = 'Audio')),
 ('PHYSICAL', 'Apple Watch 41 mm',   2800.00, 'Preco ficticio', 0.03, 'AW-41',     (SELECT id FROM category WHERE name = 'Wearables')),
 ('PHYSICAL', 'Capa de silicone',     300.00, 'Preco ficticio', 0.02, 'CP-SIL',    (SELECT id FROM category WHERE name = 'Acessorios')),
 ('PHYSICAL', 'Carregador 20 W',      200.00, 'Preco ficticio', 0.06, 'CG-20W',    (SELECT id FROM category WHERE name = 'Acessorios')),
 ('PHYSICAL', 'Pelicula de vidro',    100.00, 'Preco ficticio', 0.01, 'PL-VID',    (SELECT id FROM category WHERE name = 'Acessorios'));

UPDATE stock_balance b SET min_level = v.m
  FROM (VALUES ('IP-128-PT', 5), ('AP-BR', 5), ('AW-41', 3), ('CP-SIL', 10), ('CG-20W', 10), ('PL-VID', 10)) v(sku, m)
  JOIN product p ON p.sku = v.sku
 WHERE b.product_id = p.id;

INSERT INTO customer(customer_type, name, email, cpf) VALUES
 ('INDIVIDUAL', 'Marina Costa',   'marina@cliente.test',  '00000000101'),
 ('INDIVIDUAL', 'Pedro Martins',  'pedro@cliente.test',   '00000000102'),
 ('INDIVIDUAL', 'Juliana Rocha',  'juliana@cliente.test', '00000000103'),
 ('INDIVIDUAL', 'Lucas Ferreira', 'lucas@cliente.test',   '00000000104'),
 ('INDIVIDUAL', 'Beatriz Nunes',  'beatriz@cliente.test', '00000000105'),
 ('INDIVIDUAL', 'Rodrigo Alves',  'rodrigo@cliente.test', '00000000106');

-- Metas de setembro (a soma e verificada no COMMIT).
INSERT INTO monthly_goal(period, amount) VALUES ('2026-09-01', 240000.00);
INSERT INTO seller_goal(monthly_goal_id, seller_id, amount)
SELECT g.id, u.id, 80000.00 FROM monthly_goal g, app_users u
 WHERE g.period = '2026-09-01' AND u.role = 'VENDEDOR';

-- Vendas pagas de setembro, decompostas de forma deterministica.
DO $$
DECLARE
    t record; v_rem numeric; v_k int; v_day date; v_order bigint; v_items jsonb; v_it jsonb;
    v_cust bigint[]; v_ci int := 0; v_pay text[] := ARRAY['PIX','CARD','BOLETO']; v_pi int := 0;
    v_sum numeric;
BEGIN
    SELECT array_agg(id ORDER BY id) INTO v_cust FROM customer;
    CREATE TEMP TABLE alvo(email text, inicio date, fim date, valor numeric, fixo jsonb);
    INSERT INTO alvo VALUES
     ('ana@loja.test','2026-09-01','2026-09-06',12000,NULL), ('ana@loja.test','2026-09-07','2026-09-13',15000,NULL),
     ('ana@loja.test','2026-09-14','2026-09-20',13400,NULL),
     ('ana@loja.test','2026-09-21','2026-09-27',18000,'[{"d":"2026-09-26","c":"Juliana Rocha","i":[["AP-BR",1]]}]'),
     ('ana@loja.test','2026-09-28','2026-09-30',10000,'[{"d":"2026-09-30","c":"Marina Costa","i":[["IP-128-PT",1],["AP-BR",1]]}]'),
     ('bruno@loja.test','2026-09-01','2026-09-06',14000,NULL), ('bruno@loja.test','2026-09-07','2026-09-13',14200,NULL),
     ('bruno@loja.test','2026-09-14','2026-09-20',12000,NULL), ('bruno@loja.test','2026-09-21','2026-09-27',12800,NULL),
     ('bruno@loja.test','2026-09-28','2026-09-30',9000,NULL),
     ('carla@loja.test','2026-09-01','2026-09-06',12200,NULL), ('carla@loja.test','2026-09-07','2026-09-13',12400,NULL),
     ('carla@loja.test','2026-09-14','2026-09-20',11400,NULL), ('carla@loja.test','2026-09-21','2026-09-27',11500,NULL),
     ('carla@loja.test','2026-09-28','2026-09-30',8500,NULL);

    FOR t IN SELECT * FROM alvo ORDER BY inicio, email LOOP
        v_rem := t.valor; v_k := 0;
        LOOP
            EXIT WHEN v_rem <= 0;
            v_items := NULL;
            IF t.fixo IS NOT NULL AND v_k < jsonb_array_length(t.fixo) THEN
                v_it := t.fixo -> v_k;
                v_day := (v_it->>'d')::date;
                v_items := v_it->'i';
            ELSE
                v_day := t.inicio + (v_k % (t.fim - t.inicio + 1));
                v_items := CASE
                    WHEN v_rem >= 5700 THEN '[["IP-128-PT",1],["AP-BR",1]]'
                    WHEN v_rem >= 4500 THEN '[["IP-128-PT",1]]'
                    WHEN v_rem >= 2800 THEN '[["AW-41",1]]'
                    WHEN v_rem >= 1200 THEN '[["AP-BR",1]]'
                    WHEN v_rem >=  300 THEN '[["CP-SIL",1]]'
                    WHEN v_rem >=  200 THEN '[["CG-20W",1]]'
                    ELSE '[["PL-VID",1]]' END::jsonb;
            END IF;

            INSERT INTO orders(moment, status, customer_id, seller_id)
            VALUES (v_day + time '10:00' + (v_k * interval '37 minutes'), 'PAID',
                    CASE WHEN t.fixo IS NOT NULL AND v_k < jsonb_array_length(t.fixo)
                         THEN (SELECT id FROM customer WHERE name = v_it->>'c')
                         ELSE v_cust[1 + (v_ci % array_length(v_cust, 1))] END,
                    (SELECT id FROM app_users WHERE email = t.email))
            RETURNING id INTO v_order;
            v_ci := v_ci + 1;

            INSERT INTO order_item(quantity, price, order_id, product_id)
            SELECT (e->>1)::int, p.price, v_order, p.id
              FROM jsonb_array_elements(v_items) e JOIN product p ON p.sku = e->>0;

            SELECT sum(price * quantity) INTO v_sum FROM order_item WHERE order_id = v_order;
            INSERT INTO payment(payment_type, amount, status, payment_date, order_id, due_date)
            VALUES (v_pay[1 + (v_pi % 3)], v_sum, 'APPROVED',
                    v_day + time '10:30' + (v_k * interval '37 minutes'), v_order,
                    CASE WHEN v_pay[1 + (v_pi % 3)] = 'BOLETO' THEN v_day + 3 END);
            v_pi := v_pi + 1;
            v_rem := v_rem - v_sum; v_k := v_k + 1;
        END LOOP;
        IF v_rem <> 0 THEN RAISE EXCEPTION 'decomposicao nao fechou para % em %', t.email, t.inicio; END IF;
    END LOOP;
    DROP TABLE alvo;
END $$;

-- Entradas de estoque em 25/08 = vendido em setembro + saldo final desejado.
-- iPhone e AirPods sem custo informado (como no Guia Visual); demais com custo ficticio.
INSERT INTO stock_entry(product_id, quantity, unit_cost, created_at, note)
SELECT p.id, coalesce(s.vendido, 0) + v.saldo_final, v.custo, timestamptz '2026-08-25 09:00-03', 'Carga inicial da demonstracao'
  FROM (VALUES ('IP-128-PT', 10, NULL::numeric), ('AP-BR', 13, NULL), ('AW-41', 2, 1900.00),
               ('CP-SIL', 25, 90.00), ('CG-20W', 30, 70.00), ('PL-VID', 40, 25.00)) v(sku, saldo_final, custo)
  JOIN product p ON p.sku = v.sku
  LEFT JOIN (SELECT product_id, sum(quantity) AS vendido FROM order_item GROUP BY product_id) s ON s.product_id = p.id;

-- Baixa das vendas pagas (saida) com custo atribuido historico.
UPDATE order_item oi SET unit_cost = b.avg_cost FROM stock_balance b WHERE b.product_id = oi.product_id;
INSERT INTO stock_movement(product_id, order_id, kind, quantity, created_by, created_at)
SELECT oi.product_id, oi.order_id, 'SAIDA', oi.quantity, o.seller_id, (o.moment AT TIME ZONE 'America/Fortaleza')
  FROM order_item oi JOIN orders o ON o.id = oi.order_id;
UPDATE stock_balance b SET on_hand = b.on_hand - s.q
  FROM (SELECT product_id, sum(quantity) q FROM order_item GROUP BY product_id) s
 WHERE b.product_id = s.product_id;

-- Promocao liberada pelo gerente para outubro (categoria Acessorios, 10%).
INSERT INTO promotion(name, kind, value, category_id, starts_at, ends_at)
SELECT 'Acessorios 10% - outubro', 'PERCENT', 10, id, '2026-10-01 00:00-03', '2026-11-01 00:00-03'
  FROM category WHERE name = 'Acessorios';

COMMIT;

-- Pedidos pendentes de 01/10 criados pelo fluxo real (reservam estoque).
BEGIN;
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'ana@loja.test'), true);
SELECT fn_criar_pedido((SELECT id FROM customer WHERE name = 'Marina Costa'),
       jsonb_build_array(jsonb_build_object('product_id', (SELECT id FROM product WHERE sku = 'IP-128-PT'), 'quantity', 1),
                         jsonb_build_object('product_id', (SELECT id FROM product WHERE sku = 'AP-BR'),     'quantity', 1)), 'PIX');
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE email = 'bruno@loja.test'), true);
SELECT fn_criar_pedido((SELECT id FROM customer WHERE name = 'Pedro Martins'),
       jsonb_build_array(jsonb_build_object('product_id', (SELECT id FROM product WHERE sku = 'IP-128-PT'), 'quantity', 1)), 'CARD');
UPDATE orders SET moment = '2026-10-01 09:40' WHERE status = 'PENDING_PAYMENT';
COMMIT;
