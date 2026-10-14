package com.swee.ordermanagementspring.api.discounts;

import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Contrato v5 (G-04): alcada de desconto manual por papel. */
public final class DiscountDtos {
    private DiscountDtos() {}

    public record DiscountPolicy(UserRole role, BigDecimal maxPercent, boolean allowBelowCost, UserRef updatedBy,
                                 OffsetDateTime updatedAt) { }

    public record DiscountPolicyRequest(@NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal maxPercent,
                                        @NotNull Boolean allowBelowCost,
                                        @NotBlank @Size(min = 5, max = 200) String reason) { }
}
