-- DOM-02: dinheiro passa de double precision para NUMERIC(12,2) (decisao DOM-01: 2 casas, HALF_UP).
-- round(numeric, 2) do PostgreSQL arredonda metade para longe do zero, igual a RoundingMode.HALF_UP.
-- Valor acima de 9999999999.99 faz a migracao falhar (numeric field overflow) em vez de truncar.
ALTER TABLE product    ALTER COLUMN price  TYPE numeric(12,2) USING round(price::numeric, 2);
ALTER TABLE order_item ALTER COLUMN price  TYPE numeric(12,2) USING round(price::numeric, 2);
ALTER TABLE payment    ALTER COLUMN amount TYPE numeric(12,2) USING round(amount::numeric, 2);
