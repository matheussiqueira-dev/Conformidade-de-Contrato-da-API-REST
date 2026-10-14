package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.management.ManagementDtos.SalesAnalytics;
import com.swee.ordermanagementspring.api.management.ManagementDtos.StockAnalytics;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Analises do contrato v4. Somente GERENTE (URL e servico). */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/sales")
    public SalesAnalytics sales(@RequestParam @Pattern(regexp = ManagementDtos.MONTH) String month) {
        return service.sales(month);
    }

    @GetMapping("/stock")
    public StockAnalytics stock() {
        return service.stock();
    }
}
