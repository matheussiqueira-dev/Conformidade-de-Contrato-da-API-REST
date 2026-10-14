package com.swee.ordermanagementspring.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swee.ordermanagementspring.config.BusinessTables;
import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.audit.AuditEvent;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import com.swee.ordermanagementspring.repositories.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * [CT-AUTH-102] Login real com banco: 5 senhas erradas e a 6a correta -> 401 com corpo igual (salvo timestamp)
 * ao de usuario inexistente; apos 15 min (Clock) -> 200. Conta inativa -> o mesmo 401.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
@Import(LoginLockoutIntegrationTest.ClockConfig.class)
class LoginLockoutIntegrationTest {

    private static final String PASSWORD = "senha-sintetica-correta";

    @TestConfiguration
    static class ClockConfig {
        @Bean @Primary LoginLockoutPolicyTest.MutableClock testClock() {
            return new LoginLockoutPolicyTest.MutableClock(Instant.parse("2026-10-14T12:00:00Z"));
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired AppUserRepository users;
    @Autowired AuditEventRepository audit;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired LoginLockoutPolicyTest.MutableClock clock;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        BusinessTables.truncate(jdbc);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String hash = encoder.encode(PASSWORD);
        users.save(new AppUser("Allan", "allan@example.test", hash, UserRole.VENDEDOR));
        AppUser inactive = new AppUser("Bia", "bia@example.test", hash, UserRole.VENDEDOR);
        inactive.deactivate();
        users.save(inactive);
    }

    private MvcResult login(String email, String password) throws Exception {
        MvcResult csrf = mvc.perform(get("/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession();
        String token = json.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        return mvc.perform(post("/auth/login").session(session).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
    }

    private JsonNode bodyWithoutTimestamp(MvcResult result) throws Exception {
        ObjectNode body = (ObjectNode) json.readTree(result.getResponse().getContentAsString());
        assertThat(body.remove("timestamp")).isNotNull();
        return body;
    }

    @Test
    @DisplayName("[CT-AUTH-102] bloqueio apos 5 falhas, corpo igual ao de usuario inexistente, desbloqueio apos 15 min")
    void lockoutIsIndistinguishableAndExpires() throws Exception {
        MvcResult unknown = login("ninguem@example.test", PASSWORD);
        assertThat(unknown.getResponse().getStatus()).isEqualTo(401);
        JsonNode expected = bodyWithoutTimestamp(unknown);
        assertThat(expected.get("code").asText()).isEqualTo("UNAUTHENTICATED");

        for (int i = 0; i < 5; i++) {
            MvcResult wrong = login("allan@example.test", "senha-errada-" + i);
            assertThat(wrong.getResponse().getStatus()).isEqualTo(401);
            assertThat(bodyWithoutTimestamp(wrong)).isEqualTo(expected);
        }
        MvcResult lockedButCorrect = login("allan@example.test", PASSWORD);
        assertThat(lockedButCorrect.getResponse().getStatus()).isEqualTo(401);
        assertThat(bodyWithoutTimestamp(lockedButCorrect)).isEqualTo(expected);

        clock.advance(Duration.ofMinutes(15));
        assertThat(login("allan@example.test", PASSWORD).getResponse().getStatus()).isEqualTo(200);
        AppUser allan = users.findByEmail("allan@example.test").orElseThrow();
        assertThat(allan.getFailedLoginAttempts()).isZero();
        assertThat(allan.getLockedUntil()).isNull();

        List<AuditEvent> failed = audit.findByActionOrderByIdAsc(AuditAction.LOGIN_FAILED);
        assertThat(failed).hasSize(7);
        assertThat(failed).allSatisfy(event -> assertThat(event.getDetail()).doesNotContain("@").doesNotContain("senha")
                .contains("emailSha256"));
        assertThat(audit.findByActionOrderByIdAsc(AuditAction.ACCOUNT_LOCKED)).hasSize(1);
        assertThat(audit.findByActionOrderByIdAsc(AuditAction.LOGIN_SUCCEEDED)).hasSize(1);
    }

    @Test
    @DisplayName("[CT-AUTH-102] conta inativa com senha correta -> o mesmo 401")
    void inactiveAccountGetsTheSame401() throws Exception {
        JsonNode expected = bodyWithoutTimestamp(login("ninguem@example.test", PASSWORD));
        MvcResult inactive = login("bia@example.test", PASSWORD);
        assertThat(inactive.getResponse().getStatus()).isEqualTo(401);
        assertThat(bodyWithoutTimestamp(inactive)).isEqualTo(expected);
    }

    @Test
    @DisplayName("[CA-17-03] login e /auth/me devolvem areas pela hierarquia")
    void loginReturnsAreas() throws Exception {
        MvcResult ok = login("allan@example.test", PASSWORD);
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        JsonNode user = json.readTree(ok.getResponse().getContentAsString()).get("user");
        assertThat(user.get("areas").toString()).isEqualTo("[\"VENDER\",\"VENDAS\",\"CLIENTES\",\"MEU_DESEMPENHO\"]");
        assertThat(user.has("password") || user.has("passwordHash")).isFalse();
    }
}
