-- =====================================================================
-- Carga da massa de demonstracao Olist (historico de clientes) por SQL puro, com catalogo so de produtos Apple.
-- Executar como superusuario do banco demo, DEPOIS do Flyway (perfil demo), a partir desta pasta:
--   psql -U <superusuario> -d <banco> -v ON_ERROR_STOP=1 -f 07_carga_olist.sql
-- Fonte: Brazilian E-Commerce Public Dataset by Olist (CC BY-NC-SA 4.0); adaptacoes em olist/NOTICE.md.
-- Idempotente: apaga a carga anterior (SKU OL-%, clientes da massa e dependentes) antes de inserir.
-- Datas relativas a data da carga (dias_atras): o historico nunca envelhece.
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;

-- Autoria pelo mecanismo do sistema: app_current_user_id() le app.user_id (gerente da demonstracao).
-- As contas da V6_1 sao achadas pelo db_role, que nao muda quando o ambiente publicado renomeia nome e e-mail.
SELECT set_config('app.user_id', (SELECT id::text FROM app_users WHERE db_role = 'gerente_matheus'), true);
DO $$ BEGIN
  IF app_current_user_id() IS NULL THEN RAISE EXCEPTION 'carga Olist: conta gerente_matheus inexistente (rode o Flyway no perfil demo)'; END IF;
END $$;

-- ---------------------------------------------------------------------
-- Staging com os mesmos campos dos CSVs
-- ---------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS stg_olist;
DROP TABLE IF EXISTS stg_olist.itens_pedido, stg_olist.pedidos, stg_olist.produtos, stg_olist.clientes, stg_olist.categoria_pt,
                     stg_olist.categoria_apple, stg_olist.categoria_antiga;
CREATE TABLE stg_olist.clientes (codigo text PRIMARY KEY, tipo text, nome text, cpf text, cnpj text, email text,
                                 cidade text, uf text, cep_prefixo text);
CREATE TABLE stg_olist.produtos (sku text PRIMARY KEY, nome text, categoria text, preco numeric(12,2),
                                 custo_medio_demo numeric(12,2), peso_g integer);
CREATE TABLE stg_olist.pedidos (codigo text PRIMARY KEY, cliente text, vendedor text, dias_atras integer, hora time, status text,
                                forma_pagamento text, parcelas integer, cartao_ultimos4 text, entrega text, origem_olist text);
CREATE TABLE stg_olist.itens_pedido (pedido text, sku text, quantidade integer, preco_unitario numeric(12,2));

\copy stg_olist.clientes FROM 'olist/clientes.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')
\copy stg_olist.produtos FROM 'olist/produtos.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')
\copy stg_olist.pedidos FROM 'olist/pedidos.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')
\copy stg_olist.itens_pedido FROM 'olist/itens_pedido.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

-- Categorias do catalogo Apple. Produto em categoria fora desta lista faz a carga falhar: so produto Apple entra
-- no catalogo. Smartphones, Audio, Wearables e Acessorios sao as categorias do seed demo, reaproveitadas (D-PUB-12).
CREATE TABLE stg_olist.categoria_apple (nome text PRIMARY KEY);
INSERT INTO stg_olist.categoria_apple(nome) VALUES
  ('Smartphones'), ('Tablets'), ('Computadores'), ('Audio'), ('Wearables'), ('Casa inteligente'), ('Acessorios');
DO $$
DECLARE v_fora text;
BEGIN
  SELECT string_agg(DISTINCT p.categoria, ', ') INTO v_fora
    FROM stg_olist.produtos p LEFT JOIN stg_olist.categoria_apple a ON a.nome = p.categoria
   WHERE a.nome IS NULL;
  IF v_fora IS NOT NULL THEN RAISE EXCEPTION 'carga Olist: categoria fora do catalogo Apple: %', v_fora; END IF;
END $$;

