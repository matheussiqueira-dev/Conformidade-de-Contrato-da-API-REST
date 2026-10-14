package com.swee.ordermanagementspring.security;

import java.util.Optional;

/** Estado atual da conta, lido a cada requisicao com sessao (uma consulta por chave primaria). */
public interface SessionStateReader {

    record SessionState(boolean active, int sessionVersion) {}

    Optional<SessionState> find(long userId);
}
