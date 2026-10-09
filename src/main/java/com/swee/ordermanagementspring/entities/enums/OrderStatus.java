package com.swee.ordermanagementspring.entities.enums;

/** A ordem das constantes e o fluxo do pedido: canMoveTo depende dela. */
public enum OrderStatus {

    PENDING_PAYMENT,
    PROCESSING,
    PAID,
    SHIPPED,
    DELIVERED;

    /** D010: o pedido so avanca no fluxo (ou repete o status atual); DELIVERED e final. */
    public boolean canMoveTo(OrderStatus next) {
        return next.ordinal() >= ordinal();
    }
}
