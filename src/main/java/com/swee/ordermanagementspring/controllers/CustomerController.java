package com.swee.ordermanagementspring.controllers;

import com.swee.ordermanagementspring.dto.CustomerRequestDTO;
import com.swee.ordermanagementspring.dto.CustomerResponseDTO;
import com.swee.ordermanagementspring.entities.Address;
import com.swee.ordermanagementspring.entities.customer.Customer;
import com.swee.ordermanagementspring.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;
    public CustomerController (CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerResponseDTO> findAll() {
        return service.findAll().stream()
                .map(CustomerResponseDTO::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CustomerResponseDTO findById(@PathVariable Long id) {
        return CustomerResponseDTO.from(service.findById(id));
    }

    @PostMapping("/{id}/address")
    public CustomerResponseDTO updateAddress(
            @PathVariable Long id,
            @RequestBody Address address) {

        return CustomerResponseDTO.from(service.updateAddress(id, address));
    }

    @PostMapping
    public CustomerResponseDTO insert(@Valid @RequestBody CustomerRequestDTO dto) {
        return CustomerResponseDTO.from(service.insert(dto));
    }

    @PutMapping("/{id}")
    public CustomerResponseDTO update(@PathVariable Long id, @Valid @RequestBody CustomerRequestDTO dto) {
        return CustomerResponseDTO.from(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }
}