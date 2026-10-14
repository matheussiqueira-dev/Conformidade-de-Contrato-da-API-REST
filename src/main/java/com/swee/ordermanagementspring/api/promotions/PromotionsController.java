package com.swee.ordermanagementspring.api.promotions;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.api.promotions.PromotionsDtos.Promotion;
import com.swee.ordermanagementspring.api.promotions.PromotionsDtos.PromotionRequest;
import com.swee.ordermanagementspring.api.sales.SalesDtos.ActivePromotion;
import com.swee.ordermanagementspring.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** Promocoes do contrato v4: vigentes para o balcao; listagem, liberacao e encerramento para o GERENTE. */
@RestController
@RequestMapping("/promotions")
public class PromotionsController {
    private final PromotionsService service;

    public PromotionsController(PromotionsService service) {
        this.service = service;
    }

    @GetMapping("/active")
    public List<ActivePromotion> active(@RequestParam(required = false) @Positive Long productId) {
        return service.active(productId);
    }

    @GetMapping
    public PageResponse<Promotion> list(@RequestParam(required = false) Boolean active,
                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(active, page, size);
    }

    @PostMapping
    public ResponseEntity<Promotion> create(@AuthenticationPrincipal AppPrincipal principal,
                                            @Valid @RequestBody PromotionRequest request) {
        Promotion created = service.create(CurrentUser.of(principal), request);
        return ResponseEntity.created(URI.create("/promotions/" + created.id())).body(created);
    }

    @PostMapping("/{id}/close")
    public Promotion close(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id) {
        return service.close(CurrentUser.of(principal), id);
    }
}
