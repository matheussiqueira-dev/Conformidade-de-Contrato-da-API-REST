package com.swee.ordermanagementspring.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DOM-02: regra unica de dinheiro (decisao DOM-01): duas casas decimais, HALF_UP.
 * Colunas monetarias sao NUMERIC(PRECISION, SCALE) (migracao V4).
 */
public final class Money {

    public static final int PRECISION = 12;
    public static final int SCALE = 2;
    /** Maior valor que cabe em NUMERIC(12,2); usado tambem na validacao dos DTOs. */
    public static final String MAX = "9999999999.99";
    /**
     * D017: menor valor aceito na entrada. 0,005 arredonda (HALF_UP) para 0,01; abaixo disso o valor viraria 0,00.
     * A precisao de NUMERIC(12,2) so e garantida na fronteira dos DTOs (MIN/MAX); codigo que cria entidades
     * sem passar por eles deve respeitar os mesmos limites.
     */
    public static final String MIN = "0.005";

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
