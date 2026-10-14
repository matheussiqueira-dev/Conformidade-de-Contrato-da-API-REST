package com.swee.ordermanagementspring.entities;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * B-10: hora da loja. As colunas de pedido e pagamento sao timestamp sem fuso e as views do V6 assumem
 * America/Fortaleza; sem isto a hora dependeria do fuso da JVM (a CI roda em UTC).
 */
public final class StoreTime {

    public static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
    private static final Clock CLOCK = Clock.system(ZONE);

    private StoreTime() {
    }

    public static Clock clock() {
        return CLOCK;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }

    public static LocalDate today() {
        return LocalDate.now(CLOCK);
    }
}
