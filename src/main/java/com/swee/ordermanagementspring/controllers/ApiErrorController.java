package com.swee.ordermanagementspring.controllers;

import com.swee.ordermanagementspring.exceptions.ErrorCode;
import com.swee.ordermanagementspring.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /error no formato unico (A5) para falhas que acontecem fora do MVC. Liberado para qualquer metodo pelo
 * dispatcher ERROR; nunca expoe a excecao original.
 */
@Hidden
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        Object attribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        // Chamada direta, sem erro encaminhado pelo container: nao ha o que relatar.
        HttpStatus status = HttpStatus.resolve(attribute instanceof Integer code ? code : 404);
        if (status == null || !status.isError()) status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorCode code = switch (status.value()) {
            case 401 -> ErrorCode.UNAUTHENTICATED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 409 -> ErrorCode.STATE_CONFLICT;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.MALFORMED_REQUEST;
        };
        String message = status.is5xxServerError() ? "Occurred an unexpected error" : status.getReasonPhrase();
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), status.getReasonPhrase(), code, message));
    }
}
