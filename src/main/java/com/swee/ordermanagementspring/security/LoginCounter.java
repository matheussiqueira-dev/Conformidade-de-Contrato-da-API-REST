package com.swee.ordermanagementspring.security;

import java.time.Instant;
import java.util.Optional;

/**
 * Contador de falhas de login gravado por UPDATE atomico no banco (G-02): duas tentativas simultaneas nunca
 * leem o mesmo valor e gravam o mesmo incremento.
 */
public interface LoginCounter {

    /** Estado da conta depois de uma falha; {@code lockedNow} indica que esta falha acionou o bloqueio. */
    record FailureState(int failedAttempts, Instant lockedUntil, boolean lockedNow) {}

    /**
     * Soma uma falha. Ao chegar em {@code maxFailures}, bloqueia ate {@code lockUntil} e zera o contador.
     * Conta ja bloqueada em {@code now} nao e alterada (o bloqueio nao se prolonga) e devolve vazio.
     */
    Optional<FailureState> registerFailure(long userId, int maxFailures, Instant lockUntil, Instant now);

    /** Zera contador e bloqueio e grava o ultimo acesso, sem tocar nas demais colunas da conta. */
    void registerSuccess(long userId, Instant now);
}
