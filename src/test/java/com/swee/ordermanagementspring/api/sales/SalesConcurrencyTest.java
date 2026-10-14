package com.swee.ordermanagementspring.api.sales;

import com.swee.ordermanagementspring.support.PdvFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Corridas reais no PostgreSQL (transacoes confirmadas): ultima unidade (CT-STOCK-104, equivalente Java do CC-01),
 * mesma chave de idempotencia (CT-SALE-103) e itens em ordem inversa (CT-STOCK-105). Cada corrida e repetida
 * porque o intervalo entre leitura e escrita e curto.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class SalesConcurrencyTest {

    private static final int ROUNDS = 10;

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate template;
    @Autowired JdbcClient jdbc;

    private MockMvc mvc;
    private PdvFixtures fx;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fx = new PdvFixtures(template, jdbc);
    }

    private MockHttpServletRequestBuilder sale(long seller, UUID key, String items) {
        return post("/sales/orders").header("Idempotency-Key", key.toString()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerId":%d,"items":[%s],"payment":{"type":"PIX"},"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, items))
                .with(user(fx.principal(seller))).with(csrf());
    }

    private static String item(long product, int quantity) {
        return "{\"productId\":%d,\"quantity\":%d}".formatted(product, quantity);
    }

    /** Dispara as requisicoes ao mesmo tempo e devolve as respostas na ordem de envio. */
    private List<MvcResult> race(List<MockHttpServletRequestBuilder> requests) throws Exception {
        CyclicBarrier start = new CyclicBarrier(requests.size());
        ExecutorService pool = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<MvcResult>> running = new ArrayList<>();
            for (MockHttpServletRequestBuilder request : requests) {
                Callable<MvcResult> call = () -> {
                    start.await(10, TimeUnit.SECONDS);
                    return mvc.perform(request).andReturn();
                };
                running.add(pool.submit(call));
            }
            List<MvcResult> results = new ArrayList<>();
            for (Future<MvcResult> result : running) results.add(result.get(30, TimeUnit.SECONDS));
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static List<Integer> statuses(List<MvcResult> results) {
        return results.stream().map(r -> r.getResponse().getStatus()).sorted().toList();
    }

    @Test
    @DisplayName("[CT-STOCK-104] duas vendas da ultima unidade: uma 201, outra 409 STOCK_INSUFFICIENT, saldo nunca negativo")
    void lastUnitIsSoldOnce() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            long product = fx.product("ULT-" + round, "Ultima unidade " + round, "10.00", 1, "5.00");
            List<MvcResult> results = race(List.of(sale(fx.allan, UUID.randomUUID(), item(product, 1)),
                    sale(fx.francisco, UUID.randomUUID(), item(product, 1))));

            assertThat(statuses(results)).as("rodada %d", round).containsExactly(201, 409);
            MvcResult loser = results.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
            assertThat(loser.getResponse().getContentAsString()).contains("STOCK_INSUFFICIENT");
            assertThat(fx.balance(product)).isEqualTo("1/1");
            assertThat(fx.count("SELECT count(*) FROM order_item WHERE product_id = ?", product)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("[CT-SALE-103] mesma chave enviada duas vezes ao mesmo tempo cria um unico pedido e uma unica reserva")
    void concurrentRepeatCreatesOneOrder() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            long product = fx.product("IDP-" + round, "Idempotente " + round, "10.00", 5, "5.00");
            UUID key = UUID.randomUUID();
            List<MvcResult> results = race(List.of(sale(fx.allan, key, item(product, 2)), sale(fx.allan, key, item(product, 2))));

            assertThat(statuses(results)).as("rodada %d", round).containsExactly(200, 201);
            assertThat(results.get(0).getResponse().getContentAsString())
                    .contains("\"id\":" + fx.count("SELECT id FROM orders WHERE client_request_id = ?", key));
            assertThat(fx.count("SELECT count(*) FROM orders WHERE client_request_id = ?", key)).isEqualTo(1);
            assertThat(fx.balance(product)).isEqualTo("5/2");
        }
    }

    @Test
    @DisplayName("[CT-STOCK-105] vendas, confirmacoes e cancelamentos com itens em ordem inversa nao travam")
    void inverseItemOrderDoesNotDeadlock() throws Exception {
        long a = fx.product("DLK-A", "Produto A", "10.00", 1_000, "5.00");
        long b = fx.product("DLK-B", "Produto B", "20.00", 1_000, "5.00");
        for (int round = 1; round <= ROUNDS; round++) {
            List<MvcResult> created = race(List.of(sale(fx.allan, UUID.randomUUID(), item(a, 1) + "," + item(b, 1)),
                    sale(fx.francisco, UUID.randomUUID(), item(b, 1) + "," + item(a, 1))));
            assertThat(statuses(created)).as("rodada %d", round).containsExactly(201, 201);

            long first = fx.count("SELECT max(id) FROM orders WHERE seller_id = ?", fx.allan);
            long second = fx.count("SELECT max(id) FROM orders WHERE seller_id = ?", fx.francisco);
            List<MvcResult> settled = race(List.of(
                    post("/sales/orders/" + first + "/payment").with(user(fx.principal(fx.allan))).with(csrf()),
                    post("/orders/" + second + "/cancel").with(user(fx.principal(fx.francisco))).with(csrf())));
            assertThat(statuses(settled)).as("rodada %d", round).containsExactly(200, 200);
        }
        assertThat(fx.balance(a)).isEqualTo((1_000 - ROUNDS) + "/0");
        assertThat(fx.balance(b)).isEqualTo((1_000 - ROUNDS) + "/0");
    }
}
