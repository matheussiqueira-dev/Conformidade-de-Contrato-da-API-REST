package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.management.ManagementDtos.*;
import com.swee.ordermanagementspring.api.management.ManagementRepository.Day;
import com.swee.ordermanagementspring.entities.StoreTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Faturamento do GERENTE (RF-09, D9 do modelo de dados): soma os dias de v_faturamento_diario do periodo.
 * Lucro = liquido - custo conhecido e fica nulo se algum dia nao tiver custo (C-20); margem = lucro / liquido;
 * cobertura de custo = receita com custo / bruto. Percentuais com 1 casa, HALF_UP.
 */
@Service
@PreAuthorize("hasRole('GERENTE')")
public class ManagementService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ManagementRepository repository;

    public ManagementService(ManagementRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RevenueReport revenue(RevenuePeriod period) {
        LocalDate today = StoreTime.today();
        DateRange range = period.range(today);
        List<Day> days = repository.days(range);

        BigDecimal gross = sum(days, Day::gross);
        BigDecimal refunds = sum(days, Day::refunds);
        BigDecimal net = sum(days, Day::net);
        BigDecimal withCost = sum(days, Day::revenueWithCost);
        long ordersPaid = days.stream().mapToLong(Day::ordersPaid).sum();
        long units = days.stream().mapToLong(Day::units).sum();
        boolean costKnown = days.stream().allMatch(d -> d.grossProfit() != null);
        BigDecimal profit = costKnown ? sum(days, Day::grossProfit) : null;

        BigDecimal averageTicket = ordersPaid == 0 ? null
                : gross.divide(BigDecimal.valueOf(ordersPaid), 2, RoundingMode.HALF_UP);
        BigDecimal margin = profit == null || net.signum() <= 0 ? null
                : profit.multiply(HUNDRED).divide(net, 1, RoundingMode.HALF_UP);
        // D-EXEC-01: sem venda no periodo a cobertura e 0 (nada foi coberto), nunca um numero inventado.
        BigDecimal coverage = gross.signum() <= 0 ? BigDecimal.ZERO.setScale(1)
                : withCost.multiply(HUNDRED).divide(gross, 1, RoundingMode.HALF_UP).min(BigDecimal.valueOf(1000, 1));

        Map<LocalDate, Day> byDate = days.stream().collect(Collectors.toMap(Day::date, Function.identity()));
        List<DailyRevenue> daily = new ArrayList<>();
        for (LocalDate date = range.from(); !date.isAfter(range.to()); date = date.plusDays(1)) {
            Day day = byDate.get(date);
            daily.add(day == null ? new DailyRevenue(date, MoneyDto.of(BigDecimal.ZERO), MoneyDto.of(BigDecimal.ZERO))
                    : new DailyRevenue(date, MoneyDto.of(day.net()), MoneyDto.of(day.grossProfit())));
        }

        var pending = repository.pending();
        return new RevenueReport(range, MoneyDto.of(gross), MoneyDto.of(refunds), MoneyDto.of(net), ordersPaid, units,
                MoneyDto.of(averageTicket), MoneyDto.of(profit), margin, coverage, daily, repository.byPaymentMethod(range),
                repository.bySeller(range), repository.topProducts(range), repository.lowStockCount(), pending.count(),
                MoneyDto.of(pending.amount()), OffsetDateTime.now(StoreTime.clock()));
    }

    private static BigDecimal sum(List<Day> days, Function<Day, BigDecimal> value) {
        return days.stream().map(value).map(v -> v == null ? BigDecimal.ZERO : v).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
