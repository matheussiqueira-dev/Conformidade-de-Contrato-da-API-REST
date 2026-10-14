package com.swee.ordermanagementspring.api.management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swee.ordermanagementspring.entities.StoreTime;
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

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Estorno (C-07, BD-22, BD-23) e analises mensais e de estoque sobre as views do V6. */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class RefundAndAnalyticsApiTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate template;
    @Autowired JdbcClient jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private PdvFixtures fx;
    private long mouse;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fx = new PdvFixtures(template, jdbc);
        mouse = fx.product("MOU-001", "Mouse", "100.00", 10, "60.00");
    }

    private MvcResult call(MockHttpServletRequestBuilder request, long actor) throws Exception {
        return mvc.perform(request.with(user(fx.principal(actor))).with(csrf())).andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        assertThat(body(result).get("code").asText()).isEqualTo(code);
    }

    private long sale(String items) throws Exception {
        MvcResult result = call(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":%d,"items":[%s],"payment":{"type":"PIX"},"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, items)), fx.allan);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        return body(result).get("id").asLong();
    }

    private long paymentOf(long order) {
        return fx.count("SELECT id FROM payment WHERE order_id = ?", order);
    }

    private MvcResult refund(long payment, String amount) throws Exception {
        return call(post("/payment/" + payment + "/refund").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":{\"amount\":%s,\"currency\":\"BRL\"},\"reason\":\"Produto com defeito\"}".formatted(amount)),
                fx.manager);
    }

    @Test
    @DisplayName("[BD-22, BD-23, C-07] estorno parcial 201; acima do saldo 400; pagamento pendente 409; desconta no realizado")
    void refunds() throws Exception {
        long order = sale("{\"productId\":%d,\"quantity\":2}".formatted(mouse));
        long payment = paymentOf(order);
        assertError(refund(payment, "10.00"), 409, "STATE_CONFLICT");
        call(post("/sales/orders/" + order + "/payment"), fx.allan);

        MvcResult first = refund(payment, "50.00");
        assertThat(first.getResponse().getStatus()).as(first.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode created = body(first);
        assertThat(first.getResponse().getHeader("Location")).isEqualTo("/payment/" + payment + "/refunds/" + created.get("id").asLong());
        assertThat(created.get("refundedTotal").get("amount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(created.get("remaining").get("amount").decimalValue()).isEqualByComparingTo("150.00");
        assertThat(created.get("createdBy").get("id").asLong()).isEqualTo(fx.manager);

        assertError(refund(payment, "150.01"), 400, "VALIDATION_FAILED");
        assertThat(body(refund(payment, "150.00")).get("remaining").get("amount").decimalValue()).isEqualByComparingTo("0.00");
        assertError(refund(payment, "0.01"), 400, "VALIDATION_FAILED");
        assertError(refund(999_999, "1.00"), 404, "NOT_FOUND");
        assertError(call(post("/payment/" + payment + "/refund").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":{\"amount\":1,\"currency\":\"BRL\"},\"reason\":\"x\"}"), fx.manager), 400, "VALIDATION_FAILED");
        assertError(call(post("/payment/" + payment + "/refund").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":{\"amount\":1,\"currency\":\"BRL\"},\"reason\":\"Defeito\"}"), fx.allan), 403, "FORBIDDEN");

        BigDecimal realized = jdbc.sql("SELECT coalesce(sum(valor), 0) FROM v_lancamento_realizado").query(BigDecimal.class).single();
        assertThat(realized).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("[RF-23] analise do mes: semanas, formas de pagamento, margem por produto e uso de promocoes")
    void salesAnalytics() throws Exception {
        long cable = fx.product("CAB-001", "Cabo", "20.00", 10, null);
        long promo = jdbc.sql("""
                INSERT INTO promotion(name, kind, value, product_id, starts_at, ends_at, created_by)
                VALUES ('Semana do mouse', 'AMOUNT', 10.00, :p, now() - interval '1 day', now() + interval '1 day', :m) RETURNING id""")
                .param("p", mouse).param("m", fx.manager).query(Long.class).single();
        long order = sale("{\"productId\":%d,\"quantity\":1,\"promotionId\":%d},{\"productId\":%d,\"quantity\":2}".formatted(mouse, promo, cable));
        call(post("/sales/orders/" + order + "/payment"), fx.allan);
        String month = YearMonth.from(StoreTime.today()).toString();

        MvcResult result = call(get("/analytics/sales").param("month", month), fx.manager);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        JsonNode analytics = body(result);
        assertThat(analytics.get("month").asText()).isEqualTo(month);
        assertThat(analytics.get("weekly")).hasSize(1);
        assertThat(analytics.get("weekly").get(0).get("realized").get("amount").decimalValue()).isEqualByComparingTo("130.00");
        assertThat(analytics.get("byPaymentMethod").get(0).get("method").asText()).isEqualTo("PIX");
        JsonNode margins = analytics.get("productMargins");
        assertThat(margins.get(0).get("name").asText()).isEqualTo("Mouse");
        assertThat(margins.get(0).get("grossMargin").get("amount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(margins.get(1).get("cost").isNull()).isTrue();
        assertThat(margins.get(1).get("itemsWithoutCost").asLong()).isEqualTo(1);
        JsonNode usage = analytics.get("promotionUsage").get(0);
        assertThat(usage.get("promotionId").asLong()).isEqualTo(promo);
        assertThat(usage.get("discountTotal").get("amount").decimalValue()).isEqualByComparingTo("10.00");

        assertThat(body(call(get("/analytics/sales").param("month", "2020-01"), fx.manager)).get("weekly")).isEmpty();
        assertThat(call(get("/analytics/sales").param("month", "2026-1"), fx.manager).getResponse().getStatus()).isEqualTo(400);
        assertError(call(get("/analytics/sales").param("month", month), fx.allan), 403, "FORBIDDEN");
    }

    @Test
    @DisplayName("[RF-22] analise de estoque: valor a custo nulo quando falta custo; itens abaixo do minimo")
    void stockAnalytics() throws Exception {
        JsonNode known = body(call(get("/analytics/stock"), fx.manager));
        assertThat(known.get("products").asLong()).isEqualTo(1);
        assertThat(known.get("units").asLong()).isEqualTo(10);
        assertThat(known.get("valueAtCost").get("amount").decimalValue()).isEqualByComparingTo("600.00");
        assertThat(known.get("valueAtPrice").get("amount").decimalValue()).isEqualByComparingTo("1000.00");

        long cable = fx.product("CAB-001", "Cabo", "20.00", 2, null);
        fx.set("UPDATE stock_balance SET min_level = 5 WHERE product_id = ?", cable);
        JsonNode unknown = body(call(get("/analytics/stock"), fx.manager));
        assertThat(unknown.get("valueAtCost").isNull()).isTrue();
        assertThat(unknown.get("productsWithoutCost").asLong()).isEqualTo(1);
        assertThat(unknown.get("belowMin")).hasSize(1);
        assertThat(unknown.get("belowMin").get(0).get("sku").asText()).isEqualTo("CAB-001");
        assertError(call(get("/analytics/stock"), fx.allan), 403, "FORBIDDEN");
    }
}
