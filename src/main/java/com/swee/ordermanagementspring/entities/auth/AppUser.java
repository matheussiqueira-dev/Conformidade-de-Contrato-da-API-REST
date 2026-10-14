package com.swee.ordermanagementspring.entities.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private UserRole role;
    // D2 (V5): situacao da conta e bloqueio por tentativas
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private short failedLoginAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    // V8: so leitura pela entidade; as escritas sao UPDATE explicitos (cadastro de usuarios, troca de senha)
    @Column(insertable = false, updatable = false)
    private int sessionVersion;
    @Column(insertable = false, updatable = false)
    private boolean mustChangePassword;

    protected AppUser() {}
    public AppUser(String name, String email, String passwordHash, UserRole role) {
        this.name = name;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.role = role;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public int getSessionVersion() { return sessionVersion; }
    public boolean isMustChangePassword() { return mustChangePassword; }

    public void deactivate() { this.active = false; }

    /** Uma falha de senha; devolve o total de falhas seguidas. */
    public int registerFailure() {
        this.failedLoginAttempts = (short) (failedLoginAttempts + 1);
        return failedLoginAttempts;
    }

    public void lockUntil(Instant until) {
        this.lockedUntil = until;
        this.failedLoginAttempts = 0;
    }

    public void registerSuccess(Instant now) {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = now;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
