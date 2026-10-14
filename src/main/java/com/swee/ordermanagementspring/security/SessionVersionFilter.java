package com.swee.ordermanagementspring.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * G-01: a sessao guarda o papel do momento do login. A cada requisicao autenticada por sessao, compara a versao
 * da conta no banco com a do login; conta inativa, apagada ou com versao diferente (papel, situacao, senha ou
 * desbloqueio alterados) encerra a sessao com 401 SESSION_REVOKED. Enquanto a senha temporaria nao for trocada,
 * so as rotas da propria conta respondem; as demais recebem 403 PASSWORD_CHANGE_REQUIRED.
 * Esta API so autentica por sessao (nao ha outro mecanismo de login): sem sessao, nao ha o que revogar.
 */
public class SessionVersionFilter extends OncePerRequestFilter {
    private static final Set<String> ACCOUNT_ROUTES = Set.of(
            "GET /auth/me", "GET /auth/csrf", "PUT /auth/password", "POST /auth/logout");

    private final SessionStateReader states;

    public SessionVersionFilter(SessionStateReader states) {
        this.states = states;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (session == null || authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            chain.doFilter(request, response);
            return;
        }
        boolean current = states.find(principal.id())
                .filter(SessionStateReader.SessionState::active)
                .filter(state -> state.sessionVersion() == principal.sessionVersion())
                .isPresent();
        if (!current) {
            session.invalidate();
            SecurityContextHolder.clearContext();
            SecurityJson.sessionRevoked(response);
            return;
        }
        if (principal.mustChangePassword() && !isAccountRoute(request)) {
            SecurityJson.passwordChangeRequired(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isAccountRoute(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/error") || ACCOUNT_ROUTES.contains(request.getMethod() + " " + path);
    }
}
