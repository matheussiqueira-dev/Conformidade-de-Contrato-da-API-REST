package com.swee.ordermanagementspring.entities.customer;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("CORPORATE")
public class CorporateCustomer extends Customer{

    @Column(unique = true)
    private String cnpj;
    private String companyName;

    public CorporateCustomer() {
    }

    public CorporateCustomer(String name, String email, LocalDate birthDate, String cnpj, String companyName) {
        super(name, email, birthDate);
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
