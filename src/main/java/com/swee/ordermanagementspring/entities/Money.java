package com.swee.ordermanagementspring.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DOM-02: regra unica de dinheiro (decisao DOM-01): duas casas decimais, HALF_UP.
 * Colunas monetarias sao NUMERIC(PRECISION, SCALE) (migracao V3).
 */
public final class Money {

    public static final int PRECISION = 12;
    public static final int SCALE = 2;
    /** Maior valor que cabe em NUMERIC(12,2); usado tambem na validacao dos DTOs. */
    public static final String MAX = "9999999999.99";

    private Money() {
    }

    /** Normaliza para duas casas com HALF_UP; null continua null (campo obrigatorio e validado no DTO). */
    public static BigDecimal of(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String value) {
        return of(new BigDecimal(value));
    }
}
