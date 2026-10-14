package com.swee.ordermanagementspring.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Formato unico de erro com code (A5) para entradas invalidas e [CT-SEC-115] variacoes de caminho e de
 * metodo que nao abrem acesso; rejeicao do firewall responde 400, nunca 500.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class ErrorContractTest {

    @Autowired WebApplicationContext context;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static AppPrincipal as(UserRole role) {
        AppUser user = new AppUser(role.name(), role.name().toLowerCase() + "@example.test", "hash", role);
        ReflectionTestUtils.setField(user, "id", role == UserRole.GERENTE ? 1L : 2L);
        return new AppPrincipal(user);
    }

    private JsonNode expect(MockHttpServletRequestBuilder request, int status, String code) throws Exception {
        MvcResult result = mvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo(code);
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("timestamp").asText()).matches("\\d{4}-\\d{2}-\\d{2}T.*[+-]\\d{2}:\\d{2}");
        assertThat(body.has("details")).isTrue();
        assertThat(body.toString()).doesNotContain("Exception").doesNotContain("SQL").doesNotContain("at com.");
        return body;
    }

    @Test
    @DisplayName("JSON malformado, propriedade desconhecida e tipo errado -> 400 MALFORMED_REQUEST")
    void malformedInputIsA400() throws Exception {
        expect(post("/products").with(user(as(UserRole.GERENTE))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\": "), 400, "MALFORMED_REQUEST");
        expect(post("/products").with(user(as(UserRole.GERENTE))).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"DIGITAL\",\"name\":\"E-book\",\"price\":10,\"downloadLink\":\"https://x.test/a\",\"sellerId\":2}"),
                400, "MALFORMED_REQUEST");
        expect(get("/products/abc").with(user(as(UserRole.GERENTE))), 400, "MALFORMED_REQUEST");
    }

    @Test
    @DisplayName("Bean Validation -> 400 VALIDATION_FAILED com details")
    void validationFailureListsDetails() throws Exception {
        JsonNode body = expect(post("/products").with(user(as(UserRole.GERENTE))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PHYSICAL\"}"), 400, "VALIDATION_FAILED");
        assertThat(body.get("details").isArray()).isTrue();
        assertThat(body.get("details").size()).isPositive();
    }

    @Test
    @DisplayName("Rota inexistente, metodo e midia nao suportados -> 404, 405, 415")
    void routingErrorsUseTheErrorFormat() throws Exception {
        expect(get("/products/1/inexistente").with(user(as(UserRole.GERENTE))), 404, "NOT_FOUND");
        expect(get("/products/999999").with(user(as(UserRole.GERENTE))), 404, "NOT_FOUND");
        expect(patch("/products/1").with(user(as(UserRole.GERENTE))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"), 405, "METHOD_NOT_ALLOWED");
        expect(post("/products").with(user(as(UserRole.GERENTE))).with(csrf())
                .contentType(MediaType.TEXT_PLAIN).content("x"), 415, "UNSUPPORTED_MEDIA_TYPE");
        expect(get("/error"), 404, "NOT_FOUND");
    }

    @Test
    @DisplayName("[CT-SEC-115] firewall rejeita ';' e '%2e%2e' com 400 no formato ErrorResponse")
    void firewallRejectionIsA400() throws Exception {
        expect(get("/products;jsessionid=abc").with(user(as(UserRole.GERENTE))), 400, "MALFORMED_REQUEST");
        expect(get("/sales/catalog/%2e%2e/stock/products").with(user(as(UserRole.VENDEDOR))), 400, "MALFORMED_REQUEST");
    }

    @Test
    @DisplayName("[CT-SEC-115] barra final, maiusculas e X-HTTP-Method-Override nao abrem acesso ao VENDEDOR")
    void pathAndMethodVariantsDoNotOpenAccess() throws Exception {
        expect(get("/stock/products/").with(user(as(UserRole.VENDEDOR))), 403, "FORBIDDEN");
        expect(get("/STOCK/products").with(user(as(UserRole.VENDEDOR))), 403, "FORBIDDEN");
        expect(get("/Products").with(user(as(UserRole.VENDEDOR))), 403, "FORBIDDEN");
        expect(post("/customers/999999").header("X-HTTP-Method-Override", "DELETE").with(user(as(UserRole.VENDEDOR)))
                .with(csrf()), 403, "FORBIDDEN");
        expect(get("/products").header("X-HTTP-Method-Override", "GET").with(user(as(UserRole.VENDEDOR))), 403, "FORBIDDEN");
    }

    @Test
    @DisplayName("[CT-SEC-104] mutacao sem CSRF -> 403 FORBIDDEN, mesmo para o GERENTE")
    void mutationWithoutCsrfIsForbidden() throws Exception {
        expect(post("/products").with(user(as(UserRole.GERENTE))).contentType(MediaType.APPLICATION_JSON).content("{}"),
                403, "FORBIDDEN");
        expect(post("/products").with(user(as(UserRole.GERENTE))).with(csrf().useInvalidToken())
                .contentType(MediaType.APPLICATION_JSON).content("{}"), 403, "FORBIDDEN");
    }
}
