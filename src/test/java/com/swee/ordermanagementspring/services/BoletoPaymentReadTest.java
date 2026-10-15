package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.support.PdvFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Pagamento por boleto gravado no banco precisa ser lido pelas listagens legadas sem erro 500. */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
@Tag("regression")
class BoletoPaymentReadTest {

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
    @DisplayName("GET /payment, /payment/{id} e /orders leem pagamento por boleto")
    void legacyListingsReadBoletoPayment() throws Exception {
        long product = fx.product("BOL-001", "Cabo HDMI", "80.00", 5, "30.00");
        long order = jdbc.sql("INSERT INTO orders(moment, status, customer_id, fulfillment) VALUES (now(), 'PAID', :c, 'PICKUP') RETURNING id")
                .param("c", fx.customer).query(Long.class).single();
        jdbc.sql("INSERT INTO order_item(quantity, price, order_id, product_id) VALUES (1, 80.00, :o, :p)")
                .param("o", order).param("p", product).update();
        long payment = jdbc.sql("""
                INSERT INTO payment(payment_type, amount, status, payment_date, order_id, bar_code, due_date)
                VALUES ('BOLETO', 80.00, 'APPROVED', now(), :o, '34191790010104351004791020150008291070026000', current_date + 3)
                RETURNING id""").param("o", order).query(Long.class).single();

        for (String path : new String[] {"/payment", "/payment/" + payment, "/orders"}) {
            MvcResult result = mvc.perform(get(path).with(user(fx.principal(fx.manager)))).andReturn();
            assertThat(result.getResponse().getStatus()).as(path + " " + result.getResponse().getContentAsString()).isEqualTo(200);
        }
    }
}
