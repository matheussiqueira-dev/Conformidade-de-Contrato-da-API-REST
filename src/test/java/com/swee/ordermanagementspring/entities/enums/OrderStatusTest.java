package com.swee.ordermanagementspring.entities.enums;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** [D010, D10] Tabela de decisao completa das transicoes de status (6 x 6 = 36 combinacoes). */
class OrderStatusTest {

    @ParameterizedTest(name = "[D010] {0} -> {1} permitido = {2}")
    @CsvSource({
            "PENDING_PAYMENT, PENDING_PAYMENT, true",
            "PENDING_PAYMENT, PROCESSING,      true",
            "PENDING_PAYMENT, PAID,            true",
            "PENDING_PAYMENT, SHIPPED,         true",
            "PENDING_PAYMENT, DELIVERED,       true",
            "PENDING_PAYMENT, CANCELED,        true",
            "PROCESSING,      PENDING_PAYMENT, false",
            "PROCESSING,      PROCESSING,      true",
            "PROCESSING,      PAID,            true",
            "PROCESSING,      SHIPPED,         true",
            "PROCESSING,      DELIVERED,       true",
            "PROCESSING,      CANCELED,        false",
            "PAID,            PENDING_PAYMENT, false",
            "PAID,            PROCESSING,      false",
            "PAID,            PAID,            true",
            "PAID,            SHIPPED,         true",
            "PAID,            DELIVERED,       true",
            "PAID,            CANCELED,        false",
            "SHIPPED,         PENDING_PAYMENT, false",
            "SHIPPED,         PROCESSING,      false",
            "SHIPPED,         PAID,            false",
            "SHIPPED,         SHIPPED,         true",
            "SHIPPED,         DELIVERED,       true",
            "SHIPPED,         CANCELED,        false",
            "DELIVERED,       PENDING_PAYMENT, false",
            "DELIVERED,       PROCESSING,      false",
            "DELIVERED,       PAID,            false",
            "DELIVERED,       SHIPPED,         false",
            "DELIVERED,       DELIVERED,       true",
            "DELIVERED,       CANCELED,        false",
            "CANCELED,        PENDING_PAYMENT, false",
            "CANCELED,        PROCESSING,      false",
            "CANCELED,        PAID,            false",
            "CANCELED,        SHIPPED,         false",
            "CANCELED,        DELIVERED,       false",
            "CANCELED,        CANCELED,        true"
    })
    @Tag("regression")
    void transitionTable(OrderStatus current, OrderStatus next, boolean allowed) {
        assertThat(current.canMoveTo(next)).isEqualTo(allowed);
    }
}
