package com.swee.ordermanagementspring.api.users;

import com.swee.ordermanagementspring.api.users.UsersDtos.UserDetail;
import com.swee.ordermanagementspring.api.users.UsersDtos.UserSummary;
import com.swee.ordermanagementspring.entities.StoreTime;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Consultas de usuario com SQL explicito. O hash da senha so e lido pelo servico de troca de senha. As leituras
 * para alteracao travam a linha com FOR NO KEY UPDATE, que nao bloqueia a FK do audit_event gravado em transacao
 * propria (AuditService) sobre o mesmo usuario.
 */
@Repository
public class UsersRepository {
    private static final String FILTER = """
             WHERE (:q = '' OR lower(name) LIKE :prefix ESCAPE '\\' OR lower(email) LIKE :prefix ESCAPE '\\')
               AND (CAST(:role AS varchar) IS NULL OR role = CAST(:role AS varchar))
               AND (CAST(:active AS boolean) IS NULL OR active = CAST(:active AS boolean))
            """;
    private static final String DETAIL = """
            SELECT id, name, email, role, active, locked_until, last_login_at, failed_login_attempts,
                   must_change_password, version, created_at, updated_at
              FROM app_users
            """;

    private final JdbcClient jdbc;

    public UsersRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Estado usado pelas regras de alteracao (dono da operacao, versao otimista). */
    public record Head(long id, String email, UserRole role, boolean active, int version) { }

    /** Credencial e estado para a troca da propria senha. */
    public record Credential(long id, String email, String passwordHash, boolean active, OffsetDateTime lockedUntil) { }

    public record Filter(String q, UserRole role, Boolean active) {
        String prefix() {
            String escaped = q.toLowerCase(java.util.Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            return escaped + "%";
        }
    }

    public List<UserSummary> search(Filter filter, int page, int size) {
        return jdbc.sql("SELECT id, name, email, role, active, locked_until, last_login_at FROM app_users" + FILTER
                        + " ORDER BY lower(name), id LIMIT :size OFFSET :offset")
                .param("q", filter.q()).param("prefix", filter.prefix())
                .param("role", filter.role() == null ? null : filter.role().name())
                .param("active", filter.active())
                .param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> new UserSummary(rs.getLong("id"), rs.getString("name"), rs.getString("email"),
                        UserRole.valueOf(rs.getString("role")), rs.getBoolean("active"),
                        local(rs, "locked_until"), local(rs, "last_login_at")))
                .list();
    }

    public long count(Filter filter) {
        return jdbc.sql("SELECT count(*) FROM app_users" + FILTER)
                .param("q", filter.q()).param("prefix", filter.prefix())
                .param("role", filter.role() == null ? null : filter.role().name())
                .param("active", filter.active())
                .query(Long.class).single();
    }

    public Optional<UserDetail> detail(long id) {
        return jdbc.sql(DETAIL + " WHERE id = :id").param("id", id).query(UsersRepository::detail).optional();
    }

    public Optional<Head> head(long id) {
        return jdbc.sql("SELECT id, email, role, active, version FROM app_users WHERE id = :id FOR NO KEY UPDATE")
                .param("id", id)
                .query((rs, n) -> new Head(rs.getLong("id"), rs.getString("email"), UserRole.valueOf(rs.getString("role")),
                        rs.getBoolean("active"), rs.getInt("version")))
                .optional();
    }

    public Optional<Credential> credential(long id) {
        return jdbc.sql("SELECT id, email, password_hash, active, locked_until FROM app_users WHERE id = :id FOR NO KEY UPDATE")
                .param("id", id)
                .query((rs, n) -> new Credential(rs.getLong("id"), rs.getString("email"), rs.getString("password_hash"),
                        rs.getBoolean("active"), rs.getObject("locked_until", OffsetDateTime.class)))
                .optional();
    }

    private static UserDetail detail(ResultSet rs, int n) throws SQLException {
        return new UserDetail(rs.getLong("id"), rs.getString("name"), rs.getString("email"),
                UserRole.valueOf(rs.getString("role")), rs.getBoolean("active"), local(rs, "locked_until"),
                local(rs, "last_login_at"), rs.getInt("failed_login_attempts"), rs.getBoolean("must_change_password"),
                rs.getInt("version"), local(rs, "created_at"), local(rs, "updated_at"));
    }

    private static OffsetDateTime local(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.atZoneSameInstant(StoreTime.ZONE).toOffsetDateTime();
    }
}
