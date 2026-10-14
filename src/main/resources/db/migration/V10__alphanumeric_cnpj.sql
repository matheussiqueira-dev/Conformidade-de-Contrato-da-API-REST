-- WP-EVO-4 (docs/decisoes/evolucao.md, D-EVO-17): a Receita Federal emite CNPJ alfanumerico para novas
-- inscricoes desde 07/2026 (12 posicoes com digitos ou letras maiusculas + 2 digitos verificadores).
-- O CHECK da V5 so aceitava 14 digitos; os CNPJs numericos existentes continuam validos no CHECK novo.
DO $$
DECLARE v text;
BEGIN
    SELECT string_agg(id::text, ', ') INTO v FROM customer WHERE cnpj IS NOT NULL AND cnpj !~ '^[0-9A-Z]{12}[0-9]{2}$';
    IF v IS NOT NULL THEN RAISE EXCEPTION 'V10: clientes com CNPJ fora do formato (ids: %)', v; END IF;
END $$;

ALTER TABLE customer DROP CONSTRAINT ck_customer_cnpj;
ALTER TABLE customer ADD CONSTRAINT ck_customer_cnpj CHECK (cnpj ~ '^[0-9A-Z]{12}[0-9]{2}$');