-- Categorias do catalogo diverso das cargas anteriores (nome original sem acento e nome em portugues), so para a
-- limpeza apagar as que ficarem vazias num banco que ja recebeu a carga antiga, como o Neon publicado.
CREATE TABLE stg_olist.categoria_antiga (origem text PRIMARY KEY, nome text NOT NULL UNIQUE);
INSERT INTO stg_olist.categoria_antiga(origem, nome) VALUES
  ('Alimentos', 'Alimentos'),
  ('Alimentos bebidas', 'Alimentos e bebidas'),
  ('Artes e artesanato', 'Artes e artesanato'),
  ('Automotivo', 'Automotivo'),
  ('Bebes', 'Bebês'),
  ('Bebidas', 'Bebidas'),
  ('Beleza saude', 'Beleza e saúde'),
  ('Brinquedos', 'Brinquedos'),
  ('Cama mesa banho', 'Cama, mesa e banho'),
  ('Casa construcao', 'Casa e construção'),
  ('Climatizacao', 'Climatização'),
  ('Consoles games', 'Consoles e games'),
  ('Construcao ferramentas construcao', 'Construção e ferramentas: construção'),
  ('Construcao ferramentas ferramentas', 'Construção e ferramentas: ferramentas'),
  ('Cool stuff', 'Cool stuff'),
  ('Dvds blu ray', 'DVDs e Blu-ray'),
  ('Eletrodomesticos', 'Eletrodomésticos'),
  ('Eletrodomesticos 2', 'Eletrodomésticos 2'),
  ('Esporte lazer', 'Esporte e lazer'),
  ('Fashion bolsas e acessorios', 'Moda: bolsas e acessórios'),
  ('Fashion roupa feminina', 'Moda: roupa feminina'),
  ('Ferramentas jardim', 'Ferramentas de jardim'),
  ('Informatica acessorios', 'Informática e acessórios'),
  ('La cuisine', 'La Cuisine'),
  ('Malas acessorios', 'Malas e acessórios'),
  ('Moveis decoracao', 'Móveis e decoração'),
  ('Moveis quarto', 'Móveis de quarto'),
  ('Moveis sala', 'Móveis de sala'),
  ('Papelaria', 'Papelaria'),
  ('Pc gamer', 'PC gamer'),
  ('Perfumaria', 'Perfumaria'),
  ('Pet shop', 'Pet shop'),
  ('Relogios presentes', 'Relógios e presentes'),
  ('Tablets impressao imagem', 'Tablets, impressão e imagem'),
  ('Telefonia', 'Telefonia'),
  ('Utilidades domesticas', 'Utilidades domésticas');

-- ---------------------------------------------------------------------
-- Limpeza idempotente da carga anterior, na ordem das FKs
-- ---------------------------------------------------------------------
CREATE TEMP TABLE alvo_cliente ON COMMIT DROP AS
SELECT id, address_id FROM customer WHERE email IN (SELECT email FROM stg_olist.clientes);
CREATE TEMP TABLE alvo_produto ON COMMIT DROP AS SELECT id FROM product WHERE sku LIKE 'OL-%';
CREATE TEMP TABLE alvo_pedido ON COMMIT DROP AS
SELECT id, address_id FROM orders
 WHERE customer_id IN (SELECT id FROM alvo_cliente)
    OR id IN (SELECT order_id FROM order_item WHERE product_id IN (SELECT id FROM alvo_produto));

DELETE FROM stock_movement WHERE order_id IN (SELECT id FROM alvo_pedido) OR product_id IN (SELECT id FROM alvo_produto);
DELETE FROM refund WHERE payment_id IN (SELECT id FROM payment WHERE order_id IN (SELECT id FROM alvo_pedido));
DELETE FROM payment WHERE order_id IN (SELECT id FROM alvo_pedido);
DELETE FROM order_item WHERE order_id IN (SELECT id FROM alvo_pedido);
DELETE FROM orders WHERE id IN (SELECT id FROM alvo_pedido);
DELETE FROM address WHERE id IN (SELECT address_id FROM alvo_pedido WHERE address_id IS NOT NULL);
DELETE FROM promotion WHERE product_id IN (SELECT id FROM alvo_produto);
DELETE FROM product_price_history WHERE product_id IN (SELECT id FROM alvo_produto);
DELETE FROM stock_entry WHERE product_id IN (SELECT id FROM alvo_produto);
DELETE FROM stock_balance WHERE product_id IN (SELECT id FROM alvo_produto);
DELETE FROM product WHERE id IN (SELECT id FROM alvo_produto);
DELETE FROM customer WHERE id IN (SELECT id FROM alvo_cliente);
DELETE FROM address WHERE id IN (SELECT address_id FROM alvo_cliente WHERE address_id IS NOT NULL);
-- Categorias da massa sem produto nem promocao: as do catalogo Apple e as do catalogo diverso de cargas anteriores.
DELETE FROM category c
 WHERE (c.name IN (SELECT nome FROM stg_olist.categoria_apple)
        OR c.name IN (SELECT origem FROM stg_olist.categoria_antiga) OR c.name IN (SELECT nome FROM stg_olist.categoria_antiga))
   AND NOT EXISTS (SELECT 1 FROM product p WHERE p.category_id = c.id)
   AND NOT EXISTS (SELECT 1 FROM promotion pr WHERE pr.category_id = c.id);

