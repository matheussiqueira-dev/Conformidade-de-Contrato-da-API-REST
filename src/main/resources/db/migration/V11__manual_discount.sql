-- G-04 (docs/decisoes/evolucao.md): desconto manual com alcada por papel.
-- O desconto manual e rateado nos itens: order_item.discount passa a ser promocao + parte manual, e
-- order_item.manual_discount guarda a parte manual. Assim o liquido (preco x quantidade - discount) continua
-- igual em todas as views de faturamento, metas e clientes, sem mudar o sentido de nenhuma metrica.

CREATE TABLE discount_policy (
    role             varchar(20)  PRIMARY KEY REFERENCES role(code),
    max_percent      numeric(5,2) NOT NULL CHECK (max_percent BETWEEN 0 AND 100),
    allow_below_cost boolean      NOT NULL DEFAULT false,
    updated_by       bigint       DEFAULT app_current_user_id() REFERENCES app_users(id),
    updated_at       timestamptz  NOT NULL DEFAULT now()
);
-- Valores iniciais de exemplo; a equipe define a alcada real (docs/decisoes/regras-loja.md, DEC-01).
INSERT INTO discount_policy(role, max_percent, allow_below_cost) VALUES ('VENDEDOR', 5.00, false), ('GERENTE', 20.00, false);

ALTER TABLE orders
    ADD COLUMN manual_discount         numeric(12,2) NOT NULL DEFAULT 0 CHECK (manual_discount >= 0),
    ADD COLUMN manual_discount_percent numeric(5,2) CHECK (manual_discount_percent > 0 AND manual_discount_percent <= 100),
    ADD COLUMN manual_discount_reason  varchar(200),
    ADD COLUMN discount_approved_by    bigint REFERENCES app_users(id),
    ADD CONSTRAINT ck_orders_manual_discount_reason
        CHECK (manual_discount_percent IS NULL OR (manual_discount_reason IS NOT NULL AND length(manual_discount_reason) >= 5));

ALTER TABLE order_item
    ADD COLUMN manual_discount numeric(12,2) NOT NULL DEFAULT 0 CHECK (manual_discount >= 0),
    ADD CONSTRAINT ck_order_item_manual_discount CHECK (manual_discount <= discount);

-- Uso de promocoes: so a parte da promocao (sem o rateio manual); mesmas colunas da V6.
CREATE OR REPLACE VIEW v_uso_promocoes AS
SELECT pr.id AS promotion_id, pr.name AS promocao,
       count(oi.id) AS itens, coalesce(sum(oi.discount - oi.manual_discount), 0) AS desconto_total,
       coalesce(sum(oi.price * oi.quantity - oi.discount), 0) AS receita_liquida
  FROM promotion pr LEFT JOIN order_item oi ON oi.promotion_id = pr.id
 GROUP BY pr.id, pr.name;

-- Contrapartida de v_uso_promocoes: descontos manuais em pedidos pagos, por mes do pagamento e vendedor.
CREATE OR REPLACE VIEW v_descontos_manuais AS
SELECT date_trunc('month', p.payment_date)::date              AS periodo,
       o.seller_id,
       u.name                                                  AS vendedor,
       count(*)                                                AS pedidos,
       sum(o.manual_discount)                                  AS valor_concedido,
       count(*) FILTER (WHERE o.discount_approved_by IS NOT NULL) AS aprovados_por_gerente
  FROM orders o
  JOIN payment p   ON p.order_id = o.id AND p.status = 'APPROVED'
  JOIN app_users u ON u.id = o.seller_id
 WHERE o.manual_discount > 0
 GROUP BY date_trunc('month', p.payment_date), o.seller_id, u.name;

ALTER TABLE audit_event DROP CONSTRAINT ck_audit_event_action;
ALTER TABLE audit_event ADD CONSTRAINT ck_audit_event_action CHECK (action IN (
    'LOGIN_SUCCEEDED','LOGIN_FAILED','ACCOUNT_LOCKED','LOGOUT','CUSTOMER_CREATED','CUSTOMER_UPDATED',
    'CUSTOMER_DELETED','STOCK_ADJUSTED','PRODUCT_STATUS_CHANGED','ORDER_CANCELED',
    'USER_CREATED','USER_UPDATED','USER_ROLE_CHANGED','USER_STATUS_CHANGED','ACCOUNT_UNLOCKED',
    'PASSWORD_RESET','PASSWORD_CHANGED','DISCOUNT_APPROVED','DISCOUNT_POLICY_CHANGED'));
