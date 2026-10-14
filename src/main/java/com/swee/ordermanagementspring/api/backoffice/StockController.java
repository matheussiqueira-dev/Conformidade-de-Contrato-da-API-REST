package com.swee.ordermanagementspring.api.backoffice;

import com.swee.ordermanagementspring.api.backoffice.BackofficeDtos.*;
import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

/** Estoque do contrato v4: posicao, entradas, ajustes, minimo e extrato. Somente GERENTE (URL e servico). */
@RestController
@RequestMapping("/stock")
public class StockController {
    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public PageResponse<StockProduct> list(@RequestParam(defaultValue = "") @Size(max = 120) String q,
                                           @RequestParam(required = false) Boolean belowMin,
                                           @RequestParam(required = false) Boolean sellable,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(q, belowMin, sellable, page, size);
    }

    @PostMapping("/products/{id}/entries")
    public ResponseEntity<StockEntryResponse> entry(@AuthenticationPrincipal AppPrincipal principal,
                                                    @PathVariable @Positive long id,
                                                    @Valid @RequestBody StockEntryRequest request) {
        StockEntryResponse created = service.entry(CurrentUser.of(principal), id, request);
        return ResponseEntity.created(URI.create("/stock/products/" + id + "/entries/" + created.id())).body(created);
    }

    @PostMapping("/products/{id}/adjustments")
    public ResponseEntity<StockMovement> adjust(@AuthenticationPrincipal AppPrincipal principal,
                                                @PathVariable @Positive long id,
                                                @Valid @RequestBody StockAdjustmentRequest request) {
        StockMovement created = service.adjust(CurrentUser.of(principal), id, request);
        return ResponseEntity.created(URI.create("/stock/movements?productId=" + id)).body(created);
    }

    @PatchMapping("/products/{id}/min-level")
    public StockProduct changeMinLevel(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                       @Valid @RequestBody MinLevelRequest request) {
        return service.changeMinLevel(CurrentUser.of(principal), id, request);
    }

    @GetMapping("/movements")
    public PageResponse<StockMovement> movements(@RequestParam(required = false) @Positive Long productId,
                                                 @RequestParam(required = false) MovementKind kind,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.movements(productId, kind, from, to, page, size);
    }
}
