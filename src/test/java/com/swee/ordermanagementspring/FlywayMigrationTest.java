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
    private static final String PASSWORD = "local_test_only";

    private String schema() {
        return "flyway_test_" + UUID.randomUUID().toString().replace("-", "");
    }

    private Connection connect(String schema) throws Exception {
        return DriverManager.getConnection(URL + "?currentSchema=" + schema, USER, PASSWORD);
    }

    private Flyway flyway(String schema, String target) {
        var config = Flyway.configure().dataSource(URL + "?currentSchema=" + schema, USER, PASSWORD)
                .schemas(schema).baselineOnMigrate(false);
        if (target != null) config.target(MigrationVersion.fromVersion(target));
        return config.load();
    }

    @Test
    void emptyDatabaseMigratesThroughV3AndRejectsUnknownSeller() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                Flyway flyway = flyway(schema, null);
                assertThat(flyway.migrate().migrationsExecuted).isEqualTo(3);
                flyway.validate();
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    sql.executeUpdate("INSERT INTO orders (status, seller_id) VALUES ('PENDING_PAYMENT', NULL)");
                    assertThatThrownBy(() -> sql.executeUpdate(
                            "INSERT INTO orders (status, seller_id) VALUES ('PENDING_PAYMENT', 999999)"))
                            .hasMessageContaining("fk_orders_seller");
                    try (ResultSet rows = sql.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success")) {
                        rows.next();
                        assertThat(rows.getInt(1)).isEqualTo(3);
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
        try (Connection admin = DriverManager.getConnection(URL, USER, PASSWORD);
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
                assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(2);
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
    void v3ConvertsLegacyDoubleMoneyToNumericHalfUp() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                assertThat(flyway(schema, "2").migrate().migrationsExecuted).isEqualTo(2);
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    // Massa sintetica com valores tipicos de double: meio centavo e soma 0.1 + 0.2
                    sql.executeUpdate("INSERT INTO product (id, product_type, name, price) VALUES (1, 'DIGITAL', 'E-book', 1.005)");
                    sql.executeUpdate("INSERT INTO orders (id, status) VALUES (7, 'PENDING_PAYMENT')");
                    sql.executeUpdate("INSERT INTO order_item (quantity, price, order_id, product_id) VALUES (1, 0.1 + 0.2, 7, 1)");
                    sql.executeUpdate("INSERT INTO payment (payment_type, amount, status, order_id) VALUES ('PIX', 1.004, 'PENDING', 7)");
                }
                Flyway flyway = flyway(schema, null);
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

    @Test
    void v3FailsInsteadOfTruncatingValuesAboveColumnLimit() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                flyway(schema, "2").migrate();
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    sql.executeUpdate("INSERT INTO product (product_type, name, price) VALUES ('DIGITAL', 'Caro', 10000000000.00)");
                }
                assertThatThrownBy(() -> flyway(schema, null).migrate()).hasStackTraceContaining("numeric field overflow");
            } finally {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
