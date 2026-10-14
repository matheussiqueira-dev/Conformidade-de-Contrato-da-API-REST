package com.swee.ordermanagementspring.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcSessionStateReader implements SessionStateReader {
    private final JdbcClient jdbc;

    public JdbcSessionStateReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SessionState> find(long userId) {
        return jdbc.sql("SELECT active, session_version FROM app_users WHERE id = :id")
                .param("id", userId)
                .query((rs, n) -> new SessionState(rs.getBoolean("active"), rs.getInt("session_version")))
                .optional();
    }
}
