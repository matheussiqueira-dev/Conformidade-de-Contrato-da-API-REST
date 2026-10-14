package com.swee.ordermanagementspring.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swee.ordermanagementspring.entities.customer.Customer;
import com.swee.ordermanagementspring.entities.customer.CorporateCustomer;
import com.swee.ordermanagementspring.entities.customer.IndividualCustomer;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponseDTO {

    private Long id;
    private String type;
    private String name;
    private String email;
    private LocalDate birthDate;
    private String cpf;
    private String cnpj;
    private String companyName;

    public CustomerResponseDTO(Long id, String type, String name, String email, LocalDate birthDate,
                             String cpf, String cnpj, String companyName) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.email = email;
        this.birthDate = birthDate;
        this.cpf = cpf;
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    public static CustomerResponseDTO from(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO(
                customer.getId(),
                switch (customer) {
                    case IndividualCustomer i -> "INDIVIDUAL";
                    case CorporateCustomer c -> "CORPORATE";
                    default -> "UNKNOWN";
                },
                customer.getName(),
                customer.getEmail(),
                customer.getBirthDate(),
                null,
                null,
                null
        );

        if (customer instanceof IndividualCustomer individual) {
            dto.cpf = individual.getCpf();
        }

        if (customer instanceof CorporateCustomer corporate) {
            dto.cnpj = corporate.getCnpj();
            dto.companyName = corporate.getCompanyName();
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getCpf() {
        return cpf;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getCompanyName() {
        return companyName;
    }
}