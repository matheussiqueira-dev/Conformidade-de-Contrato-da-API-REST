package com.swee.ordermanagementspring.api.promotions;

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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Promocoes (RF-23): liberacao pelo GERENTE, vigentes para o balcao, encerramento e escopo (BD-27, C-08, C-09). */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class PromotionsApiTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate template;
    @Autowired JdbcClient jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private PdvFixtures fx;
    private long mouse;
    private long cable;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fx = new PdvFixtures(template, jdbc);
        mouse = fx.product("MOU-001", "Mouse", "100.00", 10, "60.00");
        long other = jdbc.sql("INSERT INTO category(name) VALUES ('Cabos') RETURNING id").query(Long.class).single();
        cable = fx.product("CAB-001", "Cabo", "20.00", 10, "5.00");
        fx.set("UPDATE product SET category_id = ? WHERE id = ?", other, cable);
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

    private static String window(int startDays, int endDays) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.ofHours(-3)).withNano(0);
        return "\"startsAt\":\"%s\",\"endsAt\":\"%s\"".formatted(now.plusDays(startDays), now.plusDays(endDays));
    }

    private MvcResult create(String body, long actor) throws Exception {
        return call(post("/promotions").contentType(MediaType.APPLICATION_JSON).content(body), actor);
    }

    @Test
    @DisplayName("[RF-23] gerente libera; VENDEDOR 403; PERCENT acima de 100, janela invertida ou dois escopos 400")
    void createRules() throws Exception {
        MvcResult result = create("{\"name\":\"Mouse 10%%\",\"kind\":\"PERCENT\",\"value\":10,\"productId\":%d,%s}"
                .formatted(mouse, window(-1, 5)), fx.manager);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode promo = body(result);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/promotions/" + promo.get("id").asLong());
        assertThat(promo.get("product").get("sku").asText()).isEqualTo("MOU-001");
        assertThat(promo.get("category").isNull()).isTrue();
        assertThat(promo.get("createdBy").get("id").asLong()).isEqualTo(fx.manager);
        assertThat(promo.get("active").asBoolean()).isTrue();

        assertError(create("{\"name\":\"Mouse\",\"kind\":\"PERCENT\",\"value\":10,%s}".formatted(window(-1, 5)), fx.ana), 403, "FORBIDDEN");
        assertError(create("{\"name\":\"Demais\",\"kind\":\"PERCENT\",\"value\":101,%s}".formatted(window(-1, 5)), fx.manager),
                400, "VALIDATION_FAILED");
        assertError(create("{\"name\":\"Invertida\",\"kind\":\"AMOUNT\",\"value\":5,%s}".formatted(window(5, 1)), fx.manager),
                400, "VALIDATION_FAILED");
        assertError(create("{\"name\":\"Dois escopos\",\"kind\":\"AMOUNT\",\"value\":5,\"productId\":%d,\"categoryId\":%d,%s}"
                .formatted(mouse, fx.category, window(-1, 5)), fx.manager), 400, "VALIDATION_FAILED");
        assertError(create("{\"name\":\"Sem produto\",\"kind\":\"AMOUNT\",\"value\":5,\"productId\":999999,%s}"
                .formatted(window(-1, 5)), fx.manager), 404, "NOT_FOUND");
        assertError(create("{\"name\":\"Tipo\",\"kind\":\"BOGO\",\"value\":5,%s}".formatted(window(-1, 5)), fx.manager),
                400, "MALFORMED_REQUEST");
    }

    @Test
    @DisplayName("[BD-27, C-08] vigentes por produto: do produto, da categoria dele ou da loja; fora do escopo nao aparece")
    void activeByProductScope() throws Exception {
        long product = body(create("{\"name\":\"Mouse\",\"kind\":\"PERCENT\",\"value\":10,\"productId\":%d,%s}"
                .formatted(mouse, window(-1, 5)), fx.manager)).get("id").asLong();
        long category = body(create("{\"name\":\"Cabos\",\"kind\":\"AMOUNT\",\"value\":2,\"categoryId\":%d,%s}"
                .formatted(fx.count("SELECT category_id FROM product WHERE id = ?", cable), window(-1, 5)), fx.manager)).get("id").asLong();
        long store = body(create("{\"name\":\"Loja\",\"kind\":\"PERCENT\",\"value\":5,%s}".formatted(window(-1, 5)), fx.manager))
                .get("id").asLong();
        create("{\"name\":\"Futura\",\"kind\":\"PERCENT\",\"value\":50,%s}".formatted(window(2, 5)), fx.manager);

        JsonNode forMouse = body(call(get("/promotions/active").param("productId", Long.toString(mouse)), fx.ana));
        assertThat(forMouse.findValues("id").stream().map(JsonNode::asLong).toList()).containsExactly(product, store);
        JsonNode forCable = body(call(get("/promotions/active").param("productId", Long.toString(cable)), fx.ana));
        assertThat(forCable.findValues("id").stream().map(JsonNode::asLong).toList()).containsExactly(category, store);
        assertThat(body(call(get("/promotions/active"), fx.ana))).hasSize(3);

        // C-08: promocao de outro produto no pedido responde 409.
        MvcResult wrong = call(post("/sales/orders").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":%d,"items":[{"productId":%d,"quantity":1,"promotionId":%d}],"payment":{"type":"PIX"},"fulfillment":"PICKUP"}"""
                        .formatted(fx.customer, cable, product)), fx.ana);
        assertError(wrong, 409, "STATE_CONFLICT");
    }

    @Test
    @DisplayName("[RF-23] encerrar tira das vigentes; repetir nao tem efeito; lista filtra por situacao")
    void closeAndList() throws Exception {
        long id = body(create("{\"name\":\"Loja\",\"kind\":\"PERCENT\",\"value\":5,%s}".formatted(window(-1, 5)), fx.manager))
                .get("id").asLong();
        create("{\"name\":\"Outra\",\"kind\":\"AMOUNT\",\"value\":1,%s}".formatted(window(-1, 5)), fx.manager);

        MvcResult closed = call(post("/promotions/" + id + "/close"), fx.manager);
        assertThat(closed.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(closed).get("active").asBoolean()).isFalse();
        assertThat(call(post("/promotions/" + id + "/close"), fx.manager).getResponse().getStatus()).isEqualTo(200);
        assertError(call(post("/promotions/999999/close"), fx.manager), 404, "NOT_FOUND");
        assertError(call(post("/promotions/" + id + "/close"), fx.ana), 403, "FORBIDDEN");

        assertThat(body(call(get("/promotions/active"), fx.ana))).hasSize(1);
        assertThat(body(call(get("/promotions"), fx.manager)).get("totalElements").asLong()).isEqualTo(2);
        assertThat(body(call(get("/promotions").param("active", "false"), fx.manager)).get("content").get(0).get("id").asLong()).isEqualTo(id);
        assertError(call(get("/promotions"), fx.ana), 403, "FORBIDDEN");
    }
}
