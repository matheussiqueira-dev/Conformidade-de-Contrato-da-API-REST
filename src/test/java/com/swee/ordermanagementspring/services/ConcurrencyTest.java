package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.OrderRequestDTO;
import com.swee.ordermanagementspring.dto.OrderStatusUpdateDTO;
import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.enums.PaymentStatus;
import com.swee.ordermanagementspring.entities.product.PhysicalProduct;
import com.swee.ordermanagementspring.exceptions.OrderException;
import com.swee.ordermanagementspring.exceptions.PaymentException;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import com.swee.ordermanagementspring.repositories.OrderRepository;
import com.swee.ordermanagementspring.repositories.PaymentRepository;
import com.swee.ordermanagementspring.repositories.ProductRepository;
import com.swee.ordermanagementspring.security.AppPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.swee.ordermanagementspring.services.OrderFixtures.orderRequest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * [D019] Duas requisicoes simultaneas sobre o mesmo pedido, com PostgreSQL real e transacoes confirmadas.
 * Sem bloqueio de linha, as duas leem o estado antigo e passam pela validacao; a corrida e repetida
 * algumas vezes porque o intervalo entre leitura e escrita e curto.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
@Tag("regression")
class ConcurrencyTest {

    private static final int ROUNDS = 10;

    @Autowired OrderService orderService;
    @Autowired PaymentService paymentService;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired AppUserRepository users;

    private Order newOrder() {
        PhysicalProduct product = products.save(new PhysicalProduct(new BigDecimal("100.00"), "Mouse", "Fixture sintetica", 0.2));
        AppUser seller = users.save(new AppUser("Vendedor de teste", "seller-" + UUID.randomUUID() + "@example.test",
                "synthetic-hash", UserRole.VENDEDOR));
        OrderRequestDTO request = orderRequest(product.getId(), 2);
        // Transacoes confirmadas: cada rodada precisa de um CPF sintetico proprio (constraint unica).
        request.getClient().setCpf(String.format("%011d", ThreadLocalRandom.current().nextLong(1L, 99_999_999_999L)));
        return orderService.insert(request, new AppPrincipal(seller));
    }

    private static OrderStatusUpdateDTO status(OrderStatus value) {
        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setStatus(value.name());
        return dto;
    }

    /** Executa as duas acoes ao mesmo tempo e devolve as excecoes lancadas. */
    private static List<Throwable> race(Runnable first, Runnable second) throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Object>> running = new ArrayList<>();
            for (Runnable action : List.of(first, second)) {
                running.add(pool.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    action.run();
                    return null;
                }));
            }
            List<Throwable> failures = new ArrayList<>();
            for (Future<Object> result : running) {
                try {
                    result.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
            return failures;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("[D019] dois /payment/{id}/process simultaneos aprovam o pagamento uma unica vez")
    void concurrentProcessingApprovesOnce() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            Order order = newOrder();
            Long paymentId = order.getPayment().getId();
            try {
                List<Throwable> failures = race(() -> paymentService.process(paymentId), () -> paymentService.process(paymentId));

                assertThat(failures).as("rodada %d: exatamente uma requisicao deve ser recusada", round).hasSize(1);
                assertThat(failures.getFirst()).isInstanceOf(PaymentException.class);
                assertThat(payments.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.APPROVED);
            } finally {
                orders.deleteById(order.getId());
            }
        }
    }

    @Test
    @DisplayName("[D019] SHIPPED e PAID simultaneos nao fazem o pedido voltar para PAID")
    void concurrentStatusUpdatesNeverMoveBack() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            Long orderId = newOrder().getId();
            try {
                orderService.updateStatus(orderId, status(OrderStatus.PROCESSING));

                List<Throwable> failures = race(() -> orderService.updateStatus(orderId, status(OrderStatus.SHIPPED)),
                        () -> orderService.updateStatus(orderId, status(OrderStatus.PAID)));

                assertThat(failures).allMatch(OrderException.class::isInstance);
                assertThat(orders.findById(orderId).orElseThrow().getStatus()).as("rodada %d", round).isEqualTo(OrderStatus.SHIPPED);
            } finally {
                orders.deleteById(orderId);
            }
        }
    }
}