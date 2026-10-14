package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Relatorio de faturamento do contrato v4 (REC-e: faturamento em /management/revenue). */
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
}
