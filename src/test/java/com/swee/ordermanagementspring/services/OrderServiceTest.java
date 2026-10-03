package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.AddressRequestDTO;
import com.swee.ordermanagementspring.dto.ClientRequestDTO;
import com.swee.ordermanagementspring.dto.OrderItemRequestDTO;
import com.swee.ordermanagementspring.dto.OrderPaymentRequestDTO;
import com.swee.ordermanagementspring.dto.OrderRequestDTO;
import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.entities.product.PhysicalProduct;
import com.swee.ordermanagementspring.repositories.ClientRepository;
import com.swee.ordermanagementspring.repositories.OrderRepository;
import com.swee.ordermanagementspring.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void insertKeepsItemPriceAsUnitPriceAndMultipliesQuantityOnce() {
        PhysicalProduct product = new PhysicalProduct(100.0, "Mouse", "Mouse gamer", 0.2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order saved = orderService.insert(orderRequest(1L, 2));

        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().getFirst().getPrice()).isEqualTo(100.0);
        assertThat(saved.total()).isEqualTo(200.0);
    }

    private static OrderRequestDTO orderRequest(Long productId, Integer quantity) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setClient(individualClient());
        request.setItems(List.of(item(productId, quantity)));
        request.setPayment(pixPayment());
        request.setShippingAddress(address());
        return request;
    }

    private static ClientRequestDTO individualClient() {
        ClientRequestDTO client = new ClientRequestDTO();
        client.setType("INDIVIDUAL");
        client.setName("Marina Costa");
        client.setEmail("marina@example.com");
        client.setBirthDate(LocalDate.of(1995, 4, 12));
        client.setCpf("12345678901");
        return client;
    }

    private static OrderItemRequestDTO item(Long productId, Integer quantity) {
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
