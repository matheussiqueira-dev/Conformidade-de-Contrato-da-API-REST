-- DOM-02: dinheiro passa de double precision para NUMERIC(12,2) (decisao DOM-01: 2 casas, HALF_UP).
-- round(numeric, 2) do PostgreSQL arredonda metade para longe do zero, igual a RoundingMode.HALF_UP.
-- D020: o limite e conferido ANTES do arredondamento. Contar so com o overflow do ALTER deixaria 9999999999.994
-- virar 9999999999.99 em silencio. Qualquer valor acima do limite faz a migracao falhar inteira.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM product WHERE abs(price::numeric) > 9999999999.99)
       OR EXISTS (SELECT 1 FROM order_item WHERE abs(price::numeric) > 9999999999.99)
       OR EXISTS (SELECT 1 FROM payment WHERE abs(amount::numeric) > 9999999999.99) THEN
        RAISE EXCEPTION 'V4: valor monetario acima de 9999999999.99 antes do arredondamento; corrija os dados legados antes de migrar';
    END IF;
END $$;

ALTER TABLE product    ALTER COLUMN price  TYPE numeric(12,2) USING round(price::numeric, 2);
ALTER TABLE order_item ALTER COLUMN price  TYPE numeric(12,2) USING round(price::numeric, 2);
ALTER TABLE payment    ALTER COLUMN amount TYPE numeric(12,2) USING round(amount::numeric, 2);
