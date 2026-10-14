package com.swee.ordermanagementspring.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * [CT-SEC-101] Cada linha da matriz executavel x {anonimo, VENDEDOR, GERENTE} produz o status esperado, com
 * corpo de erro no formato ErrorResponse. [CT-SEC-102] Todo endpoint mapeado no Spring tem linha na matriz.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class AccessMatrixTest {

    /** Objeto inexistente: PERMIT nao pode virar 401/403; 404, 400 e 405 sao aceitos. */
    private static final String MISSING_ID = "999999";
    private static final List<String> ALL_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE");
    /**
     * D-NOITE-05: o /customers legado expoe documento completo. Ate a ETAPA 2C trocar o controlador pelas projecoes
     * mascaradas, o @PreAuthorize do controlador barra o VENDEDOR que a regra de URL ja libera.
     */
    private static final Set<String> SELLER_PENDING_2C = Set.of("GET /customers", "POST /customers",
            "GET /customers/{id}", "PUT /customers/{id}");

    record Row(String method, String path, String anonymous, String vendedor, String gerente) {
        String key() { return method + " " + path; }
    }

    static List<Row> rows() throws IOException {
        try (InputStream in = AccessMatrixTest.class.getResourceAsStream("/security/access-matrix.csv")) {
            assertThat(in).as("access-matrix.csv no classpath de teste").isNotNull();
            List<String> lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().filter(l -> !l.isBlank()).toList();
            assertThat(lines.getFirst()).isEqualTo("method,path,anonymous,vendedor,gerente");
            List<Row> rows = new ArrayList<>();
            for (String line : lines.subList(1, lines.size())) {
                String[] c = line.split(",");
                rows.add(new Row(c[0], c[1], c[2], c[3], c[4]));
            }
            return rows;
        }
    }

    static Stream<Arguments> cases() throws IOException {
        List<Arguments> cases = new ArrayList<>();
        for (Row row : rows()) {
            for (String method : row.method().equals("*") ? ALL_METHODS : List.of(row.method())) {
                cases.add(Arguments.of(method, row.path(), "ANONIMO", row.anonymous(), row.key()));
                cases.add(Arguments.of(method, row.path(), "VENDEDOR", row.vendedor(), row.key()));
                cases.add(Arguments.of(method, row.path(), "GERENTE", row.gerente(), row.key()));
            }
        }
        return cases.stream();
    }

    @Autowired WebApplicationContext context;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static AppPrincipal principal(UserRole role) {
        AppUser user = new AppUser(role.name(), role.name().toLowerCase() + "@example.test", "hash", role);
        ReflectionTestUtils.setField(user, "id", role == UserRole.GERENTE ? 1L : 2L);
        return new AppPrincipal(user);
    }

    @ParameterizedTest(name = "[CT-SEC-101] {0} {1} como {2} -> {3}")
    @MethodSource("cases")
    void matrixRowIsEnforced(String method, String path, String actor, String expected, String key) throws Exception {
        String uri = path.replace("{id}", MISSING_ID).replace("**", "index.html");
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), uri)
                .contentType(MediaType.APPLICATION_JSON).content("{}").with(csrf());
        if (!actor.equals("ANONIMO")) request.with(user(principal(UserRole.valueOf(actor))));
        if (actor.equals("VENDEDOR") && SELLER_PENDING_2C.contains(key)) expected = "403";

        MvcResult result = mvc.perform(request).andReturn();
        int status = result.getResponse().getStatus();
        switch (expected) {
            case "401" -> assertError(result, 401, "UNAUTHENTICATED");
            case "403" -> assertError(result, 403, "FORBIDDEN");
            case "PERMIT", "OWN" -> assertThat(status).as("%s %s como %s", method, uri, actor)
                    .isNotIn(401, 403).isLessThan(500);
            default -> throw new IllegalStateException("Valor desconhecido na matriz: " + expected);
        }
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("code").asText()).isEqualTo(code);
        assertThat(body.has("timestamp") && body.has("error") && body.has("message") && body.has("details")).isTrue();
    }

    @Test
    @DisplayName("[CT-SEC-102] todo endpoint mapeado no Spring tem linha na matriz")
    void everyMappedEndpointHasAMatrixRow() throws IOException {
        List<Row> rows = rows();
        List<String> missing = new ArrayList<>();
        for (RequestMappingHandlerMapping mapping : context.getBeansOfType(RequestMappingHandlerMapping.class).values()) {
            for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
                Set<String> methods = info.getMethodsCondition().getMethods().stream().map(Enum::name)
                        .collect(java.util.stream.Collectors.toSet());
                for (String pattern : info.getPatternValues()) {
                    if (methods.isEmpty()) {
                        if (rows.stream().noneMatch(r -> r.path().equals(pattern) && r.method().equals("*"))) missing.add("* " + pattern);
                    } else {
                        for (String method : methods) {
                            if (rows.stream().noneMatch(r -> r.path().equals(pattern) && (r.method().equals(method) || r.method().equals("*")))) {
                                missing.add(method + " " + pattern);
                            }
                        }
                    }
                }
            }
        }
        assertThat(missing).as("endpoints sem linha em access-matrix.csv").isEmpty();
    }

    @Test
    @DisplayName("[CT-SEC-103] toda linha liberada ao VENDEDOR tambem e liberada ao GERENTE")
    void managerInheritsEverySellerRow() throws IOException {
        for (Row row : rows()) {
            if (row.vendedor().equals("PERMIT") || row.vendedor().equals("OWN")) {
                assertThat(row.gerente()).as(row.key()).isEqualTo("PERMIT");
            }
        }
    }
}
