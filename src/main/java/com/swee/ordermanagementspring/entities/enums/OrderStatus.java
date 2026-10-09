package com.swee.ordermanagementspring.entities.enums;

import java.util.EnumSet;
import java.util.Set;

/** Status do pedido; as transicoes permitidas ficam explicitas em {@link #allowedNext()}. */
public enum OrderStatus {

    PENDING_PAYMENT,
    PROCESSING,
    PAID,
    SHIPPED,
    DELIVERED;

    /**
     * D010: tabela de transicoes. O pedido so avanca no fluxo (ou repete o status atual); DELIVERED e final.
     * Fica explicita para que reordenar ou incluir constantes nao mude a regra de negocio sem querer.
     */
    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(PENDING_PAYMENT, PROCESSING, PAID, SHIPPED, DELIVERED);
            case PROCESSING -> EnumSet.of(PROCESSING, PAID, SHIPPED, DELIVERED);
            case PAID -> EnumSet.of(PAID, SHIPPED, DELIVERED);
            case SHIPPED -> EnumSet.of(SHIPPED, DELIVERED);
            case DELIVERED -> EnumSet.of(DELIVERED);
        };
    }

    public boolean canMoveTo(OrderStatus next) {
        return allowedNext().contains(next);
    }
}
