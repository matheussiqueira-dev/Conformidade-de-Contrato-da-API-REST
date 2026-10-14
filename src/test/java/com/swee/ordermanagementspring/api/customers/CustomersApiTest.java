package com.swee.ordermanagementspring.api.customers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swee.ordermanagementspring.support.PdvFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Clientes do contrato v4 (RF-20, RF-21): CT-CRM-102..105, C-11, C-12 e DEC-ERP-02. */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class CustomersApiTest {

    private static final String INDIVIDUAL = """
            {"type":"INDIVIDUAL","name":"Joana Prado","cpf":"123.456.789-09","email":"joana@cliente.test",
             "phone":"(85) 99999-0000","birthDate":"1990-05-10"}""";
    private static final String CORPORATE = """
            {"type":"CORPORATE","name":"Loja Azul","companyName":"Azul Comercio LTDA","cnpj":"11.222.333/0001-81",
             "email":"compras@azul.test","phone":"8533334444",
             "address":{"street":"Av. B","number":"200","neighborhood":"Aldeota","city":"Fortaleza","state":"CE","zipCode":"60150000"}}""";

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate template;
    @Autowired JdbcClient jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private PdvFixtures fx;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fx = new PdvFixtures(template, jdbc);
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

    private long create(String body, long actor) throws Exception {
        MvcResult result = send(post("/customers"), actor, body);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        long id = body(result).get("id").asLong();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/customers/" + id);
        return id;
    }

    @Test
    @DisplayName("[C-12] VENDEDOR cadastra e recebe documento mascarado; GERENTE ve completo; banco guarda digitos")
    void createAndMaskDocument() throws Exception {
        MvcResult result = send(post("/customers"), fx.ana, INDIVIDUAL);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode created = body(result);
        long id = created.get("id").asLong();
        assertThat(created.get("document").asText()).isEqualTo("***.456.789-**");
        assertThat(created.get("type").asText()).isEqualTo("INDIVIDUAL");
        assertThat(created.get("createdBy").get("name").asText()).isEqualTo("Ana Souza");
        assertThat(created.get("version").asInt()).isZero();
        assertThat(created.get("address").isNull()).isTrue();
        assertThat(created.get("companyName").isNull()).isTrue();
        assertThat(created.get("birthDate").asText()).isEqualTo("1990-05-10");

        assertThat(body(call(get("/customers/" + id), fx.manager)).get("document").asText()).isEqualTo("123.456.789-09");
        assertThat(body(call(get("/customers/" + id), fx.bruno)).get("document").asText()).isEqualTo("***.456.789-**");
        assertThat(jdbc.sql("SELECT cpf FROM customer WHERE id = :id").param("id", id).query(String.class).single())
                .isEqualTo("12345678909");
        assertThat(fx.count("SELECT count(*) FROM audit_event WHERE action = 'CUSTOMER_CREATED' AND entity_id = ?", id)).isEqualTo(1);

        long company = create(CORPORATE, fx.manager);
        JsonNode corporate = body(call(get("/customers/" + company), fx.ana));
        assertThat(corporate.get("document").asText()).isEqualTo("**.222.333/0001-**");
        assertThat(corporate.get("address").get("city").asText()).isEqualTo("Fortaleza");
        assertThat(corporate.get("companyName").asText()).isEqualTo("Azul Comercio LTDA");
    }

    @ParameterizedTest(name = "[CT-CRM-102] documento invalido responde 400: {0}")
    @ValueSource(strings = {"123.456.789-00", "111.111.111-11", "1234567890", "abc.def.ghi-jk"})
    void invalidCpfIsRejected(String cpf) throws Exception {
        assertError(send(post("/customers"), fx.ana, INDIVIDUAL.replace("123.456.789-09", cpf)), 400, "VALIDATION_FAILED");
        assertThat(fx.count("SELECT count(*) FROM customer")).isEqualTo(1);
    }

    @Test
    @DisplayName("[CT-CRM-102] CNPJ invalido 400; documento duplicado (com ou sem mascara) 409 DUPLICATE_DOCUMENT")
    void duplicateDocumentIsRejected() throws Exception {
        assertError(send(post("/customers"), fx.ana, CORPORATE.replace("11.222.333/0001-81", "11.222.333/0001-82")),
                400, "VALIDATION_FAILED");
        create(INDIVIDUAL, fx.ana);
        assertError(send(post("/customers"), fx.ana, INDIVIDUAL.replace("123.456.789-09", "12345678909")
                .replace("joana@", "outra@")), 409, "DUPLICATE_DOCUMENT");
        assertError(send(post("/customers"), fx.ana, INDIVIDUAL.replace("\"INDIVIDUAL\"", "\"CORPORATE\"")), 400, "MALFORMED_REQUEST");
        assertError(send(post("/customers"), fx.ana, INDIVIDUAL.replace("\"phone\":\"(85) 99999-0000\",", "")), 400, "VALIDATION_FAILED");
        assertError(send(post("/customers"), fx.ana, INDIVIDUAL.replace("\"type\":\"INDIVIDUAL\",", "")), 400, "MALFORMED_REQUEST");
    }

    @Test
    @DisplayName("[C-11] busca: so digitos = documento exato; senao prefixo do nome; ordem por nome")
    void searchByDocumentOrNamePrefix() throws Exception {
        long joana = create(INDIVIDUAL, fx.ana);
        create(CORPORATE, fx.ana);

        JsonNode byCpf = body(call(get("/customers").param("q", "123.456.789-09"), fx.ana));
        assertThat(byCpf.get("totalElements").asLong()).isEqualTo(1);
        assertThat(byCpf.get("content").get(0).get("id").asLong()).isEqualTo(joana);
        assertThat(byCpf.get("content").get(0).get("document").asText()).isEqualTo("***.456.789-**");
        assertThat(body(call(get("/customers").param("q", "123456"), fx.ana)).get("totalElements").asLong()).isZero();
        assertThat(body(call(get("/customers").param("q", "11222333000181"), fx.ana)).get("totalElements").asLong()).isEqualTo(1);

        JsonNode byName = body(call(get("/customers").param("q", "jo"), fx.ana));
        assertThat(byName.get("totalElements").asLong()).isEqualTo(1);
        assertThat(body(call(get("/customers").param("q", "%"), fx.ana)).get("totalElements").asLong()).isZero();

        JsonNode all = body(call(get("/customers").param("size", "2"), fx.manager));
        assertThat(all.get("totalElements").asLong()).isEqualTo(3);
        assertThat(all.get("content").findValuesAsText("name")).containsExactly("Joana Prado", "Loja Azul");
        assertThat(all.get("content").get(0).get("document").asText()).isEqualTo("123.456.789-09");
        assertThat(all.get("content").get(0).get("ordersCount").asLong()).isZero();
        assertThat(all.get("content").get(0).get("lastOrderAt").isNull()).isTrue();
    }

    @Test
    @DisplayName("[CT-CRM-104] edicao com versao: concorrente 409; documento nao muda pelo PUT")
    void updateUsesVersionAndKeepsDocument() throws Exception {
        long id = create(INDIVIDUAL, fx.ana);
        String update = """
                {"name":"Joana P. Lima","email":"joana.lima@cliente.test","phone":"85988887777","version":%d,
                 "address":{"street":"Rua C","number":"5","neighborhood":"Centro","city":"Sobral","state":"CE","zipCode":"62010000"}}""";

        MvcResult first = send(put("/customers/" + id), fx.ana, update.formatted(0));
        assertThat(first.getResponse().getStatus()).as(first.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(body(first).get("version").asInt()).isEqualTo(1);
        assertThat(body(first).get("name").asText()).isEqualTo("Joana P. Lima");
        assertThat(body(first).get("address").get("city").asText()).isEqualTo("Sobral");
        assertThat(body(first).get("birthDate").asText()).isEqualTo("1990-05-10");

        assertError(send(put("/customers/" + id), fx.bruno, update.formatted(0)), 409, "CONCURRENT_UPDATE");
        assertError(send(put("/customers/" + id), fx.ana, update.formatted(1).replace("\"version\"", "\"cpf\":\"52998224725\",\"version\"")),
                400, "MALFORMED_REQUEST");
        assertError(send(put("/customers/" + id), fx.ana, update.formatted(1).replace("\"version\"", "\"companyName\":\"Empresa\",\"version\"")),
                400, "VALIDATION_FAILED");
        assertError(send(put("/customers/999999"), fx.ana, update.formatted(0)), 404, "NOT_FOUND");
        assertThat(jdbc.sql("SELECT cpf FROM customer WHERE id = :id").param("id", id).query(String.class).single())
                .isEqualTo("12345678909");

        MvcResult second = send(put("/customers/" + id), fx.manager, update.formatted(1).replace("Sobral", "Crato"));
        assertThat(body(second).get("version").asInt()).isEqualTo(2);
        assertThat(fx.count("SELECT count(*) FROM address WHERE city IN ('Sobral', 'Crato')")).isEqualTo(1);
    }

    @Test
    @DisplayName("[CT-CRM-105] VENDEDOR nao exclui; cliente com pedido 409 RESOURCE_IN_USE; sem pedido 204")
    void deleteRules() throws Exception {
        long id = create(CORPORATE, fx.ana);
        assertError(call(delete("/customers/" + id), fx.ana), 403, "FORBIDDEN");

        MvcResult deleted = call(delete("/customers/" + id), fx.manager);
        assertThat(deleted.getResponse().getStatus()).isEqualTo(204);
        assertThat(fx.count("SELECT count(*) FROM address")).isZero();
        assertError(call(delete("/customers/" + id), fx.manager), 404, "NOT_FOUND");

        long product = fx.product("CAB-001", "Cabo", "10.00", 5, "4.00");
        sale(fx.ana, product);
        assertError(call(delete("/customers/" + fx.customer), fx.manager), 409, "RESOURCE_IN_USE");
    }

    private void sale(long seller, long product) throws Exception {
        MvcResult result = call(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":%d,"items":[{"productId":%d,"quantity":1}],"payment":{"type":"PIX"},"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, product)), seller);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("[CT-CRM-103] historico: mais recente primeiro, paginado, sem custo; total e vendedor so nos proprios")
    void historyHidesOtherSellersTotals() throws Exception {
        long product = fx.product("CAB-001", "Cabo USB", "10.00", 10, "4.00");
        sale(fx.ana, product);
        sale(fx.bruno, product);
        sale(fx.ana, product);

        JsonNode page = body(call(get("/customers/" + fx.customer + "/orders").param("size", "2"), fx.ana));
        assertThat(page.get("totalElements").asLong()).isEqualTo(3);
        assertThat(page.get("content")).hasSize(2);
        JsonNode latest = page.get("content").get(0);
        JsonNode brunos = page.get("content").get(1);
        assertThat(latest.get("id").asLong()).isGreaterThan(brunos.get("id").asLong());
        assertThat(latest.get("own").asBoolean()).isTrue();
        assertThat(latest.get("total").get("amount").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(latest.get("sellerFirstName").asText()).isEqualTo("Ana");
        assertThat(latest.get("items").get(0).get("name").asText()).isEqualTo("Cabo USB");
        assertThat(brunos.get("own").asBoolean()).isFalse();
        assertThat(brunos.has("total")).isFalse();
        assertThat(brunos.has("sellerFirstName")).isFalse();
        assertThat(page.toString()).doesNotContain("cost", "Cost");

        JsonNode manager = body(call(get("/customers/" + fx.customer + "/orders"), fx.manager));
        assertThat(manager.get("content").findValuesAsText("sellerFirstName")).containsExactly("Ana", "Bruno", "Ana");

        JsonNode summary = body(call(get("/customers").param("q", "Marina"), fx.ana)).get("content").get(0);
        assertThat(summary.get("ordersCount").asLong()).isEqualTo(3);
        assertThat(summary.get("lastOrderAt").isNull()).isFalse();
        assertError(call(get("/customers/999999/orders"), fx.ana), 404, "NOT_FOUND");
    }

    private JsonNode order(long seller, long product, String payment) throws Exception {
        MvcResult result = call(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":%d,"items":[{"productId":%d,"quantity":1}],"payment":%s,"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, product, payment)), seller);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return body(result);
    }

    private JsonNode paid(long seller, long product, String payment) throws Exception {
        JsonNode created = order(seller, product, payment);
        MvcResult paidResult = call(post("/sales/orders/" + created.get("id").asLong() + "/payment"), seller);
        assertThat(paidResult.getResponse().getStatus()).isEqualTo(200);
        return body(paidResult);
    }

    /** Ana: duas vendas pagas (Pix), uma com estorno de 5; Bruno: uma paga (cartao), uma pendente e uma cancelada. */
    private void purchases() throws Exception {
        long product = fx.product("CAB-001", "Cabo USB", "10.00", 20, "4.00");
        long charger = fx.product("CAR-001", "Carregador", "30.00", 20, "12.00");
        JsonNode first = paid(fx.ana, product, "{\"type\":\"PIX\"}");
        paid(fx.ana, product, "{\"type\":\"PIX\"}");
        paid(fx.bruno, charger, "{\"type\":\"CARD\",\"cardLast4\":\"4242\",\"installments\":1}");
        order(fx.bruno, product, "{\"type\":\"PIX\"}");
        JsonNode canceled = order(fx.bruno, charger, "{\"type\":\"PIX\"}");
        assertThat(call(post("/orders/" + canceled.get("id").asLong() + "/cancel"), fx.bruno).getResponse().getStatus()).isEqualTo(200);
        MvcResult refund = call(post("/payment/" + first.get("payment").get("id").asLong() + "/refund")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":{\"amount\":5,\"currency\":\"BRL\"},\"reason\":\"Troca parcial\"}"),
                fx.manager);
        assertThat(refund.getResponse().getStatus()).isEqualTo(201);
    }

    private static String amount(JsonNode money) {
        return money.isNull() ? null : money.get("amount").decimalValue().setScale(2).toPlainString();
    }

    @Test
    @DisplayName("[CT-CUS-121] resumo do GERENTE: loja toda, liquido de estornos, pendentes e cancelados fora dos totais")
    void managerSummaryCoversTheWholeStore() throws Exception {
        purchases();
        JsonNode summary = body(call(get("/customers/" + fx.customer + "/summary"), fx.manager));
        assertThat(summary.get("scope").asText()).isEqualTo("STORE");
        assertThat(summary.get("ordersPaid").asLong()).isEqualTo(3);
        assertThat(summary.get("pendingOrders").asLong()).isEqualTo(1);
        assertThat(summary.get("canceledOrders").asLong()).isEqualTo(1);
        assertThat(amount(summary.get("totalSpent"))).isEqualTo("45.00");
        assertThat(amount(summary.get("averageTicket"))).isEqualTo("15.00");
        assertThat(amount(summary.get("refundsTotal"))).isEqualTo("5.00");
        assertThat(summary.get("firstPurchaseAt").isNull()).isFalse();
        assertThat(summary.get("lastPurchaseAt").asText()).matches("\\d{4}-\\d{2}-\\d{2}T.*-03:00");
        assertThat(summary.get("topProducts").get(0).get("name").asText()).isEqualTo("Cabo USB");
        assertThat(summary.get("topProducts").get(0).get("quantity").asLong()).isEqualTo(2);
        assertThat(summary.get("topProducts").get(1).get("quantity").asLong()).isEqualTo(1);
        assertThat(summary.get("paymentMix").findValuesAsText("method")).containsExactly("CARD", "PIX");
        assertThat(amount(summary.get("paymentMix").get(1).get("amount"))).isEqualTo("20.00");
        assertThat(summary.toString()).doesNotContain("cost", "Cost", "margin");
    }

    @Test
    @DisplayName("[CT-CUS-122] resumo do VENDEDOR: valores so dos pedidos dele, sem estornos da loja")
    void sellerSummaryIsScopedToOwnOrders() throws Exception {
        purchases();
        JsonNode ana = body(call(get("/customers/" + fx.customer + "/summary"), fx.ana));
        assertThat(ana.get("scope").asText()).isEqualTo("OWN");
        assertThat(ana.get("ordersPaid").asLong()).isEqualTo(3);
        assertThat(amount(ana.get("totalSpent"))).isEqualTo("15.00");
        assertThat(amount(ana.get("averageTicket"))).isEqualTo("7.50");
        assertThat(ana.get("refundsTotal").isNull()).isTrue();
        assertThat(ana.get("paymentMix").findValuesAsText("method")).containsExactly("PIX");
        assertThat(ana.get("topProducts")).hasSize(2);

        JsonNode bruno = body(call(get("/customers/" + fx.customer + "/summary"), fx.bruno));
        assertThat(amount(bruno.get("totalSpent"))).isEqualTo("30.00");
        assertThat(bruno.get("paymentMix").findValuesAsText("method")).containsExactly("CARD");
    }

    @Test
    @DisplayName("[CT-CUS-123] cliente sem compras: zeros e datas nulas, sem numero ficticio; inexistente 404")
    void customerWithoutPurchasesHasZeros() throws Exception {
        JsonNode summary = body(call(get("/customers/" + fx.customer + "/summary"), fx.manager));
        assertThat(summary.get("ordersPaid").asLong()).isZero();
        assertThat(summary.get("pendingOrders").asLong()).isZero();
        assertThat(amount(summary.get("totalSpent"))).isEqualTo("0.00");
        assertThat(summary.get("averageTicket").isNull()).isTrue();
        assertThat(amount(summary.get("refundsTotal"))).isEqualTo("0.00");
        assertThat(summary.get("firstPurchaseAt").isNull()).isTrue();
        assertThat(summary.get("lastPurchaseAt").isNull()).isTrue();
        assertThat(summary.get("topProducts")).isEmpty();
        assertThat(summary.get("paymentMix")).isEmpty();
        assertError(call(get("/customers/999999/summary"), fx.ana), 404, "NOT_FOUND");
    }

    @Test
    @DisplayName("[CT-CRM-106] CNPJ alfanumerico: cadastro, duplicidade, mascara do VENDEDOR e busca exata")
    void alphanumericCnpjIsSupported() throws Exception {
        String company = CORPORATE.replace("11.222.333/0001-81", "12.ABC.345/01DE-35");
        long id = create(company, fx.ana);
        assertThat(fx.count("SELECT count(*) FROM customer WHERE cnpj = '12ABC34501DE35'")).isEqualTo(1);
        assertThat(body(call(get("/customers/" + id), fx.manager)).get("document").asText()).isEqualTo("12.ABC.345/01DE-35");
        assertThat(body(call(get("/customers/" + id), fx.ana)).get("document").asText()).isEqualTo("**.ABC.345/01DE-**");
        assertError(send(post("/customers"), fx.bruno, CORPORATE.replace("11.222.333/0001-81", "12ABC34501DE35")), 409, "DUPLICATE_DOCUMENT");
        assertError(send(post("/customers"), fx.bruno, CORPORATE.replace("11.222.333/0001-81", "12.ABC.345/01DE-36")), 400, "VALIDATION_FAILED");
        assertError(send(post("/customers"), fx.bruno, CORPORATE.replace("11.222.333/0001-81", "12.abc.345/01de-35")), 400, "VALIDATION_FAILED");
        JsonNode found = body(call(get("/customers").param("q", "12abc34501de35"), fx.bruno));
        assertThat(found.get("totalElements").asLong()).isEqualTo(1);
        assertThat(found.get("content").get(0).get("document").asText()).isEqualTo("**.ABC.345/01DE-**");
    }
}
