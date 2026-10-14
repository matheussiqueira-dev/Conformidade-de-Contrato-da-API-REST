package com.swee.ordermanagementspring.api.sales;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** [CT-DISC-105] Ordem de calculo e centavos do desconto manual (G-04), sem banco. */
class ManualDiscountPricingTest {

    private static List<BigDecimal> money(String... values) {
        return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
    }

    @ParameterizedTest(name = "[CT-DISC-105] {1}% sobre {0} = {2}")
    @CsvSource({"100.00, 5, 5.00", "19.99, 7.5, 1.50", "0.10, 12.5, 0.01", "250.00, 0.01, 0.03", "90.00, 100, 90.00"})
    void manualDiscountIsHalfUpOnThePromotedSubtotal(String subtotal, String percent, String expected) {
        assertThat(Pricing.manualDiscount(new BigDecimal(subtotal), new BigDecimal(percent))).isEqualTo(new BigDecimal(expected));
    }

    @Test
    @DisplayName("[CT-DISC-105] bruto -> promocao -> desconto manual: 100 com 10% de promocao e 10% manual = 81,00")
    void manualDiscountComesAfterThePromotion() {
        var promotion = new Pricing.Promotion(1, "Promo", "PERCENT", new BigDecimal("10"), null, null,
                OffsetDateTime.parse("2026-12-31T23:59:59-03:00"));
        BigDecimal gross = Pricing.gross(new BigDecimal("100.00"), 1);
        BigDecimal promoted = gross.subtract(Pricing.discount(new BigDecimal("100.00"), 1, promotion));
        BigDecimal manual = Pricing.manualDiscount(promoted, new BigDecimal("10"));
        assertThat(promoted.subtract(manual)).isEqualTo(new BigDecimal("81.00"));
    }

    @Test
    @DisplayName("[CT-DISC-105] rateio proporcional soma exatamente o desconto; a sobra vai para o maior item")
    void allocationSumsExactly() {
        assertThat(Pricing.allocate(money("33.33", "33.33", "33.34"), new BigDecimal("10.00")))
                .containsExactly(new BigDecimal("3.33"), new BigDecimal("3.33"), new BigDecimal("3.34"));
        assertThat(Pricing.allocate(money("10.00", "20.00", "0.01"), new BigDecimal("3.00")))
                .containsExactly(new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("0.00"));
        List<BigDecimal> shares = Pricing.allocate(money("0.05", "0.05", "0.05"), new BigDecimal("0.01"));
        assertThat(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("[CT-DISC-105] desconto de 100% zera os itens sem deixar nenhum negativo; sem desconto, partes zero")
    void allocationNeverExceedsTheItem() {
        assertThat(Pricing.allocate(money("12.34", "0.66"), new BigDecimal("13.00")))
                .containsExactly(new BigDecimal("12.34"), new BigDecimal("0.66"));
        assertThat(Pricing.allocate(money("12.34", "0.66"), new BigDecimal("0.00")))
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("0.00"));
    }
}
