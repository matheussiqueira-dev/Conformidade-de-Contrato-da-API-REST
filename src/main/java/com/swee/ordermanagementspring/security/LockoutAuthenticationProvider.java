package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Login com bloqueio (RF-18). O BCrypt roda antes de qualquer verificacao de estado, inclusive para conta
 * inexistente (hash ficticio), para que o tempo de resposta nao revele se a conta existe, esta bloqueada ou
 * inativa. Os quatro casos de falha lancam a mesma excecao e geram o mesmo 401.
 */
public class LockoutAuthenticationProvider implements AuthenticationProvider {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final LoginAttemptService attempts;
    private final String dummyHash;

    public LockoutAuthenticationProvider(AppUserRepository users, PasswordEncoder encoder, LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.attempts = attempts;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String email = authentication.getName() == null ? "" : authentication.getName().trim().toLowerCase(Locale.ROOT);
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        Optional<AppUser> found = users.findByEmail(email);
        boolean matches = encoder.matches(password, found.map(AppUser::getPasswordHash).orElse(dummyHash));
        if (found.isEmpty()) {
            attempts.rejected(null, email);
            throw invalid();
        }
        AppUser user = found.get();
        if (!user.isActive() || attempts.isLocked(user)) {
            attempts.rejected(user, email);
            throw invalid();
        }
        if (!matches) {
            attempts.wrongPassword(user, email);
            throw invalid();
        }
        attempts.succeeded(user);
        AppPrincipal principal = new AppPrincipal(user);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    private static BadCredentialsException invalid() {
        return new BadCredentialsException("Credenciais invalidas");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
