package com.swee.ordermanagementspring.api.common;

import com.swee.ordermanagementspring.entities.Money;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** Money do contrato v4: valor com 2 casas (HALF_UP no servidor, DEC-ERP-08) e moeda BRL. */
public record MoneyDto(
        @NotNull @DecimalMin("0.00") @DecimalMax(Money.MAX) @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "BRL") String currency) {

    public static MoneyDto of(BigDecimal amount) {
        return amount == null ? null : new MoneyDto(Money.of(amount), "BRL");
    }

    public BigDecimal value() {
        return Money.of(amount);
    }
}
