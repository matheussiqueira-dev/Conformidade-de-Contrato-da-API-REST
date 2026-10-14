package com.swee.ordermanagementspring.repositories;

import com.swee.ordermanagementspring.entities.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
