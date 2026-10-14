-- Testes de RBAC executados como gerente_matheus (psql -U gerente_matheus)
\set QUIET on
CREATE OR REPLACE FUNCTION pg_temp.ok(p_id text, p_cond boolean, p_info text DEFAULT '') RETURNS void
LANGUAGE plpgsql AS $$ BEGIN
  IF coalesce(p_cond, false) THEN RAISE NOTICE 'PASS %', p_id; ELSE RAISE NOTICE 'FAIL % %', p_id, p_info; END IF;
END $$;

SELECT pg_temp.ok('RG-01 Gerente ve todos os pedidos',
       (SELECT count(*) FROM orders) = (SELECT count(*) FROM v_minhas_vendas)
       AND (SELECT count(DISTINCT seller_id) FROM orders) = 3);
SELECT pg_temp.ok('RG-02 Gerente le posicao de estoque com custo', count(*) = 6 AND count(custo_medio) = 4)
  FROM v_posicao_estoque;
BEGIN;
SET LOCAL app.reason = 'Ajuste de tabela';
UPDATE product SET price = 1150 WHERE sku = 'AP-BR';
SELECT pg_temp.ok('RG-03 Atualizacao de preco registra gerente e motivo',
       h.new_price = 1150 AND u.name = 'Matheus Siqueira' AND h.reason = 'Ajuste de tabela')
  FROM product_price_history h JOIN app_users u ON u.id = h.changed_by ORDER BY h.id DESC LIMIT 1;
INSERT INTO promotion(name, kind, value, product_id, starts_at, ends_at)
SELECT 'AirPods R$ 100 off', 'AMOUNT', 100, id, now(), now() + interval '3 days' FROM product WHERE sku = 'AP-BR';
SELECT pg_temp.ok('RG-04 Gerente libera promocao, visivel como ativa',
       EXISTS (SELECT 1 FROM v_promocoes_ativas WHERE promocao = 'AirPods R$ 100 off'));
INSERT INTO stock_entry(product_id, quantity, unit_cost) SELECT id, 5, 1950 FROM product WHERE sku = 'AW-41';
SELECT pg_temp.ok('RG-05 Entrada de estoque soma saldo e recalcula custo (2x1900 + 5x1950)',
       saldo = 7 AND custo_medio = 1935.71, row(saldo, custo_medio)::text)
  FROM v_posicao_estoque WHERE sku = 'AW-41';
ROLLBACK;
DO $$ BEGIN UPDATE stock_balance SET on_hand = 999; RAISE NOTICE 'FAIL RG-06 gerente altera saldo sem entrada';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RG-06 saldo so muda por entrada/movimento'; END $$;
DO $$ BEGIN DELETE FROM orders; RAISE NOTICE 'FAIL RG-07 exclusao fisica de pedidos permitida';
EXCEPTION WHEN insufficient_privilege THEN RAISE NOTICE 'PASS RG-07 exclusao fisica de pedidos negada'; END $$;
