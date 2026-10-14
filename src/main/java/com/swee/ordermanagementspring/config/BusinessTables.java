package com.swee.ordermanagementspring.config;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Tabelas de negocio que podem ser esvaziadas em banco descartavel (testes e runner de contrato).
 * Nunca inclui role nem flyway_schema_history: elas sao semeadas pelas migracoes.
 */
public final class BusinessTables {

    public static final List<String> NAMES = List.of(
            "audit_event", "refund", "stock_movement", "stock_entry", "stock_balance", "seller_goal", "monthly_goal",
            "payment", "order_item", "promotion", "orders", "product_price_history", "product", "category",
            "customer", "address", "app_users");

    private BusinessTables() {
    }

    public static void truncate(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", NAMES) + " RESTART IDENTITY CASCADE");
    }
}
