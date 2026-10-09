package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.entities.Money;
import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.exceptions.PaymentException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/** Regras de pagamento compartilhadas por OrderService e PaymentService (FIX-01). */
final class PaymentRules {

    private PaymentRules() {
    }

    /** D016: conversao independente do locale padrao da JVM (ex.: tr-TR transforma "pix" em "PİX"). */
    static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new PaymentException("Payment type is required");
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    /** D012: o valor pago deve ser exatamente o total do pedido, comparado em centavos. */
    static void requireAmountMatchesTotal(BigDecimal amount, Order order) {
        if (amount == null) {
            throw new PaymentException("Payment amount is required");
        }
        BigDecimal paid = Money.of(amount);
        BigDecimal total = Money.of(order.total());
        if (paid.compareTo(total) != 0) {
            throw new PaymentException("Payment amount " + paid + " must equal the order total " + total);
        }
    }

    /** D006: boleto sem vencimento e rejeitado antes de salvar. */
    static void requireDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new PaymentException("dueDate is required for BOLETO payments");
        }
    }
}
