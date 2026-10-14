package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.config.BusinessTables;
import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import com.swee.ordermanagementspring.repositories.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [CT-SEC-120] Falhas simultaneas no PostgreSQL real. Antes do UPDATE atomico, cada requisicao lia o contador
 * da entidade carregada no inicio do login e gravava o mesmo incremento: 10 falhas paralelas terminavam com o
 * contador em 1 e a conta sem bloqueio. A corrida e repetida porque depende do escalonamento das threads.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class LoginAttemptConcurrencyTest {

    private static final String EMAIL = "corrida@example.test";
    private static final int ATTEMPTS = 10;
    private static final int ROUNDS = 3;

    @Autowired AuthenticationManager manager;
    @Autowired AppUserRepository users;
    @Autowired AuditEventRepository audit;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    @BeforeEach
    void setUp() {
        BusinessTables.truncate(jdbc);
    }

    @Test
    @DisplayName("[CT-SEC-120] 10 senhas erradas em paralelo bloqueiam a conta uma unica vez, sem perder incremento")
    void parallelFailuresLockTheAccountOnce() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            BusinessTables.truncate(jdbc);
            users.save(new AppUser("Corrida", EMAIL, encoder.encode("senha-sintetica-correta"), UserRole.VENDEDOR));

            assertThat(failInParallel()).as("rodada %d: todas as tentativas recusadas", round).isEqualTo(ATTEMPTS);

            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT failed_login_attempts, locked_until FROM app_users WHERE email = ?", EMAIL);
            assertThat(row.get("locked_until")).as("rodada %d: conta bloqueada", round).isNotNull();
            assertThat(((Timestamp) row.get("locked_until")).toInstant()).isAfter(clock.instant());
            assertThat(audit.findByActionOrderByIdAsc(AuditAction.ACCOUNT_LOCKED)).as("rodada %d", round).hasSize(1);
            assertThat(audit.findByActionOrderByIdAsc(AuditAction.LOGIN_FAILED)).as("rodada %d", round).hasSize(ATTEMPTS);
        }
    }

    private int failInParallel() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(ATTEMPTS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < ATTEMPTS; i++) {
                String password = "senha-errada-" + i;
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, password));
                        return false;
                    } catch (BadCredentialsException expected) {
                        return true;
                    }
                }));
            }
            start.countDown();
            int rejected = 0;
            for (Future<Boolean> result : results) {
                if (result.get(60, TimeUnit.SECONDS)) rejected++;
            }
            return rejected;
        } finally {
            pool.shutdownNow();
        }
    }
}
