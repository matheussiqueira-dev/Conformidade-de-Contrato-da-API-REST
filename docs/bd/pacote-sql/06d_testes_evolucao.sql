-- =====================================================================
-- Testes de banco da evolucao (dono do esquema). Cada caso imprime PASS ou FAIL e reverte a propria alteracao.
-- CT-BD-30: gatilho keep_one_active_manager (V8) garante pelo menos um GERENTE ativo.
-- =====================================================================
\set QUIET on

-- BD-30a Rebaixar o unico GERENTE ativo e rejeitado
DO $$ BEGIN
  UPDATE app_users SET role = 'VENDEDOR' WHERE email = 'rafael@loja.test';
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
  UPDATE app_users SET active = false WHERE email = 'rafael@loja.test';
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
  UPDATE app_users SET role = 'GERENTE' WHERE email = 'ana@loja.test';
  UPDATE app_users SET role = 'VENDEDOR' WHERE email = 'rafael@loja.test';
  SET CONSTRAINTS keep_one_active_manager IMMEDIATE;
  RAISE NOTICE 'PASS BD-30c rebaixamento aceito com outro gerente ativo';
  RAISE EXCEPTION USING ERRCODE = 'LJ000';
EXCEPTION
  WHEN raise_exception THEN RAISE NOTICE 'FAIL BD-30c rebaixamento rejeitado: %', SQLERRM;
  WHEN SQLSTATE 'LJ000' THEN NULL;
END $$;
