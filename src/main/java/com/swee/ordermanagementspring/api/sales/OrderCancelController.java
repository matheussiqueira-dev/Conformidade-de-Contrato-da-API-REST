package com.swee.ordermanagementspring.api.sales;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.sales.SalesDtos.SalesOrderResponse;
import com.swee.ordermanagementspring.security.AppPrincipal;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /orders/{id}/cancel (v3/v4): VENDEDOR cancela a propria venda pendente; GERENTE qualquer uma (DEC-ERP-06). */
@RestController
public class OrderCancelController {
    private final SalesService service;

    public OrderCancelController(SalesService service) {
        this.service = service;
    }

    @PostMapping("/orders/{id}/cancel")
    public SalesOrderResponse cancel(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id) {
        return service.cancel(CurrentUser.of(principal), id);
    }
}
