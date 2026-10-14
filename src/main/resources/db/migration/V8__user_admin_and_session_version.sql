-- G-01/G-03 (docs/decisoes/evolucao.md): cadastro de usuarios, revogacao de sessao por versao e
-- invariante "sempre existe pelo menos um GERENTE ativo".

-- Pre-verificacao: o indice unico por e-mail em minusculas nao pode nascer sobre duplicidade existente.
DO $$
DECLARE v text;
BEGIN
    SELECT string_agg(lower(email), ', ') INTO v
      FROM (SELECT lower(email) AS email FROM app_users GROUP BY lower(email) HAVING count(*) > 1) d;
    IF v IS NOT NULL THEN RAISE EXCEPTION 'V8: e-mails duplicados ignorando maiusculas: %', v; END IF;
END $$;

ALTER TABLE app_users
    ADD COLUMN session_version      integer     NOT NULL DEFAULT 0,
    ADD COLUMN must_change_password boolean     NOT NULL DEFAULT false,
    ADD COLUMN password_changed_at  timestamptz,
    ADD COLUMN updated_at           timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN created_by           bigint      DEFAULT app_current_user_id() REFERENCES app_users(id),
    ADD COLUMN version              integer     NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_app_users_session_version CHECK (session_version >= 0),
    ADD CONSTRAINT ck_app_users_version CHECK (version >= 0);

CREATE UNIQUE INDEX ux_app_users_email_lower ON app_users (lower(email));

-- Invariante 2.1: sempre um GERENTE ativo. A trava consultiva serializa a verificacao entre transacoes
-- concorrentes (dois gerentes rebaixando um ao outro ao mesmo tempo nao zeram a contagem).
CREATE OR REPLACE FUNCTION trg_keep_one_active_manager() RETURNS trigger
LANGUAGE plpgsql SET search_path = ${flyway:defaultSchema}, pg_temp AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('keep_one_active_manager'));
    IF NOT EXISTS (SELECT 1 FROM app_users WHERE role = 'GERENTE' AND active) THEN
        RAISE EXCEPTION 'LAST_MANAGER: at least one active manager is required' USING ERRCODE = 'P0001';
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER keep_one_active_manager
    AFTER UPDATE OF role, active OR DELETE ON app_users
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION trg_keep_one_active_manager();

-- Auditoria das operacoes de usuario (nunca senha nem hash no detalhe).
ALTER TABLE audit_event DROP CONSTRAINT ck_audit_event_action;
ALTER TABLE audit_event ADD CONSTRAINT ck_audit_event_action CHECK (action IN (
    'LOGIN_SUCCEEDED','LOGIN_FAILED','ACCOUNT_LOCKED','LOGOUT','CUSTOMER_CREATED','CUSTOMER_UPDATED',
    'CUSTOMER_DELETED','STOCK_ADJUSTED','PRODUCT_STATUS_CHANGED','ORDER_CANCELED',
    'USER_CREATED','USER_UPDATED','USER_ROLE_CHANGED','USER_STATUS_CHANGED','ACCOUNT_UNLOCKED',
    'PASSWORD_RESET','PASSWORD_CHANGED'));
