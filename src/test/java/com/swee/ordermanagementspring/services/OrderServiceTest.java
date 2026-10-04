package com.swee.ordermanagementspring.services;

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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static com.swee.ordermanagementspring.services.OrderFixtures.*;
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

    @Test
    void insertCalculatesDemonstrationSaleTotal() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(
                new PhysicalProduct(4500.0, "iPhone 128 GB Preto", "Fixture sintetica", 0.2)));
        when(productRepository.findById(2L)).thenReturn(Optional.of(
                new PhysicalProduct(1200.0, "AirPods Branco", "Fixture sintetica", 0.1)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrderRequestDTO request = orderRequest(1L, 1);
        request.setItems(List.of(item(1L, 1), item(2L, 1)));
        request.getPayment().setAmount(5700.0);

        Order saved = orderService.insert(request);

        assertThat(saved.total()).isEqualTo(5700.0);
        assertThat(saved.getItems()).extracting(i -> i.getPrice()).containsExactly(4500.0, 1200.0);
    }

    @Test
    void catalogPriceChangeDoesNotChangeExistingItemPriceInMemory() {
        PhysicalProduct product = new PhysicalProduct(100.0, "Mouse", "Fixture sintetica", 0.2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Order saved = orderService.insert(orderRequest(1L, 2));

        product.setPrice(150.0);

        assertThat(saved.getItems().getFirst().getPrice()).isEqualTo(100.0);
        assertThat(saved.total()).isEqualTo(200.0);
    }

}
