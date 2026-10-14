package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** [CT-AUTH-101] Politica de bloqueio com Clock controlado e BCrypt executado em todos os casos. */
class LoginLockoutPolicyTest {

    private static final String PASSWORD = "senha-correta-123";
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-14T12:00:00Z"));
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private AppUser ana;
    private LockoutAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        ana = new AppUser("Ana", "ana@example.test", "hash-ana", UserRole.VENDEDOR);
        ReflectionTestUtils.setField(ana, "id", 7L);
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        when(users.findByEmail("ana@example.test")).thenReturn(Optional.of(ana));
        when(encoder.encode(anyString())).thenReturn("hash-ficticio");
        when(encoder.matches(anyString(), anyString())).thenReturn(false);
        when(encoder.matches(PASSWORD, "hash-ana")).thenReturn(true);
        provider = new LockoutAuthenticationProvider(users, encoder, new LoginAttemptService(new EntityCounter(ana), audit, clock));
    }

    private UsernamePasswordAuthenticationToken login(String email, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(email, password);
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            assertThatThrownBy(() -> provider.authenticate(login("ana@example.test", "errada"))).isInstanceOf(BadCredentialsException.class);
        }
    }

    @Test
    @DisplayName("[CT-AUTH-101] 4 falhas nao bloqueiam; a 5a bloqueia por 15 minutos")
    void fifthFailureLocksForFifteenMinutes() {
        fail(4);
        assertThat(ana.getLockedUntil()).isNull();
        assertThat(provider.authenticate(login("ana@example.test", PASSWORD)).isAuthenticated()).isTrue();
        assertThat(ana.getFailedLoginAttempts()).isZero();

        fail(5);
        assertThat(ana.getLockedUntil()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
        assertThatThrownBy(() -> provider.authenticate(login("ana@example.test", PASSWORD))).isInstanceOf(BadCredentialsException.class);

        clock.advance(Duration.ofMinutes(15).minusSeconds(1));
        assertThatThrownBy(() -> provider.authenticate(login("ana@example.test", PASSWORD))).isInstanceOf(BadCredentialsException.class);
        clock.advance(Duration.ofSeconds(1));
        assertThat(provider.authenticate(login("ana@example.test", PASSWORD)).isAuthenticated()).isTrue();
        assertThat(ana.getLockedUntil()).isNull();
        assertThat(ana.getLastLoginAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("[CT-AUTH-101] tentativas durante o bloqueio nao o prolongam")
    void attemptsDuringLockDoNotExtendIt() {
        fail(5);
        Instant until = ana.getLockedUntil();
        clock.advance(Duration.ofMinutes(10));
        fail(3);
        assertThat(ana.getLockedUntil()).isEqualTo(until);
    }

    @Test
    @DisplayName("[CT-AUTH-101] BCrypt roda tambem para conta bloqueada, inativa e inexistente")
    void passwordEncoderRunsInEveryFailureCase() {
        fail(5);
        clearInvocations(encoder);
        assertThatThrownBy(() -> provider.authenticate(login("ana@example.test", PASSWORD))).isInstanceOf(BadCredentialsException.class);
        verify(encoder).matches(PASSWORD, "hash-ana");

        clearInvocations(encoder);
        assertThatThrownBy(() -> provider.authenticate(login("ninguem@example.test", PASSWORD))).isInstanceOf(BadCredentialsException.class);
        verify(encoder).matches(PASSWORD, "hash-ficticio");

        AppUser inactive = new AppUser("Bia", "bia@example.test", "hash-bia", UserRole.VENDEDOR);
        inactive.deactivate();
        when(users.findByEmail("bia@example.test")).thenReturn(Optional.of(inactive));
        when(encoder.matches(PASSWORD, "hash-bia")).thenReturn(true);
        clearInvocations(encoder);
        assertThatThrownBy(() -> provider.authenticate(login("bia@example.test", PASSWORD))).isInstanceOf(BadCredentialsException.class);
        verify(encoder).matches(PASSWORD, "hash-bia");
    }

    @Test
    @DisplayName("[CT-AUTH-101] toda falha grava LOGIN_FAILED com e-mail so em SHA-256")
    void everyFailureIsAuditedWithoutPlainEmail() {
        assertThatThrownBy(() -> provider.authenticate(login("Ninguem@Example.test", "x"))).isInstanceOf(BadCredentialsException.class);
        String expected = AuditService.emailDetail("ninguem@example.test");
        verify(audit).record(eq(com.swee.ordermanagementspring.entities.audit.AuditAction.LOGIN_FAILED), eq(null), eq(null), eq(null), eq(expected));
        assertThat(expected).doesNotContain("ninguem").matches("\\{\"emailSha256\":\"[0-9a-f]{64}\"}");
    }

    /**
     * Emula o UPDATE atomico do {@link JdbcLoginCounter} sobre a propria entidade, que faz o papel da linha do
     * banco. A atomicidade real e coberta pelo CT-SEC-120 ({@code LoginAttemptConcurrencyTest}).
     */
    static final class EntityCounter implements LoginCounter {
        private final Map<Long, AppUser> rows = new HashMap<>();

        EntityCounter(AppUser... users) {
            for (AppUser user : users) rows.put(user.getId(), user);
        }

        @Override
        public Optional<FailureState> registerFailure(long userId, int maxFailures, Instant lockUntil, Instant now) {
            AppUser row = rows.get(userId);
            if (row == null || row.isLockedAt(now)) return Optional.empty();
            int failures = row.registerFailure();
            if (failures < maxFailures) return Optional.of(new FailureState(failures, row.getLockedUntil(), false));
            row.lockUntil(lockUntil);
            return Optional.of(new FailureState(0, lockUntil, true));
        }

        @Override
        public void registerSuccess(long userId, Instant now) {
            rows.get(userId).registerSuccess(now);
        }
    }

    /** Clock ajustavel para simular a passagem dos 15 minutos. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneId.of("America/Fortaleza"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
