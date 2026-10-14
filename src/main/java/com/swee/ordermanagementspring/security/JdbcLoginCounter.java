package com.swee.ordermanagementspring.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * Leitura e escrita na mesma instrucao: o PostgreSQL trava a linha durante o UPDATE, entao tentativas
 * paralelas sao serializadas. O instante vem do Clock da aplicacao (testes deterministicos), nunca de now().
 */
@Repository
public class JdbcLoginCounter implements LoginCounter {
    private final JdbcClient jdbc;

    public JdbcLoginCounter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<FailureState> registerFailure(long userId, int maxFailures, Instant lockUntil, Instant now) {
        return jdbc.sql("""
                UPDATE app_users
                   SET failed_login_attempts = CASE WHEN failed_login_attempts + 1 >= :max THEN 0
                                                    ELSE failed_login_attempts + 1 END,
                       locked_until = CASE WHEN failed_login_attempts + 1 >= :max THEN :lockUntil
                                           ELSE locked_until END
                 WHERE id = :id
                   AND (locked_until IS NULL OR locked_until <= :now)
                RETURNING failed_login_attempts, locked_until, (locked_until = :lockUntil) AS locked_now
                """)
                .param("max", maxFailures)
                .param("lockUntil", Timestamp.from(lockUntil))
                .param("now", Timestamp.from(now))
                .param("id", userId)
                .query((rs, n) -> {
                    Timestamp until = rs.getTimestamp("locked_until");
                    return new FailureState(rs.getInt("failed_login_attempts"),
                            until == null ? null : until.toInstant(), rs.getBoolean("locked_now"));
                })
                .optional();
    }

    @Override
    public void registerSuccess(long userId, Instant now) {
        jdbc.sql("UPDATE app_users SET failed_login_attempts = 0, locked_until = NULL, last_login_at = :now WHERE id = :id")
                .param("now", Timestamp.from(now))
                .param("id", userId)
                .update();
    }
}
