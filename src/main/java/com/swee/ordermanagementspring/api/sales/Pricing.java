package com.swee.ordermanagementspring.api.sales;

import com.swee.ordermanagementspring.entities.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Preco e desconto calculados so no servidor (invariante): PERCENT = preco x quantidade x valor / 100;
 * AMOUNT = valor x quantidade; o desconto nunca passa do valor do item. Duas casas, HALF_UP (DEC-ERP-08).
 */
public final class Pricing {
    private Pricing() {
    }

    /** Promocao vigente lida do banco. productId e categoryId nulos = loja toda. */
    public record Promotion(long id, String name, String kind, BigDecimal value, Long productId, Long categoryId,
                            OffsetDateTime endsAt) {
        public boolean appliesTo(long product, Long category) {
            if (productId != null) return productId == product;
            if (categoryId != null) return categoryId.equals(category);
            return true;
        }
    }

    public static BigDecimal gross(BigDecimal unitPrice, int quantity) {
        return Money.of(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public static BigDecimal discount(BigDecimal unitPrice, int quantity, Promotion promotion) {
        if (promotion == null) return Money.of(BigDecimal.ZERO);
        BigDecimal gross = gross(unitPrice, quantity);
        BigDecimal discount = switch (promotion.kind()) {
            case "PERCENT" -> gross.multiply(promotion.value()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case "AMOUNT" -> promotion.value().multiply(BigDecimal.valueOf(quantity));
            default -> throw new IllegalArgumentException("Unknown promotion kind " + promotion.kind());
        };
        return Money.of(discount.min(gross));
    }

    /**
     * G-04: desconto manual sobre o subtotal ja promocionado (soma dos subtotais dos itens), duas casas, HALF_UP.
     * Ordem de calculo: bruto -> promocoes por item -> desconto manual -> total.
     */
    public static BigDecimal manualDiscount(BigDecimal promotedSubtotal, BigDecimal percent) {
        return Money.of(promotedSubtotal.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    /**
     * Rateia o desconto manual pelos itens na proporcao do subtotal de cada um (HALF_UP), sem passar do subtotal do
     * item. A sobra de arredondamento (alguns centavos) vai para o item de maior subtotal, o primeiro em empate, para
     * que a soma das partes seja exatamente o desconto do pedido.
     */
    public static List<BigDecimal> allocate(List<BigDecimal> subtotals, BigDecimal discount) {
        BigDecimal base = subtotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (base.signum() == 0 || discount.signum() == 0) return subtotals.stream().map(s -> Money.of(BigDecimal.ZERO)).toList();
        java.util.ArrayList<BigDecimal> shares = new java.util.ArrayList<>();
        for (BigDecimal subtotal : subtotals) {
            shares.add(subtotal.multiply(discount).divide(base, 2, RoundingMode.HALF_UP).min(subtotal));
        }
        BigDecimal rest = discount.subtract(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        int largest = 0;
        for (int i = 1; i < subtotals.size(); i++) {
            if (subtotals.get(i).compareTo(subtotals.get(largest)) > 0) largest = i;
        }
        shares.set(largest, shares.get(largest).add(rest));
        return shares.stream().map(Money::of).toList();
    }

    /** C-09: promocao vigente de maior desconto para uma unidade; empate fica com o menor id. */
    public static Optional<Promotion> best(List<Promotion> active, long productId, Long categoryId, BigDecimal unitPrice) {
        return active.stream().filter(p -> p.appliesTo(productId, categoryId))
                .max(Comparator.<Promotion, BigDecimal>comparing(p -> discount(unitPrice, 1, p))
                        .thenComparing(Promotion::id, Comparator.reverseOrder()));
    }
}
