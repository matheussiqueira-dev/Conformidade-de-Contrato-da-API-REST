package com.swee.ordermanagementspring.services;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** [D022] Reproducao do defeito: o POST /payment legado com cartao nao grava o numero completo. */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
@Tag("regression")
class CardNumberStorageTest {

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

    @Test
    @DisplayName("[D022] POST /payment com cartao grava so os 4 ultimos digitos e a resposta continua mascarada")
    void legacyCardPaymentStoresOnlyLastFourDigits() throws Exception {
        long product = fx.product("CAB-001", "Cabo USB", "50.00", 5, "20.00");
        long order = jdbc.sql("INSERT INTO orders(moment, status, customer_id, fulfillment) VALUES (now(), 'PENDING_PAYMENT', :c, 'PICKUP') RETURNING id")
                .param("c", fx.customer).query(Long.class).single();
        jdbc.sql("INSERT INTO order_item(quantity, price, order_id, product_id) VALUES (1, 50.00, :o, :p)")
                .param("o", order).param("p", product).update();

        MvcResult result = mvc.perform(post("/payment").with(user(fx.principal(fx.manager))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"CARD","amount":50.00,"orderId":%d,"cardNumber":"4111 1111 1111 1111",
                         "cardHolder":"MARINA COSTA","installments":1}""".formatted(order))).andReturn();

        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isIn(200, 201);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("4111111111111111").contains("1111");
        assertThat(jdbc.sql("SELECT coalesce(card_number, '-') || '|' || card_last4 FROM payment WHERE order_id = :o")
                .param("o", order).query(String.class).single()).isEqualTo("-|1111");
    }
}
