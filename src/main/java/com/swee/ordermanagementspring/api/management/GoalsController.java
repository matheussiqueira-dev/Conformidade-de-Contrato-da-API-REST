package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.management.ManagementDtos.MonthlyGoals;
import com.swee.ordermanagementspring.api.management.ManagementDtos.MonthlyGoalsRequest;
import com.swee.ordermanagementspring.api.management.ManagementDtos.SellerPerformance;
import com.swee.ordermanagementspring.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Metas do mes (GERENTE) e Meu desempenho (VENDEDOR e, pela hierarquia, GERENTE: DEC-ERP-01). */
@RestController
public class GoalsController {
    private final GoalsService service;

    public GoalsController(GoalsService service) {
        this.service = service;
    }

    @GetMapping("/goals/monthly")
    public MonthlyGoals get(@RequestParam @Pattern(regexp = ManagementDtos.MONTH) String period) {
        return service.get(period);
    }

    @PutMapping("/goals/monthly")
    public MonthlyGoals save(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody MonthlyGoalsRequest request) {
        return service.save(CurrentUser.of(principal), request);
    }

    @GetMapping("/performance/me")
    public SellerPerformance mine(@AuthenticationPrincipal AppPrincipal principal,
                                  @RequestParam @Pattern(regexp = ManagementDtos.MONTH) String period) {
        return service.mine(CurrentUser.of(principal), period);
    }
}
