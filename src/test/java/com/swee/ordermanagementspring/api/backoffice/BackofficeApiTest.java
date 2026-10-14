package com.swee.ordermanagementspring.api.backoffice;

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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Backoffice do GERENTE (RF-22): produtos, categorias e estoque, inclusive CT-STOCK-108 e C-15/C-16/C-17. */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class BackofficeApiTest {

    private static final String PHYSICAL = """
            {"type":"PHYSICAL","sku":"tec-001","name":"Teclado ABNT2","description":"Teclado com fio","sellable":true,
             "price":{"amount":120.00,"currency":"BRL"},"weightKg":0.8,"minLevel":3}""";
    private static final String DIGITAL = """
            {"type":"DIGITAL","sku":"EBK-001","name":"Guia de montagem","sellable":true,
             "price":{"amount":29.90,"currency":"BRL"},"downloadLink":"https://loja.test/guia.pdf"}""";

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

    private MvcResult send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return call(request.contentType(MediaType.APPLICATION_JSON).content(body), fx.manager);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        assertThat(body(result).get("code").asText()).isEqualTo(code);
    }

    private JsonNode ok(MvcResult result, int status) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        return body(result);
    }

    // ---------------- produtos ----------------

    @Test
    @DisplayName("[C-16] cadastro: SKU em maiusculas, minimo, historico inicial; SKU duplicado 409")
    void createProducts() throws Exception {
        MvcResult result = send(post("/products"), PHYSICAL);
        JsonNode product = ok(result, 201);
        long id = product.get("id").asLong();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/products/" + id);
        assertThat(product.get("sku").asText()).isEqualTo("TEC-001");
        assertThat(product.get("type").asText()).isEqualTo("PHYSICAL");
        assertThat(product.get("weightKg").decimalValue()).isEqualByComparingTo("0.8");
        assertThat(product.get("downloadLink").isNull()).isTrue();
        assertThat(product.get("category").isNull()).isTrue();
        assertThat(product.get("active").asBoolean()).isTrue();
        assertThat(fx.count("SELECT min_level FROM stock_balance WHERE product_id = ?", id)).isEqualTo(3);
        assertThat(fx.count("SELECT count(*) FROM product_price_history WHERE product_id = ? AND old_price IS NULL", id)).isEqualTo(1);

        JsonNode digital = ok(send(post("/products"), DIGITAL.replace("\"sellable\"", "\"categoryId\":%d,\"sellable\"".formatted(fx.category))), 201);
        assertThat(digital.get("weightKg").isNull()).isTrue();
        assertThat(digital.get("category").get("name").asText()).isEqualTo("Acessorios");

        assertError(send(post("/products"), PHYSICAL.replace("tec-001", "TEC-001")), 409, "STATE_CONFLICT");
        assertError(send(post("/products"), DIGITAL.replace("EBK-001", "EBK-002").replace("\"sellable\"", "\"categoryId\":999999,\"sellable\"")),
                404, "NOT_FOUND");
        assertError(send(post("/products"), PHYSICAL.replace("tec-001", "TEC-002").replace("120.00", "0.00")), 400, "VALIDATION_FAILED");
        assertError(send(post("/products"), PHYSICAL.replace("tec-001", "TEC-003").replace("\"sellable\"", "\"active\":false,\"sellable\"")),
                400, "MALFORMED_REQUEST");
        assertError(send(post("/products"), PHYSICAL.replace("tec-001", "tec 001")), 400, "VALIDATION_FAILED");
        assertError(send(post("/products"), DIGITAL.replace("https://loja.test/guia.pdf", "nao e url")), 400, "VALIDATION_FAILED");
        assertError(call(post("/products").contentType(MediaType.APPLICATION_JSON).content(PHYSICAL), fx.allan), 403, "FORBIDDEN");
    }

    @Test
    @DisplayName("[RF-22] lista com filtros; edicao sem preco nem situacao (C-17); tipo errado 400")
    void listAndUpdateProducts() throws Exception {
        long mouse = fx.product("MOU-001", "Mouse", "50.00", 2, "20.00");
        long supply = fx.product("INS-001", "Etiqueta", "0.10", 100, "0.01");
        fx.set("UPDATE product SET sellable = false WHERE id = ?", supply);

        assertThat(ok(call(get("/products"), fx.manager), 200).get("totalElements").asLong()).isEqualTo(2);
        JsonNode supplies = ok(call(get("/products").param("sellable", "false"), fx.manager), 200);
        assertThat(supplies.get("content").findValuesAsText("sku")).containsExactly("INS-001");
        assertThat(ok(call(get("/products").param("q", "mou-001"), fx.manager), 200).get("totalElements").asLong()).isEqualTo(1);

        JsonNode updated = ok(send(put("/products/" + mouse), """
                {"sku":"mou-010","name":"Mouse optico","sellable":true,"weightKg":0.15}"""), 200);
        assertThat(updated.get("sku").asText()).isEqualTo("MOU-010");
        assertThat(updated.get("price").get("amount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(updated.get("category").get("id").asLong()).isEqualTo(fx.category);
        assertError(send(put("/products/" + mouse), """
                {"sku":"MOU-010","name":"Mouse","sellable":true,"price":{"amount":1,"currency":"BRL"}}"""), 400, "MALFORMED_REQUEST");
        assertError(send(put("/products/" + mouse), """
                {"sku":"MOU-010","name":"Mouse","sellable":true,"downloadLink":"https://x.test/a"}"""), 400, "VALIDATION_FAILED");
        assertError(send(put("/products/" + mouse), """
                {"sku":"INS-001","name":"Mouse","sellable":true}"""), 409, "STATE_CONFLICT");
        assertError(send(put("/products/999999"), """
                {"sku":"X","name":"Nada","sellable":true}"""), 404, "NOT_FOUND");
        assertError(call(get("/products/999999"), fx.manager), 404, "NOT_FOUND");
    }

    @Test
    @DisplayName("[RF-22] preco com motivo e autor no historico; inativacao tira do PDV e audita")
    void priceAndStatus() throws Exception {
        long mouse = fx.product("MOU-001", "Mouse", "50.00", 2, "20.00");
        JsonNode changed = ok(send(patch("/products/" + mouse + "/price"), """
                {"newPrice":{"amount":55.50,"currency":"BRL"},"reason":"Reajuste do fornecedor"}"""), 200);
        assertThat(changed.get("price").get("amount").decimalValue()).isEqualByComparingTo("55.50");
        assertError(send(patch("/products/" + mouse + "/price"), """
                {"newPrice":{"amount":55.50,"currency":"BRL"},"reason":"abc"}"""), 400, "VALIDATION_FAILED");
        assertError(send(patch("/products/999999/price"), """
                {"newPrice":{"amount":1.00,"currency":"BRL"},"reason":"Reajuste"}"""), 404, "NOT_FOUND");

        JsonNode history = ok(call(get("/products/" + mouse + "/price-history"), fx.manager), 200);
        JsonNode latest = history.get("content").get(0);
        assertThat(latest.get("oldPrice").get("amount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(latest.get("newPrice").get("amount").decimalValue()).isEqualByComparingTo("55.50");
        assertThat(latest.get("reason").asText()).isEqualTo("Reajuste do fornecedor");
        assertThat(latest.get("changedBy").get("name").asText()).isEqualTo("Matheus Gerente");

        assertThat(ok(send(patch("/products/" + mouse + "/status"), "{\"active\":false}"), 200).get("active").asBoolean()).isFalse();
        assertThat(ok(call(get("/sales/catalog").param("q", "MOU-001"), fx.allan), 200).get("totalElements").asLong()).isZero();
        assertThat(fx.count("SELECT count(*) FROM audit_event WHERE action = 'PRODUCT_STATUS_CHANGED' AND entity_id = ?", mouse)).isEqualTo(1);
        ok(send(patch("/products/" + mouse + "/status"), "{\"active\":false}"), 200);
        assertThat(fx.count("SELECT count(*) FROM audit_event WHERE action = 'PRODUCT_STATUS_CHANGED'")).isEqualTo(1);
        assertError(send(patch("/products/" + mouse + "/status"), "{}"), 400, "VALIDATION_FAILED");
    }

    @Test
    @DisplayName("[RF-22] exclui produto sem historico; com venda ou estoque responde 409 RESOURCE_IN_USE")
    void deleteOnlyWithoutHistory() throws Exception {
        long fresh = ok(send(post("/products"), DIGITAL), 201).get("id").asLong();
        assertThat(call(delete("/products/" + fresh), fx.manager).getResponse().getStatus()).isEqualTo(204);
        assertError(call(delete("/products/" + fresh), fx.manager), 404, "NOT_FOUND");

        long stocked = fx.product("MOU-001", "Mouse", "50.00", 2, "20.00");
        assertError(call(delete("/products/" + stocked), fx.manager), 409, "RESOURCE_IN_USE");
        MvcResult sale = call(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":%d,"items":[{"productId":%d,"quantity":1}],"payment":{"type":"PIX"},"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, stocked)), fx.allan);
        assertThat(sale.getResponse().getStatus()).isEqualTo(201);
        assertError(call(delete("/products/" + stocked), fx.manager), 409, "RESOURCE_IN_USE");
    }

    // ---------------- categorias ----------------

    @Test
    @DisplayName("[C-16] categorias: cria, renomeia, nome duplicado 409, lista por nome")
    void categories() throws Exception {
        MvcResult created = send(post("/categories"), "{\"name\":\"  Perifericos \"}");
        JsonNode category = ok(created, 201);
        assertThat(category.get("name").asText()).isEqualTo("Perifericos");
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/categories/" + category.get("id").asLong());
        assertError(send(post("/categories"), "{\"name\":\"Acessorios\"}"), 409, "STATE_CONFLICT");
        assertError(send(post("/categories"), "{\"name\":\"A\"}"), 400, "VALIDATION_FAILED");

        ok(send(put("/categories/" + fx.category), "{\"name\":\"Cabos\"}"), 200);
        assertError(send(put("/categories/999999"), "{\"name\":\"Nada\"}"), 404, "NOT_FOUND");
        JsonNode all = ok(call(get("/categories"), fx.manager), 200);
        assertThat(all.findValuesAsText("name")).containsExactly("Cabos", "Perifericos");
    }

    // ---------------- estoque ----------------

    @Test
    @DisplayName("[REC-b] posicao: saldo, reservado, disponivel, minimo e custo medio; filtro abaixo do minimo")
    void stockPosition() throws Exception {
        long mouse = fx.product("MOU-001", "Mouse", "50.00", 10, "20.00");
        long cable = fx.product("CAB-001", "Cabo", "10.00", 2, "4.00");
        fx.set("UPDATE stock_balance SET reserved = 3 WHERE product_id = ?", mouse);
        ok(send(patch("/stock/products/" + cable + "/min-level"), "{\"minLevel\":5}"), 200);

        JsonNode page = ok(call(get("/stock/products"), fx.manager), 200);
        JsonNode first = page.get("content").get(0);
        assertThat(first.get("sku").asText()).isEqualTo("CAB-001");
        assertThat(first.get("belowMin").asBoolean()).isTrue();
        JsonNode second = page.get("content").get(1);
        assertThat(second.get("onHand").asInt()).isEqualTo(10);
        assertThat(second.get("reserved").asInt()).isEqualTo(3);
        assertThat(second.get("available").asInt()).isEqualTo(7);
        assertThat(second.get("avgCost").get("amount").decimalValue()).isEqualByComparingTo("20.00");
        JsonNode below = ok(call(get("/stock/products").param("belowMin", "true"), fx.manager), 200);
        assertThat(below.get("content").findValuesAsText("sku")).containsExactly("CAB-001");
        assertError(send(patch("/stock/products/" + cable + "/min-level"), "{\"minLevel\":-1}"), 400, "VALIDATION_FAILED");
        assertError(send(patch("/stock/products/999999/min-level"), "{\"minLevel\":1}"), 404, "NOT_FOUND");
        assertError(call(get("/stock/products"), fx.allan), 403, "FORBIDDEN");
    }

    @Test
    @DisplayName("[REC-a] entrada recalcula custo medio; custo nulo propaga desconhecido; salePrice muda o preco com motivo")
    void stockEntries() throws Exception {
        long mouse = fx.product("MOU-001", "Mouse", "50.00", 10, "60.00");
        MvcResult result = send(post("/stock/products/" + mouse + "/entries"), """
                {"quantity":10,"unitCost":{"amount":80.00,"currency":"BRL"},"note":"NF 123"}""");
        JsonNode entry = ok(result, 201);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/stock/products/" + mouse + "/entries/" + entry.get("id").asLong());
        assertThat(entry.get("stock").get("onHand").asInt()).isEqualTo(20);
        assertThat(entry.get("stock").get("avgCost").get("amount").decimalValue()).isEqualByComparingTo("70.00");
        assertThat(entry.get("createdBy").get("id").asLong()).isEqualTo(fx.manager);
        assertThat(entry.get("product").get("sku").asText()).isEqualTo("MOU-001");

        JsonNode priced = ok(send(post("/stock/products/" + mouse + "/entries"), """
                {"quantity":5,"unitCost":null,"salePrice":{"amount":65.00,"currency":"BRL"}}"""), 201);
        assertThat(priced.get("unitCost").isNull()).isTrue();
        assertThat(priced.get("stock").get("avgCost").isNull()).isTrue();
        assertThat(jdbc.sql("SELECT reason FROM product_price_history WHERE product_id = :p ORDER BY id DESC LIMIT 1")
                .param("p", mouse).query(String.class).single()).isEqualTo("Entrada de estoque");
        assertThat(fx.count("SELECT price * 100 FROM product WHERE id = ?", mouse)).isEqualTo(6500);

        assertError(send(post("/stock/products/" + mouse + "/entries"), "{\"quantity\":0,\"unitCost\":null}"), 400, "VALIDATION_FAILED");
        assertError(send(post("/stock/products/999999/entries"), "{\"quantity\":1,\"unitCost\":null}"), 404, "NOT_FOUND");
    }

    @Test
    @DisplayName("[CT-STOCK-108] ajuste: saida acima do disponivel 409; entrada so INVENTARIO (C-15); custo medio nao muda")
    void stockAdjustments() throws Exception {
        long mouse = fx.product("MOU-001", "Mouse", "50.00", 5, "20.00");
        fx.set("UPDATE stock_balance SET reserved = 3 WHERE product_id = ?", mouse);

        assertError(send(post("/stock/products/" + mouse + "/adjustments"), """
                {"direction":"OUT","quantity":3,"reason":"AVARIA"}"""), 409, "STOCK_INSUFFICIENT");
        assertThat(fx.balance(mouse)).isEqualTo("5/3");

        MvcResult out = send(post("/stock/products/" + mouse + "/adjustments"), """
                {"direction":"OUT","quantity":2,"reason":"AVARIA","note":"Caixa molhada"}""");
        JsonNode movement = ok(out, 201);
        assertThat(movement.get("kind").asText()).isEqualTo("AJUSTE_SAIDA");
        assertThat(movement.get("reason").asText()).isEqualTo("AVARIA");
        assertThat(movement.get("note").asText()).isEqualTo("Caixa molhada");
        assertThat(movement.get("createdBy").get("name").asText()).isEqualTo("Matheus Gerente");
        assertThat(movement.get("orderId").isNull()).isTrue();
        assertThat(fx.balance(mouse)).isEqualTo("3/3");

        assertError(send(post("/stock/products/" + mouse + "/adjustments"), """
                {"direction":"IN","quantity":4,"reason":"PERDA"}"""), 400, "VALIDATION_FAILED");
        assertError(send(post("/stock/products/" + mouse + "/adjustments"), """
                {"direction":"IN","quantity":4}"""), 400, "VALIDATION_FAILED");
        assertError(send(post("/stock/products/" + mouse + "/adjustments"), """
                {"direction":"SIDEWAYS","quantity":4,"reason":"INVENTARIO"}"""), 400, "MALFORMED_REQUEST");
        ok(send(post("/stock/products/" + mouse + "/adjustments"), """
                {"direction":"IN","quantity":4,"reason":"INVENTARIO"}"""), 201);
        assertThat(fx.balance(mouse)).isEqualTo("7/3");
        assertThat(fx.count("SELECT avg_cost * 100 FROM stock_balance WHERE product_id = ?", mouse)).isEqualTo(2000);
        assertThat(fx.count("SELECT count(*) FROM audit_event WHERE action = 'STOCK_ADJUSTED'")).isEqualTo(2);
    }

    @Test
    @DisplayName("[RF-22] extrato com filtros de produto, tipo e periodo no fuso da loja")
    void movements() throws Exception {
        long mouse = fx.product("MOU-001", "Mouse", "50.00", 5, "20.00");
        long cable = fx.product("CAB-001", "Cabo", "10.00", 5, "4.00");
        ok(send(post("/stock/products/" + mouse + "/adjustments"), "{\"direction\":\"OUT\",\"quantity\":1,\"reason\":\"PERDA\"}"), 201);

        assertThat(ok(call(get("/stock/movements"), fx.manager), 200).get("totalElements").asLong()).isEqualTo(3);
        JsonNode byProduct = ok(call(get("/stock/movements").param("productId", Long.toString(mouse)), fx.manager), 200);
        assertThat(byProduct.get("content").findValuesAsText("kind")).containsExactly("AJUSTE_SAIDA", "ENTRADA");
        JsonNode entries = ok(call(get("/stock/movements").param("kind", "ENTRADA").param("size", "1"), fx.manager), 200);
        assertThat(entries.get("totalElements").asLong()).isEqualTo(2);
        assertThat(entries.get("content")).hasSize(1);
        assertThat(entries.get("content").get(0).get("product").get("id").asLong()).isEqualTo(cable);

        String today = LocalDate.now(com.swee.ordermanagementspring.entities.StoreTime.clock()).toString();
        assertThat(ok(call(get("/stock/movements").param("from", today).param("to", today), fx.manager), 200)
                .get("totalElements").asLong()).isEqualTo(3);
        String tomorrow = LocalDate.parse(today).plusDays(1).toString();
        assertThat(ok(call(get("/stock/movements").param("from", tomorrow), fx.manager), 200).get("totalElements").asLong()).isZero();
        assertError(call(get("/stock/movements").param("from", tomorrow).param("to", today), fx.manager), 400, "VALIDATION_FAILED");
        assertError(call(get("/stock/movements").param("kind", "ROUBO"), fx.manager), 400, "MALFORMED_REQUEST");
        assertError(call(get("/stock/movements").param("from", "14/10/2026"), fx.manager), 400, "MALFORMED_REQUEST");
    }
}
