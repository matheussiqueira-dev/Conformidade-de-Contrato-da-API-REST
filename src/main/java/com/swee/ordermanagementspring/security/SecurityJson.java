package com.swee.ordermanagementspring.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

public final class SecurityJson {
    private SecurityJson() {}
    // Only fixed server messages enter this function; no request text is interpolated.
    public static void error(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        String error = status == 401 ? "Unauthorized" : "Forbidden";
        String message = status == 401 ? "Autenticacao necessaria ou credenciais invalidas" : "Acesso negado ou token CSRF invalido";
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status
                + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\",\"details\":null}");
    }
}
