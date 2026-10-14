-- =====================================================================
-- Testes de banco da evolucao (dono do esquema). Cada caso imprime PASS ou FAIL e reverte a propria alteracao.
-- CT-BD-30: gatilho keep_one_active_manager (V8) garante pelo menos um GERENTE ativo.
-- =====================================================================
\set QUIET on

-- BD-30a Rebaixar o unico GERENTE ativo e rejeitado
DO $$ BEGIN
  UPDATE app_users SET role = 'VENDEDOR' WHERE email = 'matheussiqueira@sbi.tech';
  SET CONSTRAINTS keep_one_active_manager IMMEDIATE;
  RAISE NOTICE 'FAIL BD-30a ultimo gerente rebaixado';
  RAISE EXCEPTION USING ERRCODE = 'LJ000';
EXCEPTION
  WHEN raise_exception THEN
    IF SQLERRM LIKE 'LAST_MANAGER:%' THEN RAISE NOTICE 'PASS BD-30a ultimo gerente nao pode ser rebaixado';
    ELSE RAISE NOTICE 'FAIL BD-30a erro inesperado: %', SQLERRM; END IF;
  WHEN SQLSTATE 'LJ000' THEN NULL;
END $$;

-- BD-30b Desativar o unico GERENTE ativo e rejeitado
DO $$ BEGIN
  UPDATE app_users SET active = false WHERE email = 'matheussiqueira@sbi.tech';
  SET CONSTRAINTS keep_one_active_manager IMMEDIATE;
  RAISE NOTICE 'FAIL BD-30b ultimo gerente desativado';
  RAISE EXCEPTION USING ERRCODE = 'LJ000';
EXCEPTION
  WHEN raise_exception THEN
    IF SQLERRM LIKE 'LAST_MANAGER:%' THEN RAISE NOTICE 'PASS BD-30b ultimo gerente nao pode ser desativado';
    ELSE RAISE NOTICE 'FAIL BD-30b erro inesperado: %', SQLERRM; END IF;
  WHEN SQLSTATE 'LJ000' THEN NULL;
END $$;

-- BD-30c Com outro GERENTE ativo, o rebaixamento e aceito
DO $$ BEGIN
  UPDATE app_users SET role = 'GERENTE' WHERE email = 'allanbarros@sbi.tech';
  UPDATE app_users SET role = 'VENDEDOR' WHERE email = 'matheussiqueira@sbi.tech';
  SET CONSTRAINTS keep_one_active_manager IMMEDIATE;
  RAISE NOTICE 'PASS BD-30c rebaixamento aceito com outro gerente ativo';
  RAISE EXCEPTION USING ERRCODE = 'LJ000';
EXCEPTION
  WHEN raise_exception THEN RAISE NOTICE 'FAIL BD-30c rebaixamento rejeitado: %', SQLERRM;
  WHEN SQLSTATE 'LJ000' THEN NULL;
END $$;

-- CT-CUS-121 / BD-31: v_cliente_resumo (V9) confere com o seed demo e com o faturamento
CREATE OR REPLACE FUNCTION pg_temp.ok(p_id text, p_cond boolean, p_info text DEFAULT '') RETURNS void
LANGUAGE plpgsql AS $$ BEGIN
  IF coalesce(p_cond, false) THEN RAISE NOTICE 'PASS %', p_id; ELSE RAISE NOTICE 'FAIL % %', p_id, p_info; END IF;
END $$;
SELECT pg_temp.ok('BD-31a Resumo dos clientes soma 186.400 em 65 pedidos pagos (seed)',
       sum(liquido) = 186400 AND sum(pedidos_pagos) = 65, row(sum(liquido), sum(pedidos_pagos))::text)
  FROM v_cliente_resumo;
SELECT pg_temp.ok('BD-31b Resumo dos clientes igual ao faturamento liquido de v_faturamento_diario',
       (SELECT sum(liquido) FROM v_cliente_resumo) = (SELECT sum(faturamento_liquido) FROM v_faturamento_diario));
SELECT pg_temp.ok('BD-31c Marina Costa: primeira e ultima compra e pedidos pagos coerentes com os pedidos',
       r.pedidos_pagos = (SELECT count(*) FROM orders o JOIN payment p ON p.order_id = o.id
                           WHERE p.status = 'APPROVED' AND o.customer_id = c.id)
       AND r.primeira <= r.ultima, row(r.pedidos_pagos, r.primeira, r.ultima)::text)
  FROM customer c
  CROSS JOIN LATERAL (SELECT sum(pedidos_pagos) AS pedidos_pagos, min(primeira_compra) AS primeira, max(ultima_compra) AS ultima
                        FROM v_cliente_resumo WHERE customer_id = c.id) r
 WHERE c.name = 'Marina Costa';

-- BD-32: desconto manual (V11)
SELECT pg_temp.ok('BD-32a Alcadas iniciais: VENDEDOR 5% e GERENTE 20%, sem venda abaixo do custo',
       (SELECT max_percent FROM discount_policy WHERE role = 'VENDEDOR') = 5
       AND (SELECT max_percent FROM discount_policy WHERE role = 'GERENTE') = 20
       AND NOT EXISTS (SELECT 1 FROM discount_policy WHERE allow_below_cost));
DO $$ BEGIN
  UPDATE orders SET manual_discount = 1, manual_discount_percent = 1 WHERE id = (SELECT min(id) FROM orders);
  RAISE NOTICE 'FAIL BD-32b desconto manual sem motivo aceito';
  RAISE EXCEPTION USING ERRCODE = 'LJ000';
EXCEPTION
  WHEN check_violation THEN RAISE NOTICE 'PASS BD-32b desconto manual exige motivo';
  WHEN SQLSTATE 'LJ000' THEN NULL;
END $$;
DO $$ BEGIN
  UPDATE order_item SET manual_discount = discount + 1 WHERE id = (SELECT min(id) FROM order_item);
  RAISE NOTICE 'FAIL BD-32c parte manual maior que o desconto do item aceita';
  RAISE EXCEPTION USING ERRCODE = 'LJ000';
EXCEPTION
  WHEN check_violation THEN RAISE NOTICE 'PASS BD-32c parte manual nunca passa do desconto do item';
  WHEN SQLSTATE 'LJ000' THEN NULL;
END $$;
