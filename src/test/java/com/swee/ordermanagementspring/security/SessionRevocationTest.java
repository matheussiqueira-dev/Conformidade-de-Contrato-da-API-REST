package com.swee.ordermanagementspring.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swee.ordermanagementspring.config.BusinessTables;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * G-01 com login real por sessao: a conta alterada no banco depois do login perde a sessao no request seguinte.
 * As operacoes de /users que incrementam a versao sao cobertas em UsersApiTest; aqui a alteracao e feita por SQL
 * para isolar o filtro.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class SessionRevocationTest {

    private static final String PASSWORD = "senha-sintetica-correta";

    @Autowired WebApplicationContext context;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private long seller;
    private long secondManager;

    @BeforeEach
    void setUp() {
        BusinessTables.truncate(jdbc);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String hash = encoder.encode(PASSWORD);
        users.save(new AppUser("Rafael Gerente", "rafael@example.test", hash, UserRole.GERENTE));
        secondManager = users.save(new AppUser("Paula Gerente", "paula@example.test", hash, UserRole.GERENTE)).getId();
        seller = users.save(new AppUser("Ana Souza", "ana@example.test", hash, UserRole.VENDEDOR)).getId();
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult csrf = mvc.perform(get("/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession();
        String token = json.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        MvcResult login = mvc.perform(post("/auth/login").session(session).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertThat(login.getResponse().getStatus()).as("login %s", email).isEqualTo(200);
        return session;
    }

    private MvcResult call(String path, MockHttpSession session) throws Exception {
        return mvc.perform(get(path).session(session)).andReturn();
    }

    private static JsonNode body(MvcResult result) throws Exception {
        return new ObjectMapper().readTree(result.getResponse().getContentAsString());
    }

    private static void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        assertThat(body(result).get("code").asText()).isEqualTo(code);
    }

    @Test
    @DisplayName("[CT-SEC-121] vendedor desativado perde a sessao no proximo request (401 SESSION_REVOKED)")
    void deactivatedUserLosesTheSession() throws Exception {
        MockHttpSession session = login("ana@example.test");
        assertThat(call("/sales/catalog", session).getResponse().getStatus()).isEqualTo(200);

        jdbc.update("UPDATE app_users SET active = false, session_version = session_version + 1 WHERE id = ?", seller);

        assertError(call("/sales/catalog", session), 401, "SESSION_REVOKED");
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("[CT-SEC-121] conta inativa perde a sessao mesmo sem mudanca de versao")
    void inactiveAccountIsRevokedEvenWithTheSameVersion() throws Exception {
        MockHttpSession session = login("ana@example.test");
        jdbc.update("UPDATE app_users SET active = false WHERE id = ?", seller);
        assertError(call("/auth/me", session), 401, "SESSION_REVOKED");
    }

    @Test
    @DisplayName("[CT-SEC-122] gerente rebaixado recebe 401 e, apos novo login, 403 em /products")
    void demotedManagerLosesManagerAccess() throws Exception {
        MockHttpSession session = login("paula@example.test");
        assertThat(call("/products", session).getResponse().getStatus()).isEqualTo(200);

        jdbc.update("UPDATE app_users SET role = 'VENDEDOR', session_version = session_version + 1 WHERE id = ?", secondManager);

        assertError(call("/products", session), 401, "SESSION_REVOKED");
        MockHttpSession again = login("paula@example.test");
        assertError(call("/products", again), 403, "FORBIDDEN");
        assertThat(call("/sales/catalog", again).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("[CT-SEC-121] conta apagada perde a sessao")
    void deletedAccountLosesTheSession() throws Exception {
        MockHttpSession session = login("ana@example.test");
        jdbc.update("DELETE FROM audit_event WHERE actor_id = ?", seller);
        jdbc.update("DELETE FROM app_users WHERE id = ?", seller);
        assertError(call("/auth/me", session), 401, "SESSION_REVOKED");
    }

    @Test
    @DisplayName("[CT-USR-106] senha temporaria: areas so TROCAR_SENHA e 403 PASSWORD_CHANGE_REQUIRED fora da conta")
    void temporaryPasswordAllowsOnlyAccountRoutes() throws Exception {
        jdbc.update("UPDATE app_users SET must_change_password = true WHERE id = ?", seller);
        MockHttpSession session = login("ana@example.test");

        MvcResult me = call("/auth/me", session);
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(me).get("areas").toString()).isEqualTo("[\"TROCAR_SENHA\"]");
        assertThat(call("/auth/csrf", session).getResponse().getStatus()).isEqualTo(200);
        assertError(call("/sales/catalog", session), 403, "PASSWORD_CHANGE_REQUIRED");
        assertError(call("/customers", session), 403, "PASSWORD_CHANGE_REQUIRED");
    }
}
