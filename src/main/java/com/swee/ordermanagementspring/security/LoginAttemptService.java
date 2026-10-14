package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

/**
 * RF-18: 5 falhas seguidas bloqueiam a conta por 15 minutos; sucesso zera o contador. Tentativas durante o
 * bloqueio nao o prolongam. Toda falha grava LOGIN_FAILED, existindo a conta ou nao.
 */
@Service
public class LoginAttemptService {
    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AppUserRepository users;
    private final AuditService audit;
    private final Clock clock;

    public LoginAttemptService(AppUserRepository users, AuditService audit, Clock clock) {
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    public boolean isLocked(AppUser user) {
        return user.isLockedAt(clock.instant());
    }

    public void wrongPassword(AppUser user, String attemptedEmail) {
        audit.record(AuditAction.LOGIN_FAILED, user.getId(), "app_users", user.getId(), AuditService.emailDetail(attemptedEmail));
        if (user.registerFailure() >= MAX_FAILURES) {
            user.lockUntil(clock.instant().plus(LOCK_DURATION));
            audit.record(AuditAction.ACCOUNT_LOCKED, user.getId(), "app_users", user.getId(), null);
        }
        users.save(user);
    }

    /** Conta bloqueada, inativa ou inexistente: so registra a tentativa. */
    public void rejected(AppUser user, String attemptedEmail) {
        Long id = user == null ? null : user.getId();
        audit.record(AuditAction.LOGIN_FAILED, id, id == null ? null : "app_users", id, AuditService.emailDetail(attemptedEmail));
    }

    public void succeeded(AppUser user) {
        user.registerSuccess(clock.instant());
        users.save(user);
        audit.record(AuditAction.LOGIN_SUCCEEDED, user.getId(), "app_users", user.getId(), null);
    }
}
