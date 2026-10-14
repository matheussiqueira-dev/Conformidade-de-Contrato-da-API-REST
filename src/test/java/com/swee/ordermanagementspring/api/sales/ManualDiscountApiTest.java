package com.swee.ordermanagementspring.api.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Desconto manual com alcada por papel (G-04): VENDEDOR 5%, GERENTE 20% (valores iniciais da V11). */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class ManualDiscountApiTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate template;
    @Autowired JdbcClient jdbc;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private PdvFixtures fx;
    private long headset;
    private long cheap;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fx = new PdvFixtures(template, jdbc);
        jdbc.sql("""
                INSERT INTO discount_policy(role, max_percent, allow_below_cost, updated_by) VALUES ('VENDEDOR', 5.00, false, NULL), ('GERENTE', 20.00, false, NULL)
                ON CONFLICT (role) DO UPDATE SET max_percent = EXCLUDED.max_percent, allow_below_cost = EXCLUDED.allow_below_cost""").update();
        headset = fx.product("FONE-01", "Fone Bluetooth", "100.00", 20, "40.00");
        cheap = fx.product("CABO-01", "Cabo curto", "10.00", 20, "9.80");
    }

    private MvcResult call(MockHttpServletRequestBuilder request, long actor) throws Exception {
        return mvc.perform(request.with(user(fx.principal(actor))).with(csrf())).andReturn();
    }

    private MvcResult send(MockHttpServletRequestBuilder request, long actor, String body) throws Exception {
        return call(request.contentType(MediaType.APPLICATION_JSON).content(body), actor);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        assertThat(body(result).get("code").asText()).isEqualTo(code);
    }

    private static String amount(JsonNode money) {
        return money.get("amount").decimalValue().setScale(2).toPlainString();
    }

    private static String sale(long customer, String items, String manual) {
        return """
                {"customerId":%d,"items":%s,"payment":{"type":"PIX"},"fulfillment":"PICKUP"%s}"""
                .formatted(customer, items, manual == null ? "" : ",\"manualDiscount\":" + manual);
    }

    private MvcResult order(long seller, String items, String manual) throws Exception {
        return send(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString()), seller, sale(fx.customer, items, manual));
    }

    private BigDecimal paymentAmount(long orderId) {
        return jdbc.sql("SELECT amount FROM payment WHERE order_id = :id").param("id", orderId).query(BigDecimal.class).single();
    }

    @Test
    @DisplayName("[CT-DISC-101] desconto dentro da alcada: cotacao e venda com o mesmo total, rateado nos itens e no pagamento")
    void discountWithinTheLimit() throws Exception {
        String items = "[{\"productId\":%d,\"quantity\":2}]".formatted(headset);
        String manual = "{\"percent\":5,\"reason\":\"Cliente fiel da loja\"}";
        JsonNode quote = body(send(post("/sales/quote"), fx.allan, "{\"items\":%s,\"manualDiscount\":%s}".formatted(items, manual)));
        assertThat(amount(quote.get("manualDiscount"))).isEqualTo("10.00");
        assertThat(amount(quote.get("discountTotal"))).isEqualTo("10.00");
        assertThat(amount(quote.get("total"))).isEqualTo("190.00");
        assertThat(quote.get("manualDiscountLimitPercent").decimalValue()).isEqualByComparingTo("5");
        assertThat(body(send(post("/sales/quote"), fx.allan, "{\"items\":%s}".formatted(items))).get("manualDiscount").isNull()).isTrue();

        MvcResult created = order(fx.allan, items, manual);
        assertThat(created.getResponse().getStatus()).as(created.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode sale = body(created);
        assertThat(amount(sale.get("total"))).isEqualTo("190.00");
        assertThat(sale.get("manualDiscount").get("percent").decimalValue()).isEqualByComparingTo("5");
        assertThat(amount(sale.get("manualDiscount").get("amount"))).isEqualTo("10.00");
        assertThat(sale.get("manualDiscount").get("approvedBy").isNull()).isTrue();
        assertThat(amount(sale.get("items").get(0).get("discount"))).isEqualTo("10.00");
        assertThat(paymentAmount(sale.get("id").asLong())).isEqualByComparingTo("190.00");
        assertThat(fx.count("SELECT manual_discount FROM order_item WHERE order_id = ?", sale.get("id").asLong())).isEqualTo(10);
    }

    @Test
    @DisplayName("[CT-DISC-102] acima da alcada: 403 DISCOUNT_ABOVE_LIMIT com o limite em details e nenhum pedido criado")
    void discountAboveTheLimitIsForbidden() throws Exception {
        String items = "[{\"productId\":%d,\"quantity\":1}]".formatted(headset);
        MvcResult seller = order(fx.allan, items, "{\"percent\":5.01,\"reason\":\"Pedido do cliente\"}");
        assertError(seller, 403, "DISCOUNT_ABOVE_LIMIT");
        assertThat(body(seller).get("details").toString()).contains("limitPercent=5.00");
        assertThat(fx.count("SELECT count(*) FROM orders")).isZero();
        assertThat(fx.available(headset)).isEqualTo(20);

        assertThat(order(fx.manager, items, "{\"percent\":20,\"reason\":\"Negociacao da gerencia\"}").getResponse().getStatus()).isEqualTo(201);
        assertError(send(post("/sales/quote"), fx.manager,
                "{\"items\":%s,\"manualDiscount\":{\"percent\":20.5,\"reason\":\"Acima da gerencia\"}}".formatted(items)), 403, "DISCOUNT_ABOVE_LIMIT");
    }

    @Test
    @DisplayName("[CT-DISC-103] item abaixo do custo: 409 BELOW_COST sem revelar o custo; a politica pode permitir")
    void belowCostIsRejectedWithoutRevealingCost() throws Exception {
        String items = "[{\"productId\":%d,\"quantity\":3}]".formatted(cheap);
        MvcResult rejected = order(fx.allan, items, "{\"percent\":5,\"reason\":\"Pedido do cliente\"}");
        assertError(rejected, 409, "BELOW_COST");
        String text = rejected.getResponse().getContentAsString();
        assertThat(text).doesNotContain("9.8", "9,8", "29.4", "avg", "Cost\":");
        assertThat(body(rejected).get("details").toString()).contains("productId=" + cheap);

        assertThat(send(put("/discount-policy/VENDEDOR"), fx.manager,
                "{\"maxPercent\":5,\"allowBelowCost\":true,\"reason\":\"Queima de estoque\"}").getResponse().getStatus()).isEqualTo(200);
        MvcResult accepted = order(fx.allan, items, "{\"percent\":5,\"reason\":\"Pedido do cliente\"}");
        assertThat(accepted.getResponse().getStatus()).isEqualTo(201);
        assertThat(amount(body(accepted).get("total"))).isEqualTo("28.50");
    }

    @Test
    @DisplayName("[CT-DISC-104] GERENTE aprova desconto acima da alcada no pedido pendente; total, pagamento e auditoria")
    void managerApprovesAboveTheSellerLimit() throws Exception {
        String items = "[{\"productId\":%d,\"quantity\":2}]".formatted(headset);
        JsonNode sale = body(order(fx.allan, items, "{\"percent\":5,\"reason\":\"Cliente fiel da loja\"}"));
        long id = sale.get("id").asLong();
        String approval = "{\"percent\":15,\"reason\":\"Aprovado pela gerencia\"}";
        assertError(send(post("/sales/orders/" + id + "/discount-approval"), fx.allan, approval), 403, "FORBIDDEN");

        MvcResult approved = send(post("/sales/orders/" + id + "/discount-approval"), fx.manager, approval);
        assertThat(approved.getResponse().getStatus()).as(approved.getResponse().getContentAsString()).isEqualTo(200);
        JsonNode order = body(approved);
        assertThat(amount(order.get("total"))).isEqualTo("170.00");
        assertThat(order.get("manualDiscount").get("approvedBy").get("id").asLong()).isEqualTo(fx.manager);
        assertThat(order.get("manualDiscount").get("reason").asText()).isEqualTo("Aprovado pela gerencia");
        assertThat(paymentAmount(id)).isEqualByComparingTo("170.00");
        assertThat(fx.count("SELECT count(*) FROM audit_event WHERE action = 'DISCOUNT_APPROVED' AND entity_id = ?", id)).isEqualTo(1);
        assertError(send(post("/sales/orders/" + id + "/discount-approval"), fx.manager,
                "{\"percent\":25,\"reason\":\"Acima da gerencia\"}"), 403, "DISCOUNT_ABOVE_LIMIT");

        assertThat(call(post("/sales/orders/" + id + "/payment"), fx.allan).getResponse().getStatus()).isEqualTo(200);
        assertError(send(post("/sales/orders/" + id + "/discount-approval"), fx.manager, approval), 409, "STATE_CONFLICT");
        assertError(send(post("/sales/orders/999999/discount-approval"), fx.manager, approval), 404, "NOT_FOUND");

        String month = LocalDate.now(java.time.ZoneId.of("America/Fortaleza")).toString().substring(0, 7);
        JsonNode analytics = body(call(get("/analytics/sales").param("month", month), fx.manager));
        JsonNode manual = analytics.get("manualDiscounts").get(0);
        assertThat(manual.get("seller").get("id").asLong()).isEqualTo(fx.allan);
        assertThat(manual.get("orders").asLong()).isEqualTo(1);
        assertThat(amount(manual.get("amount"))).isEqualTo("30.00");
        assertThat(manual.get("approvedByManager").asLong()).isEqualTo(1);
    }

    @Test
    @DisplayName("[CT-DISC-105] desconto manual depois da promocao, em centavos; uso da promocao nao inclui o manual")
    void manualDiscountAfterPromotion() throws Exception {
        long promo = jdbc.sql("""
                INSERT INTO promotion(name, kind, value, product_id, starts_at, ends_at, created_by)
                VALUES ('Fone 10%', 'PERCENT', 10, :product, now() - interval '1 day', now() + interval '1 day', :manager)
                RETURNING id""").param("product", headset).param("manager", fx.manager).query(Long.class).single();
        String items = "[{\"productId\":%d,\"quantity\":2,\"promotionId\":%d},{\"productId\":%d,\"quantity\":1}]".formatted(headset, promo, cheap);
        fx.set("UPDATE discount_policy SET allow_below_cost = true WHERE role = 'VENDEDOR'");
        JsonNode sale = body(order(fx.allan, items, "{\"percent\":3.33,\"reason\":\"Cliente fiel da loja\"}"));
        // bruto 210,00; promocao 20,00 -> 190,00; manual 3,33% de 190,00 = 6,327 -> 6,33; total 183,67
        assertThat(amount(sale.get("subtotal"))).isEqualTo("210.00");
        assertThat(amount(sale.get("manualDiscount").get("amount"))).isEqualTo("6.33");
        assertThat(amount(sale.get("discountTotal"))).isEqualTo("26.33");
        assertThat(amount(sale.get("total"))).isEqualTo("183.67");
        long id = sale.get("id").asLong();
        assertThat(paymentAmount(id)).isEqualByComparingTo("183.67");
        assertThat(call(post("/sales/orders/" + id + "/payment"), fx.allan).getResponse().getStatus()).isEqualTo(200);

        String month = LocalDate.now(java.time.ZoneId.of("America/Fortaleza")).toString().substring(0, 7);
        JsonNode usage = body(call(get("/analytics/sales").param("month", month), fx.manager)).get("promotionUsage").get(0);
        assertThat(amount(usage.get("discountTotal"))).isEqualTo("20.00");
    }

    @Test
    @DisplayName("[CT-DISC-106] total, desconto em valor ou preco enviados pelo cliente sao rejeitados (400)")
    void clientValuesAreRejected() throws Exception {
        String items = "[{\"productId\":%d,\"quantity\":1}]".formatted(headset);
        assertError(send(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString()), fx.allan,
                sale(fx.customer, items, null).replace("\"fulfillment\"", "\"total\":{\"amount\":1,\"currency\":\"BRL\"},\"fulfillment\"")), 400, "MALFORMED_REQUEST");
        assertError(order(fx.allan, items, "{\"percent\":5,\"amount\":50,\"reason\":\"Cliente fiel da loja\"}"), 400, "MALFORMED_REQUEST");
        assertError(order(fx.allan, items, "{\"percent\":0,\"reason\":\"Cliente fiel da loja\"}"), 400, "VALIDATION_FAILED");
        assertError(order(fx.allan, items, "{\"percent\":5.555,\"reason\":\"Cliente fiel da loja\"}"), 400, "VALIDATION_FAILED");
        assertError(order(fx.allan, items, "{\"percent\":5,\"reason\":\"oi\"}"), 400, "VALIDATION_FAILED");
        assertThat(fx.count("SELECT count(*) FROM orders")).isZero();
    }

    @Test
    @DisplayName("[CT-DISC-107] alcadas: GERENTE consulta e altera com motivo e auditoria; VENDEDOR 403")
    void policiesAreManagerOnly() throws Exception {
        JsonNode list = body(call(get("/discount-policy"), fx.manager));
        assertThat(list.findValuesAsText("role")).containsExactly("GERENTE", "VENDEDOR");
        assertError(call(get("/discount-policy"), fx.allan), 403, "FORBIDDEN");
        assertError(send(put("/discount-policy/VENDEDOR"), fx.allan, "{\"maxPercent\":50,\"allowBelowCost\":true,\"reason\":\"Tentativa\"}"), 403, "FORBIDDEN");

        MvcResult updated = send(put("/discount-policy/VENDEDOR"), fx.manager, "{\"maxPercent\":7.5,\"allowBelowCost\":false,\"reason\":\"Campanha de outubro\"}");
        assertThat(updated.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(updated).get("maxPercent").decimalValue()).isEqualByComparingTo("7.5");
        assertThat(body(updated).get("updatedBy").get("id").asLong()).isEqualTo(fx.manager);
        String detail = jdbc.sql("SELECT detail::text FROM audit_event WHERE action = 'DISCOUNT_POLICY_CHANGED'").query(String.class).single();
        assertThat(detail).contains("Campanha de outubro").contains("VENDEDOR");
        assertError(send(put("/discount-policy/VENDEDOR"), fx.manager, "{\"maxPercent\":101,\"allowBelowCost\":false,\"reason\":\"Invalido\"}"), 400, "VALIDATION_FAILED");
        assertError(send(put("/discount-policy/ADMIN"), fx.manager, "{\"maxPercent\":5,\"allowBelowCost\":false,\"reason\":\"Invalido\"}"), 400, "MALFORMED_REQUEST");
        assertThat(order(fx.allan, "[{\"productId\":%d,\"quantity\":1}]".formatted(headset), "{\"percent\":7.5,\"reason\":\"Campanha de outubro\"}")
                .getResponse().getStatus()).isEqualTo(201);
    }
}
