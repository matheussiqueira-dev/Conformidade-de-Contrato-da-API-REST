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
    void emptyDatabaseMigratesThroughV2AndRejectsUnknownSeller() throws Exception {
        String schema = schema();
        try (Connection admin = DriverManager.getConnection(URL, USER, password());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            try {
                Flyway flyway = flyway(schema, null);
                assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
                flyway.validate();
                try (Connection database = connect(schema); Statement sql = database.createStatement()) {
                    sql.executeUpdate("INSERT INTO orders (status, seller_id) VALUES ('PENDING_PAYMENT', NULL)");
                    assertThatThrownBy(() -> sql.executeUpdate(
                            "INSERT INTO orders (status, seller_id) VALUES ('PENDING_PAYMENT', 999999)"))
                            .hasMessageContaining("fk_orders_seller");
                    try (ResultSet rows = sql.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success")) {
                        rows.next();
                        assertThat(rows.getInt(1)).isEqualTo(2);
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
                assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
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
}
