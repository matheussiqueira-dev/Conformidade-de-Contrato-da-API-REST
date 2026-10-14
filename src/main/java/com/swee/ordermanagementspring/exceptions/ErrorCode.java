package com.swee.ordermanagementspring.exceptions;

/** Codigo estavel do erro (contrato v4, REC-d); o frontend traduz cada codigo para uma mensagem pt-BR. */
public enum ErrorCode {
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    STOCK_INSUFFICIENT,
    IDEMPOTENCY_CONFLICT,
    STATE_CONFLICT,
    DUPLICATE_DOCUMENT,
    RESOURCE_IN_USE,
    CONCURRENT_UPDATE,
    INTERNAL_ERROR
}
