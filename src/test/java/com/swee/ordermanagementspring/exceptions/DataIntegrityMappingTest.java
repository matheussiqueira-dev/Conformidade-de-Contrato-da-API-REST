package com.swee.ordermanagementspring.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/** Ultima barreira: violacao no banco vira 409 com code, nunca 500, e sem SQL na resposta. */
class DataIntegrityMappingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static DataIntegrityViolationException violation(String state, String constraint) {
        SQLException sql = new SQLException("ERROR: violates constraint \"" + constraint + "\" on table x", state);
        return new DataIntegrityViolationException("could not execute statement", sql);
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
            "23514, ck_stock_reserved_le_on_hand, STOCK_INSUFFICIENT",
            "23514, stock_balance_on_hand_check,  STOCK_INSUFFICIENT",
            "23514, ck_orders_delivery_address,   STATE_CONFLICT",
            "23505, client_cpf_key,               DUPLICATE_DOCUMENT",
            "23505, client_cnpj_key,              DUPLICATE_DOCUMENT",
            "23505, uq_orders_seller_request,     IDEMPOTENCY_CONFLICT",
            "23505, payment_order_id_key,         STATE_CONFLICT",
            "23503, fk_orders_customer,           RESOURCE_IN_USE"
    })
    void databaseViolationsBecome409WithCode(String state, String constraint, ErrorCode code) {
        var response = handler.handleDataIntegrityViolation(violation(state, constraint));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getCode()).isEqualTo(code);
        assertThat(response.getBody().getMessage()).doesNotContain(constraint).doesNotContain("ERROR");
    }

    @Test
    @DisplayName("Legado: segundo pagamento do mesmo pedido mantem a mensagem conhecida")
    void legacyDuplicatePaymentKeepsMessage() {
        var response = handler.handleDataIntegrityViolation(violation("23505", "payment_order_id_key"));
        assertThat(response.getBody().getMessage()).isEqualTo("This order already has a payment.");
    }

    @Test
    @DisplayName("Lock otimista -> 409 CONCURRENT_UPDATE")
    void optimisticLockIsConcurrentUpdate() {
        var response = handler.handleOptimisticLock(new ObjectOptimisticLockingFailureException(Object.class, 1L));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.CONCURRENT_UPDATE);
    }
}
