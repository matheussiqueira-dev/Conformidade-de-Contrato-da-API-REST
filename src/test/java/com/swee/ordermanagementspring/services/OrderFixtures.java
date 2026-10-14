package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.*;
import java.time.LocalDate;
import java.util.List;

final class OrderFixtures {
    static OrderRequestDTO orderRequest(Long productId, Integer quantity) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setCustomer(individualCustomer());
        request.setItems(List.of(item(productId, quantity)));
        request.setPayment(pixPayment());
        request.setShippingAddress(address());
        return request;
    }

    private static CustomerRequestDTO individualCustomer() {
        CustomerRequestDTO customer = new CustomerRequestDTO();
        customer.setType("INDIVIDUAL");
        customer.setName("Marina Costa");
        customer.setEmail("marina@example.com");
        customer.setBirthDate(LocalDate.of(1995, 4, 12));
        customer.setCpf("12345678901");
        return customer;
    }

    static OrderItemRequestDTO item(Long productId, Integer quantity) {
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private static OrderPaymentRequestDTO pixPayment() {
        OrderPaymentRequestDTO payment = new OrderPaymentRequestDTO();
        payment.setType("PIX");
        payment.setAmount(200.0);
        payment.setPixKey("marina@example.com");
        payment.setPixHolderName("Marina Costa");
        return payment;
    }

    private static AddressRequestDTO address() {
        AddressRequestDTO address = new AddressRequestDTO();
        address.setStreet("Rua das Flores");
        address.setNumber("10");
        address.setNeighborhood("Boa Viagem");
        address.setCity("Recife");
        address.setState("PE");
        address.setZipCode("50000000");
        return address;
    }
}
