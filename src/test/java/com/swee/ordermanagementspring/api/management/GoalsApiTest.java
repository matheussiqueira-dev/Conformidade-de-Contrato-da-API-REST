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

import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Metas, Meu desempenho e resumo de gestao (RF-09 a RF-12): CA-09-01 a CA-12-02. */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class GoalsApiTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate template;
    @Autowired JdbcClient jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private PdvFixtures fx;
    private String month;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fx = new PdvFixtures(template, jdbc);
        month = YearMonth.from(StoreTime.today()).toString();
    }

    private MvcResult call(MockHttpServletRequestBuilder request, long actor) throws Exception {
        return mvc.perform(request.with(user(fx.principal(actor))).with(csrf())).andReturn();
    }

    private JsonNode ok(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        assertThat(json.readTree(result.getResponse().getContentAsString()).get("code").asText()).isEqualTo(code);
    }

    private MvcResult saveGoals(String body) throws Exception {
        return call(put("/goals/monthly").contentType(MediaType.APPLICATION_JSON).content(body), fx.manager);
    }

    private String goals(String store, String ana, String bruno) {
        return """
                {"period":"%s","storeGoal":{"amount":%s,"currency":"BRL"},"sellers":[
                 {"sellerId":%d,"goal":{"amount":%s,"currency":"BRL"}},{"sellerId":%d,"goal":{"amount":%s,"currency":"BRL"}}]}"""
                .formatted(month, store, fx.ana, ana, fx.bruno, bruno);
    }

    private long paidSale(long seller, long product) throws Exception {
        MvcResult result = call(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":%d,"items":[{"productId":%d,"quantity":1}],"payment":{"type":"PIX"},"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, product)), seller);
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        ok(call(post("/sales/orders/" + id + "/payment"), seller));
        return id;
    }

    @Test
    @DisplayName("[CA-09-01, CA-09-02] distribuicao igual a meta da loja e salva; diferente, repetida ou para gerente responde 400")
    void monthlyGoalsDistribution() throws Exception {
        JsonNode saved = ok(saveGoals(goals("160000.00", "80000.00", "80000.00")));
        assertThat(saved.get("storeGoal").get("amount").decimalValue()).isEqualByComparingTo("160000.00");
        assertThat(saved.get("sellers")).hasSize(2);
        assertThat(saved.get("sellers").get(0).get("seller").get("name").asText()).isEqualTo("Ana Souza");

        assertError(saveGoals(goals("160000.00", "80000.00", "70000.00")), 400, "VALIDATION_FAILED");
        assertError(saveGoals(goals("160000.00", "80000.00", "80000.00").replace("\"sellerId\":" + fx.bruno, "\"sellerId\":" + fx.ana)),
                400, "VALIDATION_FAILED");
        assertError(saveGoals(goals("160000.00", "80000.00", "80000.00").replace("\"sellerId\":" + fx.bruno, "\"sellerId\":" + fx.manager)),
                400, "VALIDATION_FAILED");
        assertError(saveGoals(goals("160000.00", "80000.00", "80000.00").replace(month, "2026-13")), 400, "VALIDATION_FAILED");

        JsonNode updated = ok(saveGoals(goals("100000.00", "60000.00", "40000.00")));
        assertThat(updated.get("sellers").get(1).get("goal").get("amount").decimalValue()).isEqualByComparingTo("40000.00");
        assertThat(ok(call(get("/goals/monthly").param("period", month), fx.manager)).get("storeGoal").get("amount").decimalValue())
                .isEqualByComparingTo("100000.00");
        assertError(call(get("/goals/monthly").param("period", "2020-01"), fx.manager), 404, "NOT_FOUND");
        assertError(call(get("/goals/monthly").param("period", month), fx.ana), 403, "FORBIDDEN");
    }

    @Test
    @DisplayName("[CA-10-01, CA-10-03, CA-11-01] desempenho: 68.400 de 80.000 = 85,5%; pendente nao conta; so os dados da sessao")
    void myPerformance() throws Exception {
        ok(saveGoals(goals("160000.00", "80000.00", "80000.00")));
        long big = fx.product("NOT-001", "Notebook", "68400.00", 3, "50000.00");
        paidSale(fx.ana, big);
        call(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerId":%d,"items":[{"productId":%d,"quantity":1}],"payment":{"type":"PIX"},"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, big)), fx.ana);

        JsonNode mine = ok(call(get("/performance/me").param("period", month), fx.ana));
        assertThat(mine.get("seller").get("id").asLong()).isEqualTo(fx.ana);
        assertThat(mine.get("goal").get("amount").decimalValue()).isEqualByComparingTo("80000.00");
        assertThat(mine.get("realized").get("amount").decimalValue()).isEqualByComparingTo("68400.00");
        assertThat(mine.get("remaining").get("amount").decimalValue()).isEqualByComparingTo("11600.00");
        assertThat(mine.get("achievementPercent").decimalValue()).isEqualByComparingTo("85.5");
        assertThat(mine.get("state").asText()).isEqualTo("EM_ANDAMENTO");

        JsonNode brunos = ok(call(get("/performance/me").param("period", month), fx.bruno));
        assertThat(brunos.get("realized").get("amount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(brunos.toString()).doesNotContain("68400");
        assertThat(call(get("/performance/me").param("period", "2026-9"), fx.ana).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("[CA-10-02, CA-11-02] sem meta: estado SEM_META sem percentual; nao ha rota para ver desempenho de outro")
    void noGoalMeansNoPercentage() throws Exception {
        JsonNode mine = ok(call(get("/performance/me").param("period", month), fx.ana));
        assertThat(mine.get("state").asText()).isEqualTo("SEM_META");
        assertThat(mine.get("achievementPercent").isNull()).isTrue();
        assertThat(mine.get("goal").get("amount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(call(get("/performance/me").param("period", month).param("sellerId", Long.toString(fx.bruno)), fx.ana)
                .getResponse().getContentAsString()).contains("\"id\":" + fx.ana);
        assertError(call(get("/management/summary").param("period", month), fx.ana), 403, "FORBIDDEN");
    }

    @Test
    @DisplayName("[CA-12-01, CA-12-02] resumo: total da loja, equipe e metas; mes sem dados responde zeros e SEM_META")
    void managementSummary() throws Exception {
        ok(saveGoals(goals("100.00", "50.00", "50.00")));
        long cable = fx.product("CAB-001", "Cabo", "60.00", 5, "10.00");
        paidSale(fx.ana, cable);
        paidSale(fx.bruno, cable);

        JsonNode summary = ok(call(get("/management/summary").param("period", month), fx.manager));
        assertThat(summary.get("storeGoal").get("amount").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(summary.get("storeRealized").get("amount").decimalValue()).isEqualByComparingTo("120.00");
        assertThat(summary.get("remaining").get("amount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(summary.get("achievementPercent").decimalValue()).isEqualByComparingTo("120.0");
        assertThat(summary.get("state").asText()).isEqualTo("META_ATINGIDA");
        assertThat(summary.get("team")).hasSize(2);
        assertThat(summary.get("team").get(0).get("state").asText()).isEqualTo("META_ATINGIDA");
        assertThat(summary.has("stockAlerts")).isFalse();

        JsonNode empty = ok(call(get("/management/summary").param("period", "2020-01"), fx.manager));
        assertThat(empty.get("storeRealized").get("amount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(empty.get("state").asText()).isEqualTo("SEM_META");
        assertThat(empty.get("achievementPercent").isNull()).isTrue();
        assertThat(empty.get("team")).isEmpty();
    }
}