-- ---------------------------------------------------------------------
-- Catalogo: categorias, produtos (o gatilho cria stock_balance) e entradas (o gatilho calcula o custo medio)
-- ---------------------------------------------------------------------
INSERT INTO category(name)
SELECT DISTINCT categoria FROM stg_olist.produtos
ON CONFLICT (name) DO NOTHING;

-- Nome e preco do catalogo Apple ficticio do CSV; o SKU mantem o prefixo OL- da carga.
INSERT INTO product(product_type, name, price, description, weight, sku, category_id)
SELECT 'PHYSICAL', p.nome, p.preco, 'Preco ficticio (massa de demonstracao)', p.peso_g / 1000.0, p.sku, c.id
  FROM stg_olist.produtos p
  JOIN category c ON c.name = p.categoria
 ORDER BY p.sku;

-- Entrada = quantidade vendida + saldo restante (5 a 20, deterministico pelo SKU), datada antes do primeiro pedido.
INSERT INTO stock_entry(product_id, quantity, unit_cost, created_at, note)
SELECT pr.id, coalesce(v.vendido, 0) + 5 + abs(hashtext(p.sku)) % 16, p.custo_medio_demo,
       now() - interval '370 days', 'Carga Olist de demonstracao'
  FROM stg_olist.produtos p
  JOIN product pr ON pr.sku = p.sku
  LEFT JOIN (SELECT sku, sum(quantidade) AS vendido FROM stg_olist.itens_pedido GROUP BY sku) v ON v.sku = p.sku
 ORDER BY pr.id;
UPDATE stock_balance b SET min_level = 3 FROM product p WHERE p.id = b.product_id AND p.sku LIKE 'OL-%';

-- ---------------------------------------------------------------------
-- Clientes (endereco proprio) e pedidos pagos com itens, pagamento e baixa de estoque
-- ---------------------------------------------------------------------
CREATE TEMP TABLE mapa_cliente (codigo text PRIMARY KEY, customer_id bigint NOT NULL) ON COMMIT DROP;

DO $$
DECLARE
    c record; p record; v_address bigint; v_customer bigint; v_order bigint; v_moment timestamp; v_total numeric;
    v_hoje timestamp := (now() AT TIME ZONE 'America/Fortaleza');
    v_seller bigint;
