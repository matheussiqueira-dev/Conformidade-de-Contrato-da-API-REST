package com.swee.ordermanagementspring.support;

import com.swee.ordermanagementspring.config.BusinessTables;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.security.AppPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Contas fixas (GERENTE id 1, VENDEDOR id 2) para testes que autenticam com o user() do MockMvc. O user() grava o
 * contexto numa sessao HTTP, entao o SessionVersionFilter confere a conta no banco: sem a linha, a requisicao
 * responderia 401 SESSION_REVOKED.
 */
public final class SyntheticAccounts {
    private SyntheticAccounts() {}

    public static void reset(JdbcTemplate jdbc) {
        BusinessTables.truncate(jdbc);
        jdbc.update("""
                INSERT INTO app_users (id, name, email, password_hash, role) VALUES
                    (1, 'GERENTE', 'gerente@example.test', 'hash', 'GERENTE'),
                    (2, 'VENDEDOR', 'vendedor@example.test', 'hash', 'VENDEDOR')""");
        jdbc.queryForObject("SELECT setval(pg_get_serial_sequence('app_users', 'id'), 2)", Long.class);
    }

    public static AppPrincipal principal(UserRole role) {
        AppUser user = new AppUser(role.name(), role.name().toLowerCase() + "@example.test", "hash", role);
        ReflectionTestUtils.setField(user, "id", role == UserRole.GERENTE ? 1L : 2L);
        return new AppPrincipal(user);
    }
}
