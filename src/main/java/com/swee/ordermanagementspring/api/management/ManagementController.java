package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.management.ManagementDtos.RevenuePeriod;
import com.swee.ordermanagementspring.api.management.ManagementDtos.RevenueReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gestao do contrato v4. Somente GERENTE (URL e servico). */
@RestController
@RequestMapping("/management")
public class ManagementController {
    private final ManagementService service;

    public ManagementController(ManagementService service) {
        this.service = service;
    }

    @GetMapping("/revenue")
    public RevenueReport revenue(@RequestParam RevenuePeriod period) {
        return service.revenue(period);
    }
}
