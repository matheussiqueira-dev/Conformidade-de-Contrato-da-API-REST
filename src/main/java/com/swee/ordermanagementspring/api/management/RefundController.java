package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.management.ManagementDtos.RefundRequest;
import com.swee.ordermanagementspring.api.management.ManagementDtos.RefundResponse;
import com.swee.ordermanagementspring.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Estorno do contrato v4 (201 com RefundResponse, C-07). Somente GERENTE (URL e servico). */
@RestController
public class RefundController {
    private final RefundService service;

    public RefundController(RefundService service) {
        this.service = service;
    }

    @PostMapping("/payment/{id}/refund")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RefundResponse> refund(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                                 @Valid @RequestBody RefundRequest request) {
        RefundResponse created = service.refund(CurrentUser.of(principal), id, request);
        return ResponseEntity.created(URI.create("/payment/" + id + "/refunds/" + created.id())).body(created);
    }
}
