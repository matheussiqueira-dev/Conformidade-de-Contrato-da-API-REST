package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * RF-18: 5 falhas seguidas bloqueiam a conta por 15 minutos; sucesso zera o contador. Tentativas durante o
 * bloqueio nao o prolongam. Toda falha grava LOGIN_FAILED, existindo a conta ou nao. Contador e bloqueio sao
 * gravados por UPDATE atomico ({@link LoginCounter}), sem salvar a entidade inteira: tentativas paralelas nao
 * perdem incremento e o login nao sobrescreve papel ou situacao alterados por outra requisicao.
 */
@Service
public class LoginAttemptService {
    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final LoginCounter counter;
    private final AuditService audit;
    private final Clock clock;

    public LoginAttemptService(LoginCounter counter, AuditService audit, Clock clock) {
        this.counter = counter;
        this.audit = audit;
        this.clock = clock;
    }

    public boolean isLocked(AppUser user) {
        return user.isLockedAt(clock.instant());
    }

    public void wrongPassword(AppUser user, String attemptedEmail) {
        audit.record(AuditAction.LOGIN_FAILED, user.getId(), "app_users", user.getId(), AuditService.emailDetail(attemptedEmail));
        Instant now = clock.instant();
        counter.registerFailure(user.getId(), MAX_FAILURES, now.plus(LOCK_DURATION), now)
                .filter(LoginCounter.FailureState::lockedNow)
                .ifPresent(state -> audit.record(AuditAction.ACCOUNT_LOCKED, user.getId(), "app_users", user.getId(), null));
    }

    /** Conta bloqueada, inativa ou inexistente: so registra a tentativa. */
    public void rejected(AppUser user, String attemptedEmail) {
        Long id = user == null ? null : user.getId();
        audit.record(AuditAction.LOGIN_FAILED, id, id == null ? null : "app_users", id, AuditService.emailDetail(attemptedEmail));
    }

    public void succeeded(AppUser user) {
        counter.registerSuccess(user.getId(), clock.instant());
        audit.record(AuditAction.LOGIN_SUCCEEDED, user.getId(), "app_users", user.getId(), null);
    }
}
