package com.swee.ordermanagementspring.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import com.swee.ordermanagementspring.dto.CustomerResponseDTO;
import com.swee.ordermanagementspring.entities.Address;
import com.swee.ordermanagementspring.services.CustomerService;
import org.springframework.web.bind.annotation.*;

// Rota legada que sobrou do /customers: o restante passou para api/customers (contrato v4, D-NOITE-05).
@PreAuthorize("hasRole('GERENTE')")
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;
    public CustomerController (CustomerService service) {
        this.service = service;
    }

    @PostMapping("/{id}/address")
    public CustomerResponseDTO updateAddress(
            @PathVariable Long id,
            @RequestBody Address address) {

        return CustomerResponseDTO.from(service.updateAddress(id, address));
    }
}