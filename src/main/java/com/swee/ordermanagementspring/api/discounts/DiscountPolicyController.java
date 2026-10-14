package com.swee.ordermanagementspring.api.discounts;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.discounts.DiscountDtos.DiscountPolicy;
import com.swee.ordermanagementspring.api.discounts.DiscountDtos.DiscountPolicyRequest;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.security.AppPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Alcadas de desconto manual por papel (contrato v5, G-04). */
@RestController
@RequestMapping("/discount-policy")
public class DiscountPolicyController {
    private final DiscountPolicyService service;

    public DiscountPolicyController(DiscountPolicyService service) {
        this.service = service;
    }

    @GetMapping
    public List<DiscountPolicy> list() {
        return service.list();
    }

    @PutMapping("/{role}")
    public DiscountPolicy update(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UserRole role,
                                 @Valid @RequestBody DiscountPolicyRequest request) {
        return service.update(CurrentUser.of(principal), role, request);
    }
}
