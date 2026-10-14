package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.backoffice.BackofficeDtos.StockProduct;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Faturamento, metas, desempenho, estorno e analises do contrato v4 (REC-e: faturamento em /management/revenue). */
public final class ManagementDtos {
    private ManagementDtos() {
    }

    /** Periodos fechados em America/Fortaleza contados a partir de hoje (C-21). */
    public enum RevenuePeriod {
        TODAY(1), D7(7), D30(30), D90(90);

        private final int days;

        RevenuePeriod(int days) {
            this.days = days;
        }

        public DateRange range(LocalDate today) {
            return new DateRange(today.minusDays(days - 1L), today);
        }
    }

    public record DateRange(LocalDate from, LocalDate to) { }

    public record DailyRevenue(LocalDate date, MoneyDto netRevenue, MoneyDto grossProfit) { }

    public record PaymentMethodRevenue(String method, long ordersPaid, MoneyDto netRevenue) { }

    public record SellerRevenue(UserRef seller, long ordersPaid, MoneyDto netRevenue) { }

    public record ProductRevenue(long productId, String sku, String name, long unitsSold, MoneyDto netRevenue,
                                 MoneyDto grossProfit) { }

    public record RevenueReport(DateRange period, MoneyDto grossRevenue, MoneyDto refunds, MoneyDto netRevenue,
                                long ordersPaid, long unitsSold, MoneyDto averageTicket, MoneyDto grossProfit,
                                BigDecimal grossMarginPercent, BigDecimal costCoveragePercent, List<DailyRevenue> daily,
                                List<PaymentMethodRevenue> byPaymentMethod, List<SellerRevenue> bySeller,
                                List<ProductRevenue> topProducts, long lowStockCount, long pendingPaymentCount,
                                MoneyDto pendingPaymentAmount, OffsetDateTime generatedAt) { }

    // ---------------- metas e desempenho ----------------

    /** Mes no formato YYYY-MM (contrato v4). */
    public static final String MONTH = "^\\d{4}-(0[1-9]|1[0-2])$";

    public enum GoalState { SEM_META, EM_ANDAMENTO, META_ATINGIDA }

    public record SellerGoalRequest(@NotNull @Min(1) Long sellerId, @NotNull @Valid MoneyDto goal) { }

    public record MonthlyGoalsRequest(@NotNull @Pattern(regexp = MONTH) String period,
                                      @NotNull @Valid MoneyDto storeGoal,
                                      @NotNull @Size(max = 200) List<@NotNull @Valid SellerGoalRequest> sellers) { }

    public record SellerGoal(UserRef seller, MoneyDto goal) { }

    public record MonthlyGoals(String period, MoneyDto storeGoal, List<SellerGoal> sellers) { }

    public record SellerPerformance(String period, UserRef seller, MoneyDto goal, MoneyDto realized, MoneyDto remaining,
                                    BigDecimal achievementPercent, GoalState state) { }

    public record SellerMetric(UserRef seller, MoneyDto goal, MoneyDto realized, BigDecimal achievementPercent,
                               GoalState state) { }

    public record ManagementSummary(String period, MoneyDto storeGoal, MoneyDto storeRealized, MoneyDto remaining,
                                    BigDecimal achievementPercent, GoalState state, List<SellerMetric> team) { }

    // ---------------- estorno ----------------

    public record RefundRequest(@NotNull @Valid MoneyDto amount, @NotBlank @Size(min = 5, max = 200) String reason) { }

    public record RefundResponse(long id, long paymentId, MoneyDto amount, String reason, MoneyDto refundedTotal,
                                 MoneyDto remaining, UserRef createdBy, OffsetDateTime createdAt) { }

    // ---------------- analises ----------------

    public record WeeklyRevenue(LocalDate start, LocalDate end, MoneyDto realized) { }

    public record ProductMargin(long productId, String name, long units, MoneyDto netRevenue, MoneyDto cost,
                                MoneyDto grossMargin, BigDecimal marginPercent, long itemsWithoutCost) { }

    public record PromotionUsage(long promotionId, String name, long items, MoneyDto discountTotal, MoneyDto netRevenue) { }

    public record SalesAnalytics(String month, List<WeeklyRevenue> weekly, List<PaymentMethodRevenue> byPaymentMethod,
                                 List<ProductMargin> productMargins, List<PromotionUsage> promotionUsage) { }

    public record StockAnalytics(OffsetDateTime generatedAt, long products, long units, MoneyDto valueAtCost,
                                 MoneyDto valueAtPrice, long productsWithoutCost, List<StockProduct> belowMin) { }
}
