package com.swee.ordermanagementspring.config;

import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import com.swee.ordermanagementspring.security.AppPrincipal;
import com.swee.ordermanagementspring.security.AuditService;
import com.swee.ordermanagementspring.security.LockoutAuthenticationProvider;
import com.swee.ordermanagementspring.security.LoginAttemptService;
import com.swee.ordermanagementspring.security.SecurityJson;
import com.swee.ordermanagementspring.security.SessionStateReader;
import com.swee.ordermanagementspring.security.SessionVersionFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import java.util.List;

/**
 * Sessao + CSRF (ARQ-01), hierarquia GERENTE > VENDEDOR nas regras de URL e de metodo, negacao por padrao.
 * As regras espelham src/test/resources/security/access-matrix.csv (docs/decisoes/matriz-acesso.md).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);
    private static final String VENDEDOR = "VENDEDOR";
    private static final String GERENTE = "GERENTE";

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_GERENTE > ROLE_VENDEDOR");
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean public AuthenticationManager authenticationManager(AppUserRepository users, PasswordEncoder encoder,
                                                             LoginAttemptService attempts) {
        return new ProviderManager(new LockoutAuthenticationProvider(users, encoder, attempts));
    }
    @Bean public SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean public CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean public SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
    }

    /** Requisicao barrada pelo StrictHttpFirewall (";", "%2e%2e", "//") responde 400 no formato ErrorResponse. */
    @Bean public RequestRejectedHandler requestRejectedHandler() {
        return (request, response, ex) -> SecurityJson.error(response, 400);
    }

    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
                                                        CsrfTokenRepository csrf, AuditService audit,
                                                        SessionStateReader sessionStates) throws Exception {
        http.securityContext(c -> c.securityContextRepository(contexts))
                // G-01: conta alterada depois do login perde a sessao antes de qualquer outra regra.
                .addFilterAfter(new SessionVersionFilter(sessionStates), SecurityContextHolderFilter.class)
                .csrf(c -> c.csrfTokenRepository(csrf))
                .requestCache(c -> c.requestCache(new NullRequestCache()))
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/csrf", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/auth/password").hasRole(VENDEDOR)
                        // Balcao: VENDEDOR e, pela hierarquia, GERENTE. O dono do pedido e conferido no servico (404).
                        .requestMatchers(HttpMethod.GET, "/sales/catalog", "/sales/orders", "/sales/orders/{id}",
                                "/customers", "/customers/{id}", "/customers/{id}/orders", "/performance/me",
                                "/promotions/active").hasRole(VENDEDOR)
                        .requestMatchers(HttpMethod.POST, "/sales/quote", "/sales/orders", "/sales/orders/{id}/payment",
                                "/orders/{id}/cancel", "/customers").hasRole(VENDEDOR)
                        .requestMatchers(HttpMethod.PUT, "/customers/{id}").hasRole(VENDEDOR)
                        // Backoffice e rotas legadas: somente GERENTE.
                        .requestMatchers(HttpMethod.DELETE, "/customers/{id}").hasRole(GERENTE)
                        .requestMatchers(HttpMethod.POST, "/customers/{id}/address", "/payment/{id}/refund").hasRole(GERENTE)
                        .requestMatchers("/products", "/products/**", "/categories", "/categories/**", "/stock/**",
                                "/promotions", "/promotions/**", "/goals/**", "/management/**", "/analytics/**",
                                "/orders", "/orders/**", "/payment", "/payment/**", "/address", "/address/**",
                                "/users", "/users/**").hasRole(GERENTE)
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> SecurityJson.error(response, 401))
                        .accessDeniedHandler((request, response, ex) -> SecurityJson.error(response, 403)))
                .logout(l -> l.logoutUrl("/auth/logout").deleteCookies("JSESSIONID")
                        .addLogoutHandler((request, response, auth) -> {
                            if (auth != null && auth.getPrincipal() instanceof AppPrincipal principal) {
                                try {
                                    audit.record(AuditAction.LOGOUT, principal.id(), "app_users", principal.id(), null);
                                } catch (RuntimeException ex) {
                                    // A trilha nunca impede o logout; a sessao e invalidada de qualquer forma.
                                    LOG.warn("Logout audit not recorded: {}", ex.getClass().getSimpleName());
                                }
                            }
                        })
                        .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)));
        return http.build();
    }
}
