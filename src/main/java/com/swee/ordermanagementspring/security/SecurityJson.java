package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.entities.StoreTime;
import com.swee.ordermanagementspring.exceptions.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.OffsetDateTime;

/** Erros escritos pelos filtros de seguranca (antes do MVC), no mesmo formato do ErrorResponse. */
public final class SecurityJson {
    private SecurityJson() {}

    // Only fixed server messages enter this function; no request text is interpolated.
    public static void error(HttpServletResponse response, int status) throws IOException {
        switch (status) {
            case 401 -> write(response, 401, "Unauthorized", ErrorCode.UNAUTHENTICATED,
                    "Autenticacao necessaria ou credenciais invalidas");
            case 400 -> write(response, 400, "Bad Request", ErrorCode.MALFORMED_REQUEST, "Requisicao rejeitada");
            default -> write(response, 403, "Forbidden", ErrorCode.FORBIDDEN, "Acesso negado ou token CSRF invalido");
        }
    }

    private static void write(HttpServletResponse response, int status, String error, ErrorCode code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"timestamp\":\"" + OffsetDateTime.now(StoreTime.clock()) + "\",\"status\":" + status
                + ",\"error\":\"" + error + "\",\"code\":\"" + code.name() + "\",\"message\":\"" + message
                + "\",\"details\":null}");
    }
}
