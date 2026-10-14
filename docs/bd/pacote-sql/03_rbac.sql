-- =====================================================================
-- Loja Gestao - RBAC no banco (defesa em profundidade e demonstracao SQL)
-- A autorizacao principal continua na API (Spring Security + verificacao de
-- dono do recurso). Aqui: papeis, GRANT minimo, GRANT por coluna e RLS.
-- Logins de demonstracao sem senha: somente para ambiente local/descartavel.
-- =====================================================================

DO $$
DECLARE r text;
BEGIN
    FOREACH r IN ARRAY ARRAY['loja_gerente','loja_vendedor'] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            EXECUTE format('CREATE ROLE %I NOLOGIN', r);
        END IF;
    END LOOP;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'gerente_rafael') THEN
        CREATE ROLE gerente_rafael LOGIN IN ROLE loja_gerente;
    END IF;
    FOREACH r IN ARRAY ARRAY['vendedora_ana','vendedor_bruno','vendedora_carla'] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            EXECUTE format('CREATE ROLE %I LOGIN IN ROLE loja_vendedor', r);
        END IF;
    END LOOP;
END $$;

-- Nada e publico por padrao (funcoes nascem com EXECUTE para PUBLIC).
REVOKE ALL ON ALL TABLES    IN SCHEMA public FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO loja_gerente, loja_vendedor;
GRANT EXECUTE ON FUNCTION app_current_user_id(), app_current_role() TO loja_gerente, loja_vendedor;

-- ---------------- Gerente ----------------
GRANT SELECT, INSERT, UPDATE, DELETE ON category, product, promotion, customer, address,
      monthly_goal, seller_goal TO loja_gerente;
GRANT SELECT, INSERT ON stock_entry TO loja_gerente;
GRANT SELECT ON stock_balance, stock_movement, product_price_history, refund,
      orders, order_item, payment TO loja_gerente;
GRANT UPDATE (min_level) ON stock_balance TO loja_gerente;
GRANT SELECT (id, name, email, role) ON app_users TO loja_gerente;
GRANT SELECT ON v_lancamento_realizado, v_desempenho_vendedor, v_resumo_loja, v_evolucao_semanal,
      v_evolucao_semanal_loja, v_posicao_estoque, v_alerta_estoque, v_catalogo_vendedor,
      v_margem_produto, v_vendas_forma_pagamento, v_promocoes_ativas, v_uso_promocoes,
      v_minhas_vendas, v_faturamento_diario TO loja_gerente;
GRANT EXECUTE ON FUNCTION fn_criar_pedido(bigint, jsonb, varchar), fn_confirmar_pagamento(bigint),
      fn_cancelar_pedido(bigint), fn_estornar(bigint, numeric, varchar) TO loja_gerente;

-- ---------------- Vendedor ----------------
-- Sem acesso a product/stock_* (custo), metas da equipe, estornos e margens.
GRANT SELECT ON v_catalogo_vendedor, v_promocoes_ativas, v_meu_desempenho,
      v_minha_evolucao_semanal, v_minhas_vendas TO loja_vendedor;
GRANT SELECT, INSERT ON customer, address TO loja_vendedor;
GRANT SELECT ON orders TO loja_vendedor;
GRANT SELECT (id, quantity, price, order_id, product_id, promotion_id, discount) ON order_item TO loja_vendedor;
GRANT SELECT (id, payment_type, amount, status, payment_date, order_id) ON payment TO loja_vendedor;
GRANT EXECUTE ON FUNCTION fn_criar_pedido(bigint, jsonb, varchar), fn_confirmar_pagamento(bigint),
      fn_cancelar_pedido(bigint) TO loja_vendedor;

-- ---------------- Row-Level Security (objeto por dono) ----------------
ALTER TABLE orders     ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment    ENABLE ROW LEVEL SECURITY;

CREATE POLICY orders_gerente  ON orders     FOR SELECT TO loja_gerente  USING (true);
CREATE POLICY orders_vendedor ON orders     FOR SELECT TO loja_vendedor USING (seller_id = app_current_user_id());
CREATE POLICY items_gerente   ON order_item FOR SELECT TO loja_gerente  USING (true);
CREATE POLICY items_vendedor  ON order_item FOR SELECT TO loja_vendedor
       USING (EXISTS (SELECT 1 FROM orders o WHERE o.id = order_item.order_id));
CREATE POLICY pay_gerente     ON payment    FOR SELECT TO loja_gerente  USING (true);
CREATE POLICY pay_vendedor    ON payment    FOR SELECT TO loja_vendedor
       USING (EXISTS (SELECT 1 FROM orders o WHERE o.id = payment.order_id));
-- Escritas em pedidos ocorrem somente pelas funcoes fn_* (SECURITY DEFINER).
