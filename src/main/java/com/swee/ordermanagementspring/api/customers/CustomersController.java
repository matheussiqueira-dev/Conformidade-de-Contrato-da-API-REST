package com.swee.ordermanagementspring.api.customers;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.api.customers.CustomerDtos.*;
import com.swee.ordermanagementspring.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Clientes do contrato v4 (substitui o /customers legado, que expunha o documento completo: D-NOITE-05). */
@RestController
@RequestMapping("/customers")
public class CustomersController {
    private final CustomersService service;

    public CustomersController(CustomersService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CustomerSummary> search(@AuthenticationPrincipal AppPrincipal principal,
                                                @RequestParam(defaultValue = "") @Size(max = 120) String q,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(CurrentUser.of(principal), q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CustomerDetail> create(@AuthenticationPrincipal AppPrincipal principal,
                                                 @Valid @RequestBody CustomerCreateRequest request) {
        CustomerDetail created = service.create(CurrentUser.of(principal), request);
        return ResponseEntity.created(URI.create("/customers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public CustomerDetail get(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id) {
        return service.get(CurrentUser.of(principal), id);
    }

    @PutMapping("/{id}")
    public CustomerDetail update(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                 @Valid @RequestBody CustomerUpdateRequest request) {
        return service.update(CurrentUser.of(principal), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id) {
        service.delete(CurrentUser.of(principal), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/summary")
    public CustomerPurchaseSummary summary(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id) {
        return service.summary(CurrentUser.of(principal), id);
    }

    @GetMapping("/{id}/orders")
    public PageResponse<CustomerOrderSummary> orders(@AuthenticationPrincipal AppPrincipal principal,
                                                     @PathVariable @Positive long id,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.history(CurrentUser.of(principal), id, page, size);
    }
}
