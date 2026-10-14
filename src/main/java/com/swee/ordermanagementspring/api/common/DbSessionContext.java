package com.swee.ordermanagementspring.api.common;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Informa o usuario (e o motivo, quando houver) aos gatilhos da V5 no inicio de cada transacao de escrita:
 * historico de preco, autor de promocao e de entrada de estoque. set_config(..., true) vale so na transacao.
 */
@Component
public class DbSessionContext {
    private final JdbcClient jdbc;

    public DbSessionContext(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void apply(long userId, String reason) {
        jdbc.sql("SELECT set_config('app.user_id', :user, true), set_config('app.reason', :reason, true)")
                .param("user", Long.toString(userId))
                .param("reason", reason == null ? "" : reason)
                .query().singleRow();
    }
}
