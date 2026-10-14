package com.swee.ordermanagementspring;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
class FlywayMigrationTest {
    private static final String URL = "jdbc:postgresql://localhost:15432/order_management_test";
    private static final String USER = "postgres_test";
    private static final String PASSWORD_VARIABLE = "TEST_DB_PASSWORD";

    private static String password() {
        String value = System.getenv(PASSWORD_VARIABLE);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Defina " + PASSWORD_VARIABLE + " com a senha do PostgreSQL de teste.");
        }
        return value;
    }

    private String schema() {
        return "flyway_test_" + UUID.randomUUID().toString().replace("-", "");
    }

    private Connection connect(String schema) throws Exception {
        return DriverManager.getConnection(URL + "?currentSchema=" + schema, USER, password());
    }

    private Flyway flyway(String schema, String target) {
        var config = Flyway.configure().dataSource(URL + "?currentSchema=" + schema, USER, password())
                .schemas(schema).baselineOnMigrate(false);
        if (target != null) config.target(MigrationVersion.fromVersion(target));
        return config.load();
    }

    @Test
    void emptyDatabaseMigratesThroughV6AndRejectsUnknownSeller() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, password());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                Flyway flyway = flyway(schema, null);
                assertThat(flyway.migrate().migrationsExecuted).isEqualTo(6);
                flyway.validate();
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    sql.executeUpdate("INSERT INTO orders (status, seller_id) VALUES ('PENDING_PAYMENT', NULL)");
                    assertThatThrownBy(() -> sql.executeUpdate(
                            "INSERT INTO orders (status, seller_id) VALUES ('PENDING_PAYMENT', 999999)"))
                            .hasMessageContaining("fk_orders_seller");
                    try (ResultSet rows = sql.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success")) {
                        rows.next();
                        assertThat(rows.getInt(1)).isEqualTo(6);
                    }
                }
            } finally {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void explicitlyBaselinedLegacyDatabaseKeepsOrders() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, password());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                assertThat(flyway(schema, "1").migrate().migrationsExecuted).isEqualTo(1);
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    sql.executeUpdate("INSERT INTO orders (id, status) VALUES (42, 'PENDING_PAYMENT')");
                    sql.execute("DROP TABLE flyway_schema_history");
                }
                // A real legacy database has no Flyway history. Baseline is a deliberate operator step.
                Flyway upgrade = flyway(schema, null);
                upgrade.baseline();
                assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(5);
                upgrade.validate();
                try (Connection database = connect(schema); Statement sql = database.createStatement();
                     ResultSet rows = sql.executeQuery("SELECT id, seller_id FROM orders WHERE id = 42")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong("id")).isEqualTo(42);
                    assertThat(rows.getObject("seller_id")).isNull();
                }
            } finally {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void v4ConvertsLegacyDoubleMoneyToNumericHalfUp() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, password());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                assertThat(flyway(schema, "3").migrate().migrationsExecuted).isEqualTo(3);
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    // Massa sintetica com valores tipicos de double: meio centavo e soma 0.1 + 0.2
                    sql.executeUpdate("INSERT INTO product (id, product_type, name, price) VALUES (1, 'DIGITAL', 'E-book', 1.005)");
                    sql.executeUpdate("INSERT INTO orders (id, status) VALUES (7, 'PENDING_PAYMENT')");
                    sql.executeUpdate("INSERT INTO order_item (quantity, price, order_id, product_id) VALUES (1, 0.1 + 0.2, 7, 1)");
                    sql.executeUpdate("INSERT INTO payment (payment_type, amount, status, order_id) VALUES ('PIX', 1.004, 'PENDING', 7)");
                }
                Flyway flyway = flyway(schema, "4");
                assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
                flyway.validate();
                try (Connection database = connect(schema); Statement sql = database.createStatement();
                     ResultSet rows = sql.executeQuery("SELECT p.price, i.price AS item_price, pay.amount, "
                             + "pg_typeof(p.price)::text AS type FROM product p, order_item i, payment pay")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getBigDecimal("price")).isEqualByComparingTo("1.01");
                    assertThat(rows.getBigDecimal("item_price")).isEqualByComparingTo("0.30");
                    assertThat(rows.getBigDecimal("amount")).isEqualByComparingTo("1.00");
                    assertThat(rows.getString("type")).isEqualTo("numeric");
                }
            } finally {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    /** [D020] 9999999999.994 arredondaria para 9999999999.99 e passaria; a V4 confere o limite antes de arredondar. */
    @Test
    @Tag("regression")
    void v4RejectsLegacyValueJustAboveLimitBeforeRounding() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, password());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                flyway(schema, "3").migrate();
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    sql.executeUpdate("INSERT INTO product (product_type, name, price) VALUES ('DIGITAL', 'Quase no limite', 9999999999.994)");
                }
                assertThatThrownBy(() -> flyway(schema, null).migrate()).hasStackTraceContaining("acima de 9999999999.99");
            } finally {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void v4FailsInsteadOfTruncatingValuesAboveColumnLimit() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, password());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                flyway(schema, "3").migrate();
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    sql.executeUpdate("INSERT INTO product (product_type, name, price) VALUES ('DIGITAL', 'Caro', 10000000000.00)");
                }
                assertThatThrownBy(() -> flyway(schema, null).migrate()).hasStackTraceContaining("acima de 9999999999.99");
            } finally {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    /** Executa o corpo num esquema novo e apaga o esquema no fim. */
    private interface SchemaWork {
        void run(String schema) throws Exception;
    }

    private void inFreshSchema(SchemaWork work) throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, password());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                work.run(schema);
            } finally {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    /** D3/D5: documento legado com mascara vira so digitos; pedido com endereco vira DELIVERY, sem endereco PICKUP. */
    @Test
    void v5NormalizesLegacyDocumentsAndClassifiesFulfillment() throws Exception {
        inFreshSchema(schema -> {
            flyway(schema, "4").migrate();
            try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                sql.executeUpdate("INSERT INTO customer (id, customer_type, name, cpf) VALUES (1, 'INDIVIDUAL', 'PF', '529.982.247-25')");
                sql.executeUpdate("INSERT INTO customer (id, customer_type, name, cnpj, company_name) "
                        + "VALUES (2, 'CORPORATE', 'PJ', '12.345.678/0001-95', 'PJ Ltda')");
                sql.executeUpdate("INSERT INTO address (id, street) VALUES (1, 'Rua A')");
                sql.executeUpdate("INSERT INTO orders (id, status, customer_id, address_id) VALUES (1, 'PAID', 1, 1)");
                sql.executeUpdate("INSERT INTO orders (id, status, customer_id) VALUES (2, 'PENDING_PAYMENT', 2)");
            }
            Flyway flyway = flyway(schema, null);
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
            flyway.validate();
            try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                try (ResultSet rows = sql.executeQuery("SELECT cpf, cnpj FROM customer ORDER BY id")) {
                    rows.next();
                    assertThat(rows.getString("cpf")).isEqualTo("52998224725");
                    rows.next();
                    assertThat(rows.getString("cnpj")).isEqualTo("12345678000195");
                }
                try (ResultSet rows = sql.executeQuery("SELECT fulfillment FROM orders ORDER BY id")) {
                    rows.next();
                    assertThat(rows.getString(1)).isEqualTo("DELIVERY");
                    rows.next();
                    assertThat(rows.getString(1)).isEqualTo("PICKUP");
                }
                assertThatThrownBy(() -> sql.executeUpdate(
                        "INSERT INTO customer (customer_type, name, cpf) VALUES ('INDIVIDUAL', 'Mascara', '111.444.777-35')"))
                        .hasMessageContaining("ck_customer_cpf");
            }
        });
    }

    /** A2A-2: dado legado invalido faz a V5 falhar com mensagem clara; nada e corrigido. */
    @Test
    void v5AbortsWhenDocumentsCollideAfterNormalization() throws Exception {
        inFreshSchema(schema -> {
            flyway(schema, "4").migrate();
            try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                sql.executeUpdate("INSERT INTO customer (customer_type, name, cpf) VALUES ('INDIVIDUAL', 'A', '529.982.247-25')");
                sql.executeUpdate("INSERT INTO customer (customer_type, name, cpf) VALUES ('INDIVIDUAL', 'B', '52998224725')");
            }
            assertThatThrownBy(() -> flyway(schema, null).migrate())
                    .hasStackTraceContaining("documentos colidem apos normalizacao para digitos: 52998224725");
            try (Connection database = connect(schema); Statement sql = database.createStatement();
                 ResultSet rows = sql.executeQuery("SELECT count(*) FROM customer WHERE cpf = '529.982.247-25'")) {
                rows.next();
                assertThat(rows.getInt(1)).isEqualTo(1);
            }
        });
    }

    /**
     * P1/P7: as funcoes do pacote usam o esquema do Flyway no search_path e reservam em ordem de product_id;
     * a reserva acima do disponivel e recusada e os CHECKs impedem saldo negativo (parte de banco do CT-STOCK-109).
     */
    @Test
    void v5FunctionsWorkInTheMigratedSchemaAndChecksBlockNegativeStock() throws Exception {
        inFreshSchema(schema -> {
            flyway(schema, null).migrate();
            try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                sql.executeUpdate("INSERT INTO app_users (id, name, email, password_hash, role) "
                        + "VALUES (1, 'Gerente', 'g@example.test', 'x', 'GERENTE'), (2, 'Vendedora', 'v@example.test', 'x', 'VENDEDOR')");
                sql.executeUpdate("INSERT INTO customer (id, customer_type, name, cpf) VALUES (1, 'INDIVIDUAL', 'Cliente', '52998224725')");
                sql.execute("SELECT set_config('app.user_id', '1', false)");
                sql.executeUpdate("INSERT INTO product (id, product_type, name, price, sku) VALUES "
                        + "(10, 'PHYSICAL', 'Mouse', 100.00, 'MS-1'), (20, 'PHYSICAL', 'Teclado', 200.00, 'TC-1')");
                sql.executeUpdate("INSERT INTO stock_entry (product_id, quantity, unit_cost) VALUES (10, 2, 50.00), (20, 1, 120.00)");
                sql.execute("SELECT set_config('app.user_id', '2', false)");
                long order;
                try (ResultSet rows = sql.executeQuery("SELECT fn_criar_pedido(1, '[{\"product_id\": 20, \"quantity\": 1}, "
                        + "{\"product_id\": 10, \"quantity\": 2}]'::jsonb, 'PIX')")) {
                    rows.next();
                    order = rows.getLong(1);
                }
                try (ResultSet rows = sql.executeQuery("SELECT o.fulfillment, o.seller_id, p.amount, "
                        + "(SELECT array_agg(product_id ORDER BY id)::text FROM stock_movement WHERE order_id = o.id) AS ordem "
                        + "FROM orders o JOIN payment p ON p.order_id = o.id WHERE o.id = " + order)) {
                    rows.next();
                    assertThat(rows.getString("fulfillment")).isEqualTo("PICKUP");
                    assertThat(rows.getLong("seller_id")).isEqualTo(2);
                    assertThat(rows.getBigDecimal("amount")).isEqualByComparingTo("400.00");
                    assertThat(rows.getString("ordem")).isEqualTo("{10,20}");
                }
                assertThatThrownBy(() -> sql.executeQuery("SELECT fn_criar_pedido(1, '[{\"product_id\": 20, \"quantity\": 1}]'::jsonb, 'PIX')"))
                        .hasMessageContaining("estoque insuficiente");
                sql.executeUpdate("INSERT INTO product (id, product_type, name, price, sku) VALUES (30, 'PHYSICAL', 'Sem saldo', 10.00, 'SS-1')");
                assertThatThrownBy(() -> sql.executeUpdate("UPDATE stock_balance SET on_hand = -1 WHERE product_id = 30"))
                        .hasMessageContaining("relation \"stock_balance\" violates check constraint");
                assertThatThrownBy(() -> sql.executeUpdate("UPDATE stock_balance SET on_hand = 1 WHERE product_id = 10"))
                        .hasMessageContaining("ck_stock_reserved_le_on_hand");
            }
        });
    }
}
