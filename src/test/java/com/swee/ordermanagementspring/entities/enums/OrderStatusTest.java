package com.swee.ordermanagementspring.entities.enums;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** [D010] Tabela de decisao completa das transicoes de status (5 x 5 = 25 combinacoes). */
class OrderStatusTest {

    @ParameterizedTest(name = "[D010] {0} -> {1} permitido = {2}")
    @CsvSource({
            "PENDING_PAYMENT, PENDING_PAYMENT, true",
            "PENDING_PAYMENT, PROCESSING,      true",
            "PENDING_PAYMENT, PAID,            true",
            "PENDING_PAYMENT, SHIPPED,         true",
            "PENDING_PAYMENT, DELIVERED,       true",
            "PROCESSING,      PENDING_PAYMENT, false",
            "PROCESSING,      PROCESSING,      true",
            "PROCESSING,      PAID,            true",
            "PROCESSING,      SHIPPED,         true",
            "PROCESSING,      DELIVERED,       true",
            "PAID,            PENDING_PAYMENT, false",
            "PAID,            PROCESSING,      false",
            "PAID,            PAID,            true",
            "PAID,            SHIPPED,         true",
            "PAID,            DELIVERED,       true",
            "SHIPPED,         PENDING_PAYMENT, false",
            "SHIPPED,         PROCESSING,      false",
            "SHIPPED,         PAID,            false",
            "SHIPPED,         SHIPPED,         true",
            "SHIPPED,         DELIVERED,       true",
            "DELIVERED,       PENDING_PAYMENT, false",
            "DELIVERED,       PROCESSING,      false",
            "DELIVERED,       PAID,            false",
            "DELIVERED,       SHIPPED,         false",
            "DELIVERED,       DELIVERED,       true"
    })
    @Tag("regression")
    void transitionTable(OrderStatus current, OrderStatus next, boolean allowed) {
        assertThat(current.canMoveTo(next)).isEqualTo(allowed);
    }
}
