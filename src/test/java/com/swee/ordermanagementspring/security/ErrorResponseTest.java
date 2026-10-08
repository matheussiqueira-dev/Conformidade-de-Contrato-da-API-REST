package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.exceptions.GlobalExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {
    @Test void staleSessionReturns403WithoutLeakingAccountDetails() {
        var response = new GlobalExceptionHandler().handleAccessDeniedException(
                new AccessDeniedException("Authenticated user no longer exists"));
        assertEquals(403, response.getStatusCode().value());
        assertEquals(403, response.getBody().getStatus());
        assertFalse(response.getBody().getMessage().contains("no longer exists"));
    }

    @Test void genericErrorBodyMatchesHttpStatusAndDoesNotLeakException() {
        var response = new GlobalExceptionHandler().handleGenericException(new IllegalStateException("private database details"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals(500, response.getBody().getStatus());
        assertFalse(response.getBody().getMessage().contains("private database"));
    }
}
