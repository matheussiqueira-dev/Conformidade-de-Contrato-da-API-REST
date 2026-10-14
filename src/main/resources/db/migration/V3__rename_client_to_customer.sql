-- Renomeia client para customer (entidade Customer, coluna orders.customer_id).
-- So renomeia: dados, ids, constraints e chaves estrangeiras sao preservados.
ALTER TABLE client RENAME TO customer;
ALTER TABLE customer RENAME COLUMN client_type TO customer_type;
ALTER TABLE orders RENAME COLUMN client_id TO customer_id;
