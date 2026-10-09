package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.OrderRequestDTO;
import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.entities.product.PhysicalProduct;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import com.swee.ordermanagementspring.security.AppPrincipal;
import com.swee.ordermanagementspring.repositories.OrderRepository;
import com.swee.ordermanagementspring.repositories.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.swee.ordermanagementspring.services.OrderFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
@Transactional
class OrderPersistenceTest {
    @Autowired OrderService orderService;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired EntityManager entityManager;
    @Autowired AppUserRepository users;

    private AppPrincipal actor() {
        AppUser user = users.save(new AppUser("Vendedor de teste", "seller-" + java.util.UUID.randomUUID() + "@example.test",
                "synthetic-hash", UserRole.VENDEDOR));
        return new AppPrincipal(user);
    }

    @Test
    void historicalUnitPriceSurvivesCatalogChangeAndDatabaseReload() {
        PhysicalProduct product = products.save(new PhysicalProduct(new BigDecimal("100.0"), "Mouse", "Fixture sintetica", 0.2));
        AppPrincipal seller = actor();
        Order saved = orderService.insert(orderRequest(product.getId(), 2), seller);
        entityManager.flush();
        Long orderId = saved.getId();
        product.setPrice(new BigDecimal("150.0"));
        entityManager.flush();
        entityManager.clear();

        Order reloaded = orders.findById(orderId).orElseThrow();

        assertThat(reloaded.getItems().getFirst().getProduct().getPrice()).isEqualByComparingTo("150.0");
        assertThat(reloaded.getItems().getFirst().getPrice()).isEqualByComparingTo("100.0");
        assertThat(reloaded.getItems().getFirst().getQuantity()).isEqualTo(2);
        assertThat(reloaded.total()).isEqualByComparingTo("200.0");
        assertThat(reloaded.getSeller().getId()).isEqualTo(seller.id());
    }

    @Test
    void demonstrationSalePersistsClientAddressPaymentAndTotal() {
        PhysicalProduct phone = products.save(new PhysicalProduct(new BigDecimal("4500.0"), "iPhone", "Fixture sintetica", 0.2));
        PhysicalProduct earbuds = products.save(new PhysicalProduct(new BigDecimal("1200.0"), "AirPods", "Fixture sintetica", 0.1));
        OrderRequestDTO request = orderRequest(phone.getId(), 1);
        request.setItems(List.of(item(phone.getId(), 1), item(earbuds.getId(), 1)));
        request.getPayment().setAmount(new BigDecimal("5700.0"));
        AppPrincipal seller = actor();
        Long orderId = orderService.insert(request, seller).getId();
        entityManager.flush();
        entityManager.clear();

        Order reloaded = orders.findById(orderId).orElseThrow();

        assertThat(reloaded.total()).isEqualByComparingTo("5700.0");
        assertThat(reloaded.getClient().getId()).isNotNull();
        assertThat(reloaded.getShippingAddress().getId()).isNotNull();
        assertThat(reloaded.getPayment().getId()).isNotNull();
        assertThat(reloaded.getPayment().getAmount()).isEqualByComparingTo("5700.0");
        assertThat(reloaded.getSeller().getId()).isEqualTo(seller.id());
    }
}
