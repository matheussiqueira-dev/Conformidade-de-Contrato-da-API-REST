package com.swee.ordermanagementspring.config;

import com.swee.ordermanagementspring.security.DatabaseUserDetailsService;
import com.swee.ordermanagementspring.security.SecurityJson;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.savedrequest.NullRequestCache;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean public AuthenticationManager authenticationManager(DatabaseUserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean public SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean public CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean public SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
    }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
                                                        CsrfTokenRepository csrf) throws Exception {
        http.securityContext(c -> c.securityContextRepository(contexts))
                .csrf(c -> c.csrfTokenRepository(csrf))
                .requestCache(c -> c.requestCache(new NullRequestCache()))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/auth/csrf", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/logout").permitAll()
                        .requestMatchers("/auth/me").authenticated()
                        // Legacy DTOs contain unrestricted customer/payment data. Sellers only receive
                        // dedicated projections when those endpoints are implemented.
                        .requestMatchers("/products/**", "/clients/**", "/orders/**", "/payment/**", "/payments/**", "/addresses/**").hasRole("GERENTE")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> SecurityJson.error(response, 401))
                        .accessDeniedHandler((request, response, ex) -> SecurityJson.error(response, 403)))
                .logout(l -> l.logoutUrl("/auth/logout").deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)));
        return http.build();
    }
}
