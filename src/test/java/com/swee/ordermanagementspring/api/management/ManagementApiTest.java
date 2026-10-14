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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * [CT-MGMT-001, CT-MGMT-002, C-20, C-21] Faturamento do periodo: realizado = pagamentos confirmados menos estornos,
 * pendente fora do realizado, lucro nulo quando falta custo, periodo vazio sem numero inventado.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class ManagementApiTest {

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

    private JsonNode revenue(String period) throws Exception {
        MvcResult result = call(get("/management/revenue").param("period", period), fx.manager);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        return json.readTree(result.getResponse().getContentAsString());
    }

    private long sale(long seller, long product, int quantity, String payment) throws Exception {
        MvcResult result = call(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":%d,"items":[{"productId":%d,"quantity":%d}],"payment":%s,"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, product, quantity, payment)), seller);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void pay(long order, long seller) throws Exception {
        assertThat(call(post("/sales/orders/" + order + "/payment"), seller).getResponse().getStatus()).isEqualTo(200);
    }

    private static BigDecimal money(JsonNode node) {
        return node.get("amount").decimalValue();
    }

    @Test
    @DisplayName("[CT-MGMT-001] bruto, estornos, liquido, ticket, quebras e pendentes; lucro nulo quando falta custo")
    void revenueWithRefundAndUnknownCost() throws Exception {
        long cable = fx.product("CAB-001", "Cabo", "19.99", 5, null);
        long allans = sale(fx.allan, mouse, 2, "{\"type\":\"PIX\"}");
        pay(allans, fx.allan);
        long franciscos = sale(fx.francisco, cable, 1, "{\"type\":\"CARD\",\"cardLast4\":\"1234\",\"installments\":1}");
        pay(franciscos, fx.francisco);
        sale(fx.allan, mouse, 1, "{\"type\":\"PIX\"}");                       // pendente: fora do realizado
        long canceled = sale(fx.francisco, mouse, 1, "{\"type\":\"PIX\"}");
        call(post("/orders/" + canceled + "/cancel"), fx.francisco);
        jdbc.sql("INSERT INTO refund(payment_id, amount, reason, created_by) SELECT id, 20.00, 'Avaria', :m FROM payment WHERE order_id = :o")
                .param("m", fx.manager).param("o", allans).update();
        fx.set("UPDATE stock_balance SET min_level = 5 WHERE product_id = ?", cable);

        JsonNode report = revenue("D7");
        String today = StoreTime.today().toString();
        assertThat(report.get("period").get("to").asText()).isEqualTo(today);
        assertThat(report.get("period").get("from").asText()).isEqualTo(StoreTime.today().minusDays(6).toString());
        assertThat(money(report.get("grossRevenue"))).isEqualByComparingTo("219.99");
        assertThat(money(report.get("refunds"))).isEqualByComparingTo("20.00");
        assertThat(money(report.get("netRevenue"))).isEqualByComparingTo("199.99");
        assertThat(report.get("ordersPaid").asLong()).isEqualTo(2);
        assertThat(report.get("unitsSold").asLong()).isEqualTo(3);
        assertThat(money(report.get("averageTicket"))).isEqualByComparingTo("110.00");
        assertThat(report.get("grossProfit").isNull()).isTrue();
        assertThat(report.get("grossMarginPercent").isNull()).isTrue();
        assertThat(report.get("costCoveragePercent").decimalValue()).isEqualByComparingTo("90.9");
        assertThat(report.get("daily")).hasSize(7);
        assertThat(money(report.get("daily").get(6).get("netRevenue"))).isEqualByComparingTo("199.99");
        assertThat(report.get("daily").get(6).get("grossProfit").isNull()).isTrue();
        assertThat(money(report.get("daily").get(0).get("netRevenue"))).isEqualByComparingTo("0.00");

        JsonNode methods = report.get("byPaymentMethod");
        assertThat(methods.get(0).get("method").asText()).isEqualTo("PIX");
        assertThat(money(methods.get(0).get("netRevenue"))).isEqualByComparingTo("180.00");
        assertThat(methods.get(1).get("method").asText()).isEqualTo("CARD");
        assertThat(methods.get(1).get("ordersPaid").asLong()).isEqualTo(1);
        JsonNode sellers = report.get("bySeller");
        assertThat(sellers.get(0).get("seller").get("id").asLong()).isEqualTo(fx.allan);
        assertThat(money(sellers.get(0).get("netRevenue"))).isEqualByComparingTo("180.00");
        assertThat(money(sellers.get(1).get("netRevenue"))).isEqualByComparingTo("19.99");
        JsonNode top = report.get("topProducts");
        assertThat(top.get(0).get("sku").asText()).isEqualTo("MOU-001");
        assertThat(money(top.get(0).get("grossProfit"))).isEqualByComparingTo("80.00");
        assertThat(top.get(1).get("grossProfit").isNull()).isTrue();
        assertThat(report.get("lowStockCount").asLong()).isEqualTo(1);
        assertThat(report.get("pendingPaymentCount").asLong()).isEqualTo(1);
        assertThat(money(report.get("pendingPaymentAmount"))).isEqualByComparingTo("100.00");

        // Mesmos numeros da view (D9), que o job bd confere pelo psql.
        BigDecimal viewNet = jdbc.sql("SELECT coalesce(sum(faturamento_liquido), 0) FROM v_faturamento_diario WHERE dia = :d")
                .param("d", StoreTime.today()).query(BigDecimal.class).single();
        assertThat(viewNet).isEqualByComparingTo(money(report.get("netRevenue")));
    }

    @Test
    @DisplayName("[C-20] com custo conhecido: lucro = liquido - custo e margem = lucro / liquido")
    void profitAndMarginWithKnownCost() throws Exception {
        pay(sale(fx.allan, mouse, 2, "{\"type\":\"PIX\"}"), fx.allan);

        JsonNode report = revenue("TODAY");
        assertThat(money(report.get("grossProfit"))).isEqualByComparingTo("80.00");
        assertThat(report.get("grossMarginPercent").decimalValue()).isEqualByComparingTo("40.0");
        assertThat(report.get("costCoveragePercent").decimalValue()).isEqualByComparingTo("100.0");
        assertThat(report.get("daily")).hasSize(1);
        assertThat(revenue("D90").get("daily")).hasSize(90);
    }

    @Test
    @DisplayName("[CT-MGMT-002] periodo sem venda: zeros reais, sem ticket nem margem inventados")
    void emptyPeriodHasNoFictitiousNumbers() throws Exception {
        sale(fx.allan, mouse, 1, "{\"type\":\"PIX\"}");

        JsonNode report = revenue("D30");
        assertThat(money(report.get("netRevenue"))).isEqualByComparingTo("0.00");
        assertThat(report.get("ordersPaid").asLong()).isZero();
        assertThat(report.get("averageTicket").isNull()).isTrue();
        assertThat(report.get("grossMarginPercent").isNull()).isTrue();
        assertThat(report.get("costCoveragePercent").decimalValue()).isEqualByComparingTo("0.0");
        assertThat(report.get("byPaymentMethod")).isEmpty();
        assertThat(report.get("topProducts")).isEmpty();
        assertThat(report.get("pendingPaymentCount").asLong()).isEqualTo(1);
    }

    @Test
    @DisplayName("[SignedMoney] estorno maior que as vendas do periodo deixa liquido e lucro negativos, sem margem")
    void refundAboveSalesGivesNegativeNet() throws Exception {
        long order = sale(fx.allan, mouse, 1, "{\"type\":\"PIX\"}");
        pay(order, fx.allan);
        // A venda foi paga ontem (fora de TODAY); o estorno de hoje entra no periodo de hoje.
        fx.set("UPDATE payment SET payment_date = payment_date - interval '1 day' WHERE order_id = ?", order);
        jdbc.sql("INSERT INTO refund(payment_id, amount, reason, created_by) SELECT id, 50.00, 'Devolucao', :m FROM payment WHERE order_id = :o")
                .param("m", fx.manager).param("o", order).update();

        JsonNode report = revenue("TODAY");
        assertThat(money(report.get("grossRevenue"))).isEqualByComparingTo("0.00");
        assertThat(money(report.get("refunds"))).isEqualByComparingTo("50.00");
        assertThat(money(report.get("netRevenue"))).isEqualByComparingTo("-50.00");
        assertThat(money(report.get("grossProfit"))).isEqualByComparingTo("-50.00");
        assertThat(report.get("grossMarginPercent").isNull()).isTrue();
        assertThat(money(report.get("daily").get(0).get("netRevenue"))).isEqualByComparingTo("-50.00");
        assertThat(money(report.get("byPaymentMethod").get(0).get("netRevenue"))).isEqualByComparingTo("-50.00");
        assertThat(money(report.get("bySeller").get(0).get("netRevenue"))).isEqualByComparingTo("-50.00");
    }

    @Test
    @DisplayName("[C-21] periodo obrigatorio e do dominio; VENDEDOR 403")
    void periodValidationAndAccess() throws Exception {
        assertThat(call(get("/management/revenue"), fx.manager).getResponse().getStatus()).isEqualTo(400);
        assertThat(call(get("/management/revenue").param("period", "D365"), fx.manager).getResponse().getStatus()).isEqualTo(400);
        assertThat(call(get("/management/revenue").param("period", "D7"), fx.allan).getResponse().getStatus()).isEqualTo(403);
    }
}
