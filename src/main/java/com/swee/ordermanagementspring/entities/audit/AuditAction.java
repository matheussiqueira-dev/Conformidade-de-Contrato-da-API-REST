package com.swee.ordermanagementspring.entities.audit;

/** Acoes aceitas por ck_audit_event_action (V5). */
public enum AuditAction {
    LOGIN_SUCCEEDED, LOGIN_FAILED, ACCOUNT_LOCKED, LOGOUT, CUSTOMER_CREATED, CUSTOMER_UPDATED,
    CUSTOMER_DELETED, STOCK_ADJUSTED, PRODUCT_STATUS_CHANGED, ORDER_CANCELED
}
