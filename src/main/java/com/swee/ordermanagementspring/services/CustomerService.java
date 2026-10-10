package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.CustomerRequestDTO;
import com.swee.ordermanagementspring.entities.Address;
import com.swee.ordermanagementspring.entities.customer.Customer;
import com.swee.ordermanagementspring.entities.customer.CorporateCustomer;
import com.swee.ordermanagementspring.entities.customer.IndividualCustomer;
import com.swee.ordermanagementspring.exceptions.ProductException;
import com.swee.ordermanagementspring.exceptions.ResourceNotFoundException;
import com.swee.ordermanagementspring.repositories.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    public CustomerService (CustomerRepository repository){
        this.repository = repository;
    }

    public List<Customer> findAll() {
        return repository.findAll();
    }

    public Customer findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    public Customer updateAddress(Long id, Address address) {
        Customer customer = findById(id);

        customer.setAddress(address);

        return repository.save(customer);
    }

    public Customer update(Long id, CustomerRequestDTO dto) {
        Customer existing = findById(id);

        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        existing.setBirthDate(dto.getBirthDate());

        if (existing instanceof IndividualCustomer individual && dto.getCpf() != null) {
            individual.setCpf(dto.getCpf());
        }

        if (existing instanceof CorporateCustomer corporate) {
            if (dto.getCnpj() != null) {
                corporate.setCnpj(dto.getCnpj());
            }
            if (dto.getCompanyName() != null) {
                corporate.setCompanyName(dto.getCompanyName());
            }
        }

        return repository.save(existing);
    }

    public Customer insert(CustomerRequestDTO dto) {
        Customer customer = buildCustomer(dto);
        return repository.save(customer);
    }

    private Customer buildCustomer(CustomerRequestDTO dto) {
        return switch (dto.getType().toUpperCase()) {
            case "INDIVIDUAL" -> {
                if (dto.getCpf() == null || dto.getCpf().isBlank()) {
                    throw new ProductException("cpf é obrigatório para cliente INDIVIDUAL");
                }
                yield new IndividualCustomer(dto.getName(), dto.getEmail(), dto.getBirthDate(), dto.getCpf());
            }
            case "CORPORATE" -> {
                if (dto.getCnpj() == null || dto.getCnpj().isBlank()) {
                    throw new ProductException("cnpj é obrigatório para cliente CORPORATE");
                }
                yield new CorporateCustomer(dto.getName(), dto.getEmail(), dto.getBirthDate(), dto.getCnpj(), dto.getCompanyName());
            }
            default -> throw new ProductException("Tipo de cliente inválido: " + dto.getType());
        };
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}