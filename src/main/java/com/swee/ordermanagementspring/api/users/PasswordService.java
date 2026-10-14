package com.swee.ordermanagementspring.api.users;

import com.swee.ordermanagementspring.api.users.UsersDtos.PasswordChangeRequest;
import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.exceptions.ApiException;
import com.swee.ordermanagementspring.exceptions.ErrorCode;
import com.swee.ordermanagementspring.security.AppPrincipal;
import com.swee.ordermanagementspring.security.AuditService;
import com.swee.ordermanagementspring.security.LoginAttemptService;
import com.swee.ordermanagementspring.security.StrongPasswordValidator;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Troca da propria senha (PUT /auth/password). Senha atual errada conta para o bloqueio como no login e nao
 * lanca excecao, para que o incremento seja confirmado. Se essa falha bloquear a conta, as sessoes do usuario
 * sao encerradas (D-EVO-07). Sucesso grava o novo hash, tira a exigencia de troca e incrementa session_version:
 * as outras sessoes do usuario caem e a sessao atual e renovada pelo controller com a nova versao.
 */
@Service
public class PasswordService {
    private final UsersRepository users;
    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final LoginAttemptService attempts;
    private final AuditService audit;

    public PasswordService(UsersRepository users, JdbcClient jdbc, PasswordEncoder encoder, LoginAttemptService attempts,
                           AuditService audit) {
        this.users = users;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.attempts = attempts;
        this.audit = audit;
    }

    /** Principal renovado, ou vazio quando a senha atual nao confere (ou a conta esta bloqueada ou inativa). */
    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional
    public Optional<AppPrincipal> change(AppPrincipal principal, PasswordChangeRequest request) {
        var credential = users.credential(principal.id()).orElse(null);
        // BCrypt roda sempre, como no login, antes de olhar o estado da conta.
        boolean matches = encoder.matches(request.currentPassword(), credential == null ? "" : credential.passwordHash());
        if (credential == null || !credential.active() || attempts.isLocked(credential.lockedUntil())) return Optional.empty();
        if (!matches) {
            if (attempts.wrongPassword(credential.id(), credential.email())) {
                jdbc.sql("UPDATE app_users SET session_version = session_version + 1 WHERE id = :id")
                        .param("id", credential.id()).update();
            }
            return Optional.empty();
        }
        if (StrongPasswordValidator.sameAsEmail(request.newPassword(), credential.email())) {
            throw invalid("newPassword: must differ from the e-mail");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw invalid("newPassword: must differ from the current password");
        }
        String hash = encoder.encode(request.newPassword());
        int version = jdbc.sql("""
                UPDATE app_users SET password_hash = :hash, must_change_password = false, password_changed_at = now(),
                       failed_login_attempts = 0, locked_until = NULL, session_version = session_version + 1,
                       version = version + 1, updated_at = now()
                 WHERE id = :id
                RETURNING session_version""")
                .param("hash", hash).param("id", credential.id())
                .query(Integer.class).single();
        audit.record(AuditAction.PASSWORD_CHANGED, credential.id(), "app_users", credential.id(), null);
        return Optional.of(principal.afterPasswordChange(version, hash));
    }

    private static ApiException invalid(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "One or more fields are invalid", List.of(detail));
    }
}
