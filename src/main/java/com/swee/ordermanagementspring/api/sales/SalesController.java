package com.swee.ordermanagementspring.api.sales;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.api.sales.SalesDtos.*;
import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/** PDV (contrato v4): catalogo, cotacao e vendas. Controller fino; regras no SalesService. */
@RestController
@RequestMapping("/sales")
public class SalesController {
    private final SalesService service;
    private final RequestHash hash;

    public SalesController(SalesService service, RequestHash hash) {
        this.service = service;
        this.hash = hash;
    }

    @GetMapping("/catalog")
    public PageResponse<CatalogItem> catalog(@RequestParam(defaultValue = "") @Size(max = 120) String q,
                                             @RequestParam(defaultValue = "true") boolean onlyAvailable,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.catalog(q, onlyAvailable, page, size);
    }

    @PostMapping("/quote")
    public QuoteResponse quote(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody QuoteRequest request) {
        return service.quote(CurrentUser.of(principal), request);
    }

    @PostMapping("/orders")
    @ApiResponse(responseCode = "201", description = "Pedido criado")
    @ApiResponse(responseCode = "200", description = "Repeticao com a mesma Idempotency-Key e o mesmo corpo")
    public ResponseEntity<SalesOrderResponse> create(@AuthenticationPrincipal AppPrincipal principal,
                                                     @RequestHeader("Idempotency-Key") UUID key,
                                                     @Valid @RequestBody SalesOrderRequest request) {
        CurrentUser user = CurrentUser.of(principal);
        String requestHash = hash.of(request);
        var existing = service.existing(user, key, requestHash);
        if (existing.isPresent()) return ResponseEntity.ok(service.get(user, existing.get().orderId()));
        SalesService.Created created;
        try {
            created = service.create(user, key, requestHash, request);
        } catch (DataIntegrityViolationException race) {
            // Duas requisicoes simultaneas com a mesma chave: a perdedora le o pedido da vencedora (regra 8).
            var winner = service.existing(user, key, requestHash);
            if (winner.isEmpty()) throw race;
            return ResponseEntity.ok(service.get(user, winner.get().orderId()));
        }
        return ResponseEntity.created(URI.create("/sales/orders/" + created.orderId()))
                .body(service.get(user, created.orderId()));
    }

    @GetMapping("/orders")
    public PageResponse<SalesOrderSummary> list(@AuthenticationPrincipal AppPrincipal principal,
                                                @RequestParam(required = false) OrderStatus status,
                                                @RequestParam(required = false) @Positive Long sellerId,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(CurrentUser.of(principal), status == null ? null : status.name(), sellerId, page, size);
    }

    @GetMapping("/orders/{id}")
    public SalesOrderResponse get(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id) {
        return service.get(CurrentUser.of(principal), id);
    }

    @PostMapping("/orders/{id}/payment")
    public SalesOrderResponse confirmPayment(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                             @Valid @RequestBody(required = false) PaymentConfirmationRequest request) {
        return service.confirmPayment(CurrentUser.of(principal), id, request == null ? null : request.authorizationCode());
    }

    /** G-04: GERENTE aplica no pedido pendente o desconto acima da alcada do vendedor. */
    @PostMapping("/orders/{id}/discount-approval")
    public SalesOrderResponse approveDiscount(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                              @Valid @RequestBody ManualDiscountRequest request) {
        return service.approveDiscount(CurrentUser.of(principal), id, request);
    }
}
