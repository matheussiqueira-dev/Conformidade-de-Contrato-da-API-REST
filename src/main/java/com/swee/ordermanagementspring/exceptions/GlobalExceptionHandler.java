package com.swee.ordermanagementspring.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formato unico de erro (A5) com code (contrato v4). Nenhuma entrada invalida responde 500; o 500 nunca
 * expoe stack trace, SQL ou nome de classe.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern CONSTRAINT = Pattern.compile("constraint \"([^\"]+)\"");

    static ResponseEntity<ErrorResponse> respond(HttpStatus status, String error, ErrorCode code, String message) {
        return respond(status, error, code, message, null);
    }

    static ResponseEntity<ErrorResponse> respond(HttpStatus status, String error, ErrorCode code, String message,
                                                 List<String> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), error, code, message, details));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Payment Error", ErrorCode.VALIDATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ErrorResponse> handleProductException(ProductException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Product Error", ErrorCode.VALIDATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(CustomerException.class)
    public ResponseEntity<ErrorResponse> handleCustomerException(CustomerException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Customer Error", ErrorCode.VALIDATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrderException(OrderException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Order Error", ErrorCode.VALIDATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return respond(ex.getStatus(), ex.getStatus().getReasonPhrase(), ex.getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).toList();
        List<String> global = ex.getBindingResult().getGlobalErrors().stream().map(e -> e.getDefaultMessage()).toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation Error", ErrorCode.VALIDATION_FAILED,
                "One or more fields are invalid", concat(details, global));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> details = ex.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation Error", ErrorCode.VALIDATION_FAILED,
                "One or more parameters are invalid", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation Error", ErrorCode.VALIDATION_FAILED,
                "One or more fields are invalid", details);
    }

    // DOM-02: corpo invalido (ex.: "price": "abc"), JSON malformado ou propriedade desconhecida e erro do cliente
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Bad Request", ErrorCode.MALFORMED_REQUEST,
                "Request body is malformed, has an unknown property or an invalid value");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Bad Request", ErrorCode.MALFORMED_REQUEST,
                "Parameter '" + ex.getName() + "' has an invalid value");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ErrorResponse> handleMissingParameter(Exception ex) {
        String name = ex instanceof MissingServletRequestParameterException p ? p.getParameterName()
                : ((MissingRequestHeaderException) ex).getHeaderName();
        return respond(HttpStatus.BAD_REQUEST, "Validation Error", ErrorCode.VALIDATION_FAILED,
                "Required parameter '" + name + "' is missing");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "Not Found", ErrorCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "Not Found", ErrorCode.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ResponseEntity<ErrorResponse> body = respond(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                ErrorCode.METHOD_NOT_ALLOWED, "Method " + ex.getMethod() + " is not supported for this resource");
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) headers.setAllow(ex.getSupportedHttpMethods());
        return ResponseEntity.status(body.getStatusCode()).headers(headers).body(body.getBody());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "Content type is not supported; use application/json");
    }

    /** O cliente nao aceita JSON: nao ha corpo que ele consiga ler. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.CONCURRENT_UPDATE,
                "The record was changed by another request; reload and try again");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handlePessimisticLock(PessimisticLockingFailureException ex) {
        return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.CONCURRENT_UPDATE,
                "Concurrent update in progress; try again");
    }

    /** Ultima barreira: CHECK, UNIQUE e FK do banco viram 409 com code, sem SQL na resposta. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String state = sqlState(ex);
        String constraint = constraintName(ex);
        if ("23505".equals(state)) {
            if (constraint.contains("cpf") || constraint.contains("cnpj")) {
                return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.DUPLICATE_DOCUMENT, "Document already registered");
            }
            if (constraint.equals("payment_order_id_key")) {
                return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.STATE_CONFLICT, "This order already has a payment.");
            }
            if (constraint.equals("uq_orders_seller_request")) {
                return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.IDEMPOTENCY_CONFLICT, "Idempotency key already used");
            }
            return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.STATE_CONFLICT, "Duplicate value");
        }
        if ("23503".equals(state)) {
            return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.RESOURCE_IN_USE, "Record is referenced by other records");
        }
        if ("23514".equals(state) && (constraint.startsWith("stock_balance") || constraint.startsWith("ck_stock"))) {
            return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.STOCK_INSUFFICIENT, "Insufficient stock");
        }
        return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.STATE_CONFLICT, "Operation violates a data integrity rule");
    }

    /** Gatilho keep_one_active_manager (V8): P0001 com prefixo LAST_MANAGER vira 409; o resto segue como erro interno. */
    @ExceptionHandler(UncategorizedSQLException.class)
    public ResponseEntity<ErrorResponse> handleUncategorizedSql(UncategorizedSQLException ex) {
        if ("P0001".equals(sqlState(ex)) && String.valueOf(ex.getMessage()).contains("LAST_MANAGER")) {
            return respond(HttpStatus.CONFLICT, "Conflict", ErrorCode.LAST_MANAGER,
                    "The store must keep at least one active manager");
        }
        return handleGenericException(ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return respond(HttpStatus.FORBIDDEN, "Forbidden", ErrorCode.FORBIDDEN, "Acesso negado");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        LOG.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ErrorCode.INTERNAL_ERROR,
                "Occurred an unexpected error");
    }

    private static List<String> concat(List<String> first, List<String> second) {
        if (second.isEmpty()) return first;
        return java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
    }

    private static String sqlState(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null) return sql.getSQLState();
        }
        return "";
    }

    private static String constraintName(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null) {
                Matcher matcher = CONSTRAINT.matcher(cause.getMessage());
                if (matcher.find()) return matcher.group(1).toLowerCase(Locale.ROOT);
            }
        }
        return "";
    }
}