BEGIN
    FOR c IN SELECT s.*, (SELECT max(dias_atras) FROM stg_olist.pedidos WHERE cliente = s.codigo) AS mais_antigo
               FROM stg_olist.clientes s ORDER BY s.codigo LOOP
        INSERT INTO address(street, number, neighborhood, city, state, zip_code)
        VALUES ('Rua de Demonstracao', (100 + abs(hashtext(c.codigo)) % 900)::text, 'Centro', c.cidade, c.uf, lpad(c.cep_prefixo, 5, '0') || '000')
        RETURNING id INTO v_address;
        INSERT INTO customer(customer_type, name, company_name, cpf, cnpj, email, phone, address_id, created_at, updated_at)
        VALUES (c.tipo, c.nome, CASE WHEN c.tipo = 'CORPORATE' THEN c.nome END, nullif(c.cpf, ''), nullif(c.cnpj, ''), c.email,
                '859' || lpad((abs(hashtext(c.codigo || 'fone')) % 100000000)::text, 8, '0'), v_address,
                now() - make_interval(days => coalesce(c.mais_antigo, 0) + 30), now() - make_interval(days => coalesce(c.mais_antigo, 0) + 30))
        RETURNING id INTO v_customer;
        INSERT INTO mapa_cliente VALUES (c.codigo, v_customer);
    END LOOP;

    FOR p IN SELECT * FROM stg_olist.pedidos ORDER BY codigo LOOP
        v_moment := least(v_hoje::date - p.dias_atras + p.hora, v_hoje - interval '1 minute');
        v_seller := (SELECT id FROM app_users
                      WHERE db_role = CASE p.vendedor WHEN 'VENDEDOR_1' THEN 'vendedor_allan'
                                                      WHEN 'VENDEDOR_2' THEN 'vendedor_francisco'
                                                      ELSE 'gerente_gabriel' END);
        IF v_seller IS NULL THEN RAISE EXCEPTION 'carga Olist: vendedor % sem conta de demonstracao', p.vendedor; END IF;
        v_address := NULL;
        IF p.entrega = 'DELIVERY' THEN
            INSERT INTO address(street, number, neighborhood, city, state, zip_code)
            SELECT a.street, a.number, a.neighborhood, a.city, a.state, a.zip_code
              FROM customer cu JOIN address a ON a.id = cu.address_id
             WHERE cu.id = (SELECT customer_id FROM mapa_cliente WHERE codigo = p.cliente)
            RETURNING id INTO v_address;
        END IF;
        INSERT INTO orders(moment, status, customer_id, seller_id, fulfillment, address_id)
        VALUES (v_moment, 'PAID', (SELECT customer_id FROM mapa_cliente WHERE codigo = p.cliente), v_seller, p.entrega, v_address)
        RETURNING id INTO v_order;

        INSERT INTO order_item(quantity, price, order_id, product_id, unit_cost)
        SELECT i.quantidade, i.preco_unitario, v_order, pr.id, b.avg_cost
          FROM stg_olist.itens_pedido i
          JOIN product pr ON pr.sku = i.sku
          JOIN stock_balance b ON b.product_id = pr.id
         WHERE i.pedido = p.codigo
         ORDER BY pr.id;

        SELECT sum(price * quantity) INTO v_total FROM order_item WHERE order_id = v_order;
        INSERT INTO payment(payment_type, amount, status, payment_date, order_id, due_date, installments, card_last4)
        VALUES (p.forma_pagamento, v_total, 'APPROVED', least(v_moment + interval '15 minutes', v_hoje), v_order,
                CASE WHEN p.forma_pagamento = 'BOLETO' THEN v_moment::date + 3 END,
                CASE WHEN p.forma_pagamento = 'CARD' THEN p.parcelas END,
                CASE WHEN p.forma_pagamento = 'CARD' THEN nullif(p.cartao_ultimos4, '') END);

        -- Saida na confirmacao do pagamento, em ordem crescente de product_id (regra 3).
        INSERT INTO stock_movement(product_id, order_id, kind, quantity, created_by, created_at)
        SELECT oi.product_id, v_order, 'SAIDA', oi.quantity, v_seller, (v_moment AT TIME ZONE 'America/Fortaleza')
          FROM order_item oi WHERE oi.order_id = v_order ORDER BY oi.product_id;
        UPDATE stock_balance b SET on_hand = b.on_hand - s.q, updated_at = now()
          FROM (SELECT product_id, sum(quantity) AS q FROM order_item WHERE order_id = v_order GROUP BY product_id) s
         WHERE b.product_id = s.product_id;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------
-- Conferencia (a transacao so confirma se tudo bater)
-- ---------------------------------------------------------------------
DO $$
DECLARE v_clientes int; v_produtos int; v_categorias int; v_pedidos int; v_itens int; v_soma numeric;
BEGIN
    SELECT count(*) INTO v_clientes FROM customer WHERE email IN (SELECT email FROM stg_olist.clientes);
    SELECT count(*) INTO v_produtos FROM product WHERE sku LIKE 'OL-%';
    SELECT count(DISTINCT category_id) INTO v_categorias FROM product WHERE sku LIKE 'OL-%';
    SELECT count(*) INTO v_pedidos FROM orders o JOIN customer c ON c.id = o.customer_id WHERE c.email IN (SELECT email FROM stg_olist.clientes);
    SELECT count(*), sum(oi.price * oi.quantity) INTO v_itens, v_soma
      FROM order_item oi JOIN product p ON p.id = oi.product_id WHERE p.sku LIKE 'OL-%';
    RAISE NOTICE 'carga Olist: clientes=% produtos=% categorias=% pedidos=% itens=% soma=%',
                 v_clientes, v_produtos, v_categorias, v_pedidos, v_itens, v_soma;
    IF (v_clientes, v_produtos, v_categorias, v_pedidos, v_itens) <> (60, 43, 7, 219, 237) OR v_soma <> 442714.00 THEN
        RAISE EXCEPTION 'carga Olist: conferencia falhou';
    END IF;
END $$;

COMMIT;
