package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.management.ManagementDtos.ManagementSummary;
import com.swee.ordermanagementspring.api.management.ManagementDtos.RevenuePeriod;
import com.swee.ordermanagementspring.api.management.ManagementDtos.RevenueReport;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gestao do contrato v4: faturamento e resumo de metas. Somente GERENTE (URL e servico). */
@RestController
@RequestMapping("/management")
public class ManagementController {
    private final ManagementService service;
    private final GoalsService goals;

    public ManagementController(ManagementService service, GoalsService goals) {
        this.service = service;
        this.goals = goals;
    }

    @GetMapping("/revenue")
    public RevenueReport revenue(@RequestParam RevenuePeriod period) {
        return service.revenue(period);
    }

    @GetMapping("/summary")
    public ManagementSummary summary(@RequestParam @Pattern(regexp = ManagementDtos.MONTH) String period) {
        return goals.summary(period);
    }
}
