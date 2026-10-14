package com.swee.ordermanagementspring.entities.customer;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("INDIVIDUAL")
public class IndividualCustomer extends Customer{

    @Column(unique = true)
    private String cpf;

    public IndividualCustomer() {

    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public IndividualCustomer(String name, String email, LocalDate birthDate, String cpf) {
        super(name, email, birthDate);
        this.cpf = cpf;


    }
}
