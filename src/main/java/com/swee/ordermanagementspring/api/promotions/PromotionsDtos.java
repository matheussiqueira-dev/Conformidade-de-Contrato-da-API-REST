package com.swee.ordermanagementspring.api.promotions;

import com.swee.ordermanagementspring.api.common.Refs.CategoryRef;
import com.swee.ordermanagementspring.api.common.Refs.ProductRef;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.entities.Money;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Promocoes liberadas pelo GERENTE (RF-23): PERCENT de 1 a 100 ou AMOUNT; escopo por produto, categoria ou loja toda. */
public final class PromotionsDtos {
    private PromotionsDtos() {
    }

    public enum PromotionKind { PERCENT, AMOUNT }

    public record PromotionRequest(@NotBlank @Size(min = 3, max = 120) String name,
                                   @NotNull PromotionKind kind,
                                   @NotNull @DecimalMin(value = "0.00", inclusive = false) @DecimalMax(Money.MAX)
                                   @Digits(integer = 10, fraction = 2) BigDecimal value,
                                   @Min(1) Long productId,
                                   @Min(1) Long categoryId,
                                   @NotNull OffsetDateTime startsAt,
                                   @NotNull OffsetDateTime endsAt) {
        @AssertTrue(message = "value: a PERCENT promotion goes from 1 to 100")
        public boolean isPercentInRange() {
            return kind != PromotionKind.PERCENT || value == null
                    || (value.compareTo(BigDecimal.ONE) >= 0 && value.compareTo(BigDecimal.valueOf(100)) <= 0);
        }

        @AssertTrue(message = "endsAt: must be after startsAt")
        public boolean isWindowValid() {
            return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
        }

        @AssertTrue(message = "productId and categoryId: choose at most one scope")
        public boolean isSingleScope() {
            return productId == null || categoryId == null;
        }
    }

    public record Promotion(long id, String name, String kind, BigDecimal value, ProductRef product, CategoryRef category,
                            OffsetDateTime startsAt, OffsetDateTime endsAt, boolean active, UserRef createdBy,
                            OffsetDateTime createdAt) { }
}
