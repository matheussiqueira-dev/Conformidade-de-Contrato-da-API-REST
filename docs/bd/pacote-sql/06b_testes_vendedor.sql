-- Testes de RBAC executados como vendedor_allan (psql -U vendedor_allan)
\set QUIET on
CREATE OR REPLACE FUNCTION pg_temp.ok(p_id text, p_cond boolean, p_info text DEFAULT '') RETURNS void
LANGUAGE plpgsql AS $$ BEGIN
  IF coalesce(p_cond, false) THEN RAISE NOTICE 'PASS %', p_id; ELSE RAISE NOTICE 'FAIL % %', p_id, p_info; END IF;
END $$;

DO $$ BEGIN PERFORM 1 FROM product LIMIT 1; RAISE NOTICE 'FAIL RB-01 vendedor le tabela product';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-01 tabela product (custo) negada ao vendedor'; END $$;
DO $$ BEGIN PERFORM 1 FROM v_margem_produto LIMIT 1; RAISE NOTICE 'FAIL RB-02 vendedor le margem';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-02 margem negada ao vendedor'; END $$;
DO $$ BEGIN PERFORM 1 FROM v_resumo_loja LIMIT 1; RAISE NOTICE 'FAIL RB-03 vendedor le resultado da loja';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-03 resultado da loja negado ao vendedor'; END $$;
DO $$ BEGIN PERFORM unit_cost FROM order_item LIMIT 1; RAISE NOTICE 'FAIL RB-04 vendedor le custo do item';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-04 coluna unit_cost negada ao vendedor'; END $$;
DO $$ BEGIN PERFORM card_number FROM payment LIMIT 1; RAISE NOTICE 'FAIL RB-05 vendedor le dados de cartao';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-05 coluna card_number negada ao vendedor'; END $$;
DO $$ BEGIN UPDATE stock_balance SET min_level = 0; RAISE NOTICE 'FAIL RB-06 vendedor altera estoque';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-06 alteracao de estoque negada'; END $$;
DO $$ BEGIN INSERT INTO promotion(name, kind, value, starts_at, ends_at) VALUES ('x','PERCENT',5,now(),now()+interval '1 day');
  RAISE NOTICE 'FAIL RB-07 vendedor cria promocao';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-07 criacao de promocao negada'; END $$;
DO $$ BEGIN PERFORM fn_estornar(1, 1, 'x'); RAISE NOTICE 'FAIL RB-08 vendedor estorna';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-08 estorno negado ao vendedor'; END $$;

SELECT pg_temp.ok('RB-09 Catalogo do vendedor sem colunas de custo',
       NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'v_catalogo_vendedor'
                   AND column_name ~ '(cost|custo|margem|saldo)')
       AND (SELECT count(*) FROM v_catalogo_vendedor) = 6);
SELECT pg_temp.ok('RB-10 Vendedor ve somente os proprios pedidos (BOLA)',
       (SELECT count(*) FROM orders WHERE seller_id <> app_current_user_id()) = 0
       AND (SELECT count(*) FROM orders) > 0);
SELECT pg_temp.ok('RB-11 Meu desempenho retorna somente o Allan',
       count(*) = 1 AND bool_and(vendedor = 'Allan Barros' AND atingimento_pct = 57.0))
  FROM v_meu_desempenho WHERE periodo = '2026-09-01';
SELECT pg_temp.ok('RB-12 Minha evolucao semanal = 12.000/15.000/13.400/18.000/10.000',
       array_agg(realizado ORDER BY inicio) = ARRAY[12000,15000,13400,18000,10000]::numeric[])
  FROM v_minha_evolucao_semanal WHERE periodo = '2026-09-01';
-- RB-13 Cancelar pedido de outro vendedor (id informado pelo runner: psql -v francisco_pedido=...)
SELECT set_config('teste.francisco_pedido', :'francisco_pedido', false) \gset
DO $$ BEGIN
  PERFORM fn_cancelar_pedido(current_setting('teste.francisco_pedido')::bigint);
  RAISE NOTICE 'FAIL RB-13 vendedora cancelou pedido do Francisco';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RB-13 cancelamento de pedido alheio negado';
END $$;
-- RB-14 Autoria derivada da sessao (nao ha parametro de vendedor)
BEGIN;
SELECT fn_criar_pedido((SELECT id FROM customer WHERE name = 'Beatriz Nunes'),
       jsonb_build_array(jsonb_build_object('product_id', (SELECT product_id FROM v_catalogo_vendedor WHERE sku = 'PL-VID'),
                         'quantity', 1, 'seller_id', 999)), 'PIX') AS novo \gset
SELECT pg_temp.ok('RB-14 Pedido criado pela vendedora tem seller = Allan',
       seller_id = app_current_user_id()) FROM orders WHERE id = :novo;
ROLLBACK;
