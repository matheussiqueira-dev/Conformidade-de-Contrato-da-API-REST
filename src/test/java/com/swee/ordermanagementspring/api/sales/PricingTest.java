package com.swee.ordermanagementspring.api.sales;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** [CT-SALE-101] Desconto calculado no servidor: PERCENT, AMOUNT, teto no valor do item e HALF_UP (equivalente Java do BD-26). */
class PricingTest {

    private static Pricing.Promotion promotion(long id, String kind, String value, Long productId, Long categoryId) {
        return new Pricing.Promotion(id, "Promo " + id, kind, new BigDecimal(value), productId, categoryId,
                OffsetDateTime.parse("2026-12-31T23:59:59-03:00"));
    }

    @ParameterizedTest(name = "[CT-SALE-101] {0} {1} sobre {2} x {3} = {4}")
    @CsvSource({
            "PERCENT, 10,    19.99, 3, 6.00",   // 5.997 -> 6.00
            "PERCENT, 10,    1.25,  1, 0.13",   // 0.125 -> 0.13 (HALF_UP, nao HALF_EVEN)
            "PERCENT, 100,   30.00, 2, 60.00",
            "PERCENT, 12.5,  0.10,  1, 0.01",   // 0.0125 -> 0.01
            "AMOUNT,  5.00,  20.00, 2, 10.00",
            "AMOUNT,  50.00, 30.00, 1, 30.00",  // desconto nunca passa do valor do item
            "AMOUNT,  0.01,  0.01,  999, 9.99"})
    void discountUsesServerRules(String kind, String value, String price, int quantity, String expected) {
        BigDecimal discount = Pricing.discount(new BigDecimal(price), quantity, promotion(1, kind, value, null, null));
        assertThat(discount).isEqualTo(new BigDecimal(expected));
        assertThat(discount.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("[CT-SALE-101] sem promocao o desconto e 0,00 e o bruto tem duas casas")
    void noPromotionMeansZero() {
        assertThat(Pricing.discount(new BigDecimal("10.00"), 3, null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Pricing.gross(new BigDecimal("33.33"), 3)).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("[C-09] activePromotion e a de maior desconto por unidade; empate fica com o menor id")
    void bestPromotionPerUnit() {
        BigDecimal price = new BigDecimal("100.00");
        var storeWide = promotion(1, "PERCENT", "10", null, null);      // 10,00
        var category = promotion(2, "AMOUNT", "15.00", null, 7L);       // 15,00
        var product = promotion(3, "PERCENT", "15", 42L, null);         // 15,00, empata com a 2
        var otherProduct = promotion(4, "PERCENT", "90", 43L, null);

        assertThat(Pricing.best(List.of(storeWide, category, product, otherProduct), 42L, 7L, price))
                .get().extracting(Pricing.Promotion::id).isEqualTo(2L);
        assertThat(Pricing.best(List.of(storeWide, product, otherProduct), 42L, 8L, price))
                .get().extracting(Pricing.Promotion::id).isEqualTo(3L);
        assertThat(Pricing.best(List.of(otherProduct), 42L, 7L, price)).isEmpty();
        assertThat(Pricing.best(List.of(category), 42L, null, price)).isEmpty();
    }
}
