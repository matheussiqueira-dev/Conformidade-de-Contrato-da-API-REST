package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Usuario guardado na sessao. {@code sessionVersion} e a versao da conta no login: o {@link SessionVersionFilter}
 * encerra a sessao quando a versao no banco muda (papel, situacao, senha ou desbloqueio).
 */
public final class AppPrincipal extends User {
    private static final long serialVersionUID = 2L;
    private final Long id;
    private final String name;
    private final UserRole role;
    private final int sessionVersion;
    private final boolean mustChangePassword;

    public AppPrincipal(AppUser user) {
        this(user.getId(), user.getName(), user.getEmail(), user.getPasswordHash(), user.getRole(),
                user.getSessionVersion(), user.isMustChangePassword());
    }

    private AppPrincipal(Long id, String name, String email, String passwordHash, UserRole role, int sessionVersion,
                         boolean mustChangePassword) {
        super(email, passwordHash, authorities(email, passwordHash, role));
        this.id = id;
        this.name = name;
        this.role = role;
        this.sessionVersion = sessionVersion;
        this.mustChangePassword = mustChangePassword;
    }

    private static Collection<? extends GrantedAuthority> authorities(String email, String passwordHash, UserRole role) {
        return User.withUsername(email).password(passwordHash).roles(role.name()).build().getAuthorities();
    }

    /** Mesmo usuario depois da troca da propria senha: nova versao e sem troca pendente. */
    public AppPrincipal afterPasswordChange(int newSessionVersion, String newPasswordHash) {
        return new AppPrincipal(id, name, getUsername(), newPasswordHash, role, newSessionVersion, false);
    }

    public Long id() { return id; }
    public String name() { return name; }
    public UserRole role() { return role; }
    public int sessionVersion() { return sessionVersion; }
    public boolean mustChangePassword() { return mustChangePassword; }
}
