package com.swee.ordermanagementspring.config;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Tabelas de negocio que podem ser esvaziadas em banco descartavel (testes e runner de contrato).
 * Nunca inclui role nem flyway_schema_history: elas sao semeadas pelas migracoes. discount_policy (V11) tambem e
 * semeada pela migracao, mas referencia app_users (updated_by) e o CASCADE a esvaziaria: as alcadas sao guardadas
 * antes e recolocadas depois, sem autor.
 */
public final class BusinessTables {

    public static final List<String> NAMES = List.of(
            "audit_event", "refund", "stock_movement", "stock_entry", "stock_balance", "seller_goal", "monthly_goal",
            "payment", "order_item", "promotion", "orders", "product_price_history", "product", "category",
            "customer", "address", "app_users");

    private BusinessTables() {
    }

    public static void truncate(JdbcTemplate jdbc) {
        var policies = jdbc.queryForList("SELECT role, max_percent, allow_below_cost FROM discount_policy");
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", NAMES) + " RESTART IDENTITY CASCADE");
        for (var policy : policies) {
            jdbc.update("INSERT INTO discount_policy(role, max_percent, allow_below_cost, updated_by) VALUES (?, ?, ?, NULL)",
                    policy.get("role"), policy.get("max_percent"), policy.get("allow_below_cost"));
        }
    }
}
