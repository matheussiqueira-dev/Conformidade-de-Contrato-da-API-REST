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
        MvcResult result = send(post("/customers"), fx.allan, INDIVIDUAL);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode created = body(result);
        long id = created.get("id").asLong();
        assertThat(created.get("document").asText()).isEqualTo("***.456.789-**");
        assertThat(created.get("type").asText()).isEqualTo("INDIVIDUAL");
        assertThat(created.get("createdBy").get("name").asText()).isEqualTo("Allan Barros");
        assertThat(created.get("version").asInt()).isZero();
        assertThat(created.get("address").isNull()).isTrue();
        assertThat(created.get("companyName").isNull()).isTrue();
        assertThat(created.get("birthDate").asText()).isEqualTo("1990-05-10");

        assertThat(body(call(get("/customers/" + id), fx.manager)).get("document").asText()).isEqualTo("123.456.789-09");
        assertThat(body(call(get("/customers/" + id), fx.francisco)).get("document").asText()).isEqualTo("***.456.789-**");
        assertThat(jdbc.sql("SELECT cpf FROM customer WHERE id = :id").param("id", id).query(String.class).single())
                .isEqualTo("12345678909");
        assertThat(fx.count("SELECT count(*) FROM audit_event WHERE action = 'CUSTOMER_CREATED' AND entity_id = ?", id)).isEqualTo(1);

        long company = create(CORPORATE, fx.manager);
        JsonNode corporate = body(call(get("/customers/" + company), fx.allan));
        assertThat(corporate.get("document").asText()).isEqualTo("**.222.333/0001-**");
        assertThat(corporate.get("address").get("city").asText()).isEqualTo("Fortaleza");
        assertThat(corporate.get("companyName").asText()).isEqualTo("Azul Comercio LTDA");
    }

    @ParameterizedTest(name = "[CT-CRM-102] documento invalido responde 400: {0}")
    @ValueSource(strings = {"123.456.789-00", "111.111.111-11", "1234567890", "abc.def.ghi-jk"})
    void invalidCpfIsRejected(String cpf) throws Exception {
        assertError(send(post("/customers"), fx.allan, INDIVIDUAL.replace("123.456.789-09", cpf)), 400, "VALIDATION_FAILED");
        assertThat(fx.count("SELECT count(*) FROM customer")).isEqualTo(1);
    }

    @Test
    @DisplayName("[CT-CRM-102] CNPJ invalido 400; documento duplicado (com ou sem mascara) 409 DUPLICATE_DOCUMENT")
    void duplicateDocumentIsRejected() throws Exception {
        assertError(send(post("/customers"), fx.allan, CORPORATE.replace("11.222.333/0001-81", "11.222.333/0001-82")),
                400, "VALIDATION_FAILED");
        create(INDIVIDUAL, fx.allan);
        assertError(send(post("/customers"), fx.allan, INDIVIDUAL.replace("123.456.789-09", "12345678909")
                .replace("joana@", "outra@")), 409, "DUPLICATE_DOCUMENT");
        assertError(send(post("/customers"), fx.allan, INDIVIDUAL.replace("\"INDIVIDUAL\"", "\"CORPORATE\"")), 400, "MALFORMED_REQUEST");
        assertError(send(post("/customers"), fx.allan, INDIVIDUAL.replace("\"phone\":\"(85) 99999-0000\",", "")), 400, "VALIDATION_FAILED");
        assertError(send(post("/customers"), fx.allan, INDIVIDUAL.replace("\"type\":\"INDIVIDUAL\",", "")), 400, "MALFORMED_REQUEST");
    }

    @Test
    @DisplayName("[C-11] busca: so digitos = documento exato; senao prefixo do nome; ordem por nome")
    void searchByDocumentOrNamePrefix() throws Exception {
        long joana = create(INDIVIDUAL, fx.allan);
        create(CORPORATE, fx.allan);

        JsonNode byCpf = body(call(get("/customers").param("q", "123.456.789-09"), fx.allan));
        assertThat(byCpf.get("totalElements").asLong()).isEqualTo(1);
        assertThat(byCpf.get("content").get(0).get("id").asLong()).isEqualTo(joana);
        assertThat(byCpf.get("content").get(0).get("document").asText()).isEqualTo("***.456.789-**");
        assertThat(body(call(get("/customers").param("q", "123456"), fx.allan)).get("totalElements").asLong()).isZero();
        assertThat(body(call(get("/customers").param("q", "11222333000181"), fx.allan)).get("totalElements").asLong()).isEqualTo(1);

        JsonNode byName = body(call(get("/customers").param("q", "jo"), fx.allan));
        assertThat(byName.get("totalElements").asLong()).isEqualTo(1);
        assertThat(body(call(get("/customers").param("q", "%"), fx.allan)).get("totalElements").asLong()).isZero();

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
        long id = create(INDIVIDUAL, fx.allan);
        String update = """
                {"name":"Joana P. Lima","email":"joana.lima@cliente.test","phone":"85988887777","version":%d,
                 "address":{"street":"Rua C","number":"5","neighborhood":"Centro","city":"Sobral","state":"CE","zipCode":"62010000"}}""";

        MvcResult first = send(put("/customers/" + id), fx.allan, update.formatted(0));
        assertThat(first.getResponse().getStatus()).as(first.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(body(first).get("version").asInt()).isEqualTo(1);
        assertThat(body(first).get("name").asText()).isEqualTo("Joana P. Lima");
        assertThat(body(first).get("address").get("city").asText()).isEqualTo("Sobral");
        assertThat(body(first).get("birthDate").asText()).isEqualTo("1990-05-10");

        assertError(send(put("/customers/" + id), fx.francisco, update.formatted(0)), 409, "CONCURRENT_UPDATE");
        assertError(send(put("/customers/" + id), fx.allan, update.formatted(1).replace("\"version\"", "\"cpf\":\"52998224725\",\"version\"")),
                400, "MALFORMED_REQUEST");
        assertError(send(put("/customers/" + id), fx.allan, update.formatted(1).replace("\"version\"", "\"companyName\":\"Empresa\",\"version\"")),
                400, "VALIDATION_FAILED");
        assertError(send(put("/customers/999999"), fx.allan, update.formatted(0)), 404, "NOT_FOUND");
        assertThat(jdbc.sql("SELECT cpf FROM customer WHERE id = :id").param("id", id).query(String.class).single())
                .isEqualTo("12345678909");

        MvcResult second = send(put("/customers/" + id), fx.manager, update.formatted(1).replace("Sobral", "Crato"));
        assertThat(body(second).get("version").asInt()).isEqualTo(2);
        assertThat(fx.count("SELECT count(*) FROM address WHERE city IN ('Sobral', 'Crato')")).isEqualTo(1);
    }

    @Test
    @DisplayName("[CT-CRM-105] VENDEDOR nao exclui; cliente com pedido 409 RESOURCE_IN_USE; sem pedido 204")
    void deleteRules() throws Exception {
        long id = create(CORPORATE, fx.allan);
        assertError(call(delete("/customers/" + id), fx.allan), 403, "FORBIDDEN");

        MvcResult deleted = call(delete("/customers/" + id), fx.manager);
        assertThat(deleted.getResponse().getStatus()).isEqualTo(204);
        assertThat(fx.count("SELECT count(*) FROM address")).isZero();
        assertError(call(delete("/customers/" + id), fx.manager), 404, "NOT_FOUND");

        long product = fx.product("CAB-001", "Cabo", "10.00", 5, "4.00");
        sale(fx.allan, product);
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
        sale(fx.allan, product);
        sale(fx.francisco, product);
        sale(fx.allan, product);

        JsonNode page = body(call(get("/customers/" + fx.customer + "/orders").param("size", "2"), fx.allan));
        assertThat(page.get("totalElements").asLong()).isEqualTo(3);
        assertThat(page.get("content")).hasSize(2);
        JsonNode latest = page.get("content").get(0);
        JsonNode franciscos = page.get("content").get(1);
        assertThat(latest.get("id").asLong()).isGreaterThan(franciscos.get("id").asLong());
        assertThat(latest.get("own").asBoolean()).isTrue();
        assertThat(latest.get("total").get("amount").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(latest.get("sellerFirstName").asText()).isEqualTo("Allan");
        assertThat(latest.get("items").get(0).get("name").asText()).isEqualTo("Cabo USB");
        assertThat(franciscos.get("own").asBoolean()).isFalse();
        assertThat(franciscos.has("total")).isFalse();
        assertThat(franciscos.has("sellerFirstName")).isFalse();
        assertThat(page.toString()).doesNotContain("cost", "Cost");

        JsonNode manager = body(call(get("/customers/" + fx.customer + "/orders"), fx.manager));
        assertThat(manager.get("content").findValuesAsText("sellerFirstName")).containsExactly("Allan", "Francisco", "Allan");

        JsonNode summary = body(call(get("/customers").param("q", "Marina"), fx.allan)).get("content").get(0);
        assertThat(summary.get("ordersCount").asLong()).isEqualTo(3);
        assertThat(summary.get("lastOrderAt").isNull()).isFalse();
        assertError(call(get("/customers/999999/orders"), fx.allan), 404, "NOT_FOUND");
    }
}
