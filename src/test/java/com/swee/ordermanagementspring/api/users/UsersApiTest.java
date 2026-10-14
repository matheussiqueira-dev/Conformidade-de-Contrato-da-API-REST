package com.swee.ordermanagementspring.api.users;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** Cadastro de usuarios (G-03) e revogacao de sessao pelas operacoes de /users (G-01), com login real por sessao. */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class UsersApiTest {

    private static final String PASSWORD = "senha-sintetica-correta";
    private static final String TEMPORARY = "Provisoria-Loja-2026";
    private static final String NEW_PASSWORD = "Nova-Senha-Da-Ana-77";

    @Autowired WebApplicationContext context;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();
    private final List<String> responses = java.util.Collections.synchronizedList(new ArrayList<>());
    private MockMvc mvc;
    private long rafael;
    private long paula;
    private long ana;

    @BeforeEach
    void setUp() {
        BusinessTables.truncate(jdbc);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String hash = encoder.encode(PASSWORD);
        rafael = users.save(new AppUser("Rafael Mendes", "rafael@example.test", hash, UserRole.GERENTE)).getId();
        paula = users.save(new AppUser("Paula Duarte", "paula@example.test", hash, UserRole.GERENTE)).getId();
        ana = users.save(new AppUser("Ana Souza", "ana@example.test", hash, UserRole.VENDEDOR)).getId();
    }

    private MvcResult login(String email, String password) throws Exception {
        MvcResult csrf = mvc.perform(get("/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession();
        String token = json.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        return mvc.perform(post("/auth/login").session(session).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
    }

    private MockHttpSession session(String email, String password) throws Exception {
        MvcResult result = login(email, password);
        assertThat(result.getResponse().getStatus()).as("login %s", email).isEqualTo(200);
        return (MockHttpSession) result.getRequest().getSession();
    }

    private MockHttpSession session(String email) throws Exception {
        return session(email, PASSWORD);
    }

    private MvcResult call(HttpMethod method, String path, MockHttpSession session, String body) throws Exception {
        var request = request(method, path).session(session).with(csrf());
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        MvcResult result = mvc.perform(request).andReturn();
        responses.add(result.getResponse().getContentAsString());
        return result;
    }

    private MvcResult call(HttpMethod method, String path, MockHttpSession session) throws Exception {
        return call(method, path, session, null);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        assertThat(body(result).get("code").asText()).isEqualTo(code);
    }

    private static String create(String name, String email, String role, String password) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"role\":\"" + role
                + "\",\"temporaryPassword\":\"" + password + "\"}";
    }

    private long createUser(MockHttpSession manager, String email) throws Exception {
        MvcResult created = call(HttpMethod.POST, "/users", manager, create("Bruno Lima", email, "VENDEDOR", TEMPORARY));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        return body(created).get("id").asLong();
    }

    @Test
    @DisplayName("[CT-USR-101] cadastro normaliza o e-mail, exige troca de senha e rejeita e-mail duplicado em outra caixa")
    void createUserAndRejectDuplicateEmail() throws Exception {
        MockHttpSession manager = session("rafael@example.test");
        MvcResult created = call(HttpMethod.POST, "/users", manager, create("  Bruno Lima ", "Bruno@Example.TEST", "VENDEDOR", TEMPORARY));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        JsonNode user = body(created);
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/users/" + user.get("id").asLong());
        assertThat(user.get("email").asText()).isEqualTo("bruno@example.test");
        assertThat(user.get("name").asText()).isEqualTo("Bruno Lima");
        assertThat(user.get("mustChangePassword").asBoolean()).isTrue();
        assertThat(user.get("active").asBoolean()).isTrue();
        assertThat(user.has("password") || user.has("passwordHash")).isFalse();

        assertError(call(HttpMethod.POST, "/users", manager, create("Bruno Dois", "BRUNO@example.test", "VENDEDOR", TEMPORARY)),
                409, "STATE_CONFLICT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_users WHERE lower(email) = 'bruno@example.test'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action = 'USER_CREATED'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("[CT-USR-101] politica de senha: curta, comum, igual ao e-mail e acima de 72 bytes -> 400 VALIDATION_FAILED")
    void temporaryPasswordFollowsThePolicy() throws Exception {
        MockHttpSession manager = session("rafael@example.test");
        for (String weak : List.of("curta-123", "password1234", "Qwertyuiop12", "aaaaaaaaaaaaaa",
                "bruno@example.test", "ç".repeat(40))) {
            assertError(call(HttpMethod.POST, "/users", manager, create("Bruno Lima", "bruno@example.test", "VENDEDOR", weak)),
                    400, "VALIDATION_FAILED");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_users WHERE email = 'bruno@example.test'", Integer.class)).isZero();
    }

    @Test
    @DisplayName("[CT-USR-102] VENDEDOR recebe 403 em todas as rotas de /users")
    void sellerIsForbiddenEverywhere() throws Exception {
        MockHttpSession seller = session("ana@example.test");
        String path = "/users/" + rafael;
        assertError(call(HttpMethod.GET, "/users", seller), 403, "FORBIDDEN");
        assertError(call(HttpMethod.POST, "/users", seller, create("Bruno Lima", "bruno@example.test", "VENDEDOR", TEMPORARY)), 403, "FORBIDDEN");
        assertError(call(HttpMethod.GET, path, seller), 403, "FORBIDDEN");
        assertError(call(HttpMethod.PUT, path, seller, "{\"name\":\"Rafael\",\"email\":\"r@example.test\",\"version\":0}"), 403, "FORBIDDEN");
        assertError(call(HttpMethod.PATCH, path + "/role", seller, "{\"role\":\"VENDEDOR\",\"reason\":\"Teste de acesso\"}"), 403, "FORBIDDEN");
        assertError(call(HttpMethod.PATCH, path + "/status", seller, "{\"active\":false,\"reason\":\"Teste de acesso\"}"), 403, "FORBIDDEN");
        assertError(call(HttpMethod.POST, path + "/unlock", seller), 403, "FORBIDDEN");
        assertError(call(HttpMethod.POST, path + "/password-reset", seller, "{\"temporaryPassword\":\"" + TEMPORARY + "\"}"), 403, "FORBIDDEN");
        assertThat(jdbc.queryForObject("SELECT role FROM app_users WHERE id = ?", String.class, rafael)).isEqualTo("GERENTE");
    }

    @Test
    @DisplayName("[CT-USR-103] dois gerentes rebaixando um ao outro ao mesmo tempo: um 200 e um 409 LAST_MANAGER")
    void lastManagerIsKeptUnderConcurrency() throws Exception {
        MockHttpSession rafaelSession = session("rafael@example.test");
        MockHttpSession paulaSession = session("paula@example.test");
        String body = "{\"role\":\"VENDEDOR\",\"reason\":\"Reorganizacao da equipe\"}";
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<MvcResult> first = pool.submit(() -> { start.await(); return call(HttpMethod.PATCH, "/users/" + paula + "/role", rafaelSession, body); });
            Future<MvcResult> second = pool.submit(() -> { start.await(); return call(HttpMethod.PATCH, "/users/" + rafael + "/role", paulaSession, body); });
            start.countDown();
            List<MvcResult> results = List.of(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS));
            assertThat(results.stream().map(r -> r.getResponse().getStatus()).sorted().toList()).containsExactly(200, 409);
            MvcResult conflict = results.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
            assertThat(body(conflict).get("code").asText()).isEqualTo("LAST_MANAGER");
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_users WHERE role = 'GERENTE' AND active", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("[CT-USR-104] GERENTE nao altera o proprio papel, a propria situacao nem redefine a propria senha")
    void selfChangesAreForbidden() throws Exception {
        MockHttpSession manager = session("rafael@example.test");
        String self = "/users/" + rafael;
        assertError(call(HttpMethod.PATCH, self + "/role", manager, "{\"role\":\"VENDEDOR\",\"reason\":\"Autorrebaixamento\"}"), 409, "SELF_CHANGE_FORBIDDEN");
        assertError(call(HttpMethod.PATCH, self + "/status", manager, "{\"active\":false,\"reason\":\"Autodesativacao\"}"), 409, "SELF_CHANGE_FORBIDDEN");
        assertError(call(HttpMethod.POST, self + "/password-reset", manager, "{\"temporaryPassword\":\"" + TEMPORARY + "\"}"), 409, "SELF_CHANGE_FORBIDDEN");
        assertThat(call(HttpMethod.GET, "/auth/me", manager).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("[CT-USR-105] desbloqueio pelo GERENTE permite login imediato e e auditado")
    void unlockAllowsImmediateLogin() throws Exception {
        for (int i = 0; i < 5; i++) assertThat(login("ana@example.test", "senha-errada-" + i).getResponse().getStatus()).isEqualTo(401);
        assertThat(login("ana@example.test", PASSWORD).getResponse().getStatus()).isEqualTo(401);

        MockHttpSession manager = session("rafael@example.test");
        MvcResult detail = call(HttpMethod.GET, "/users/" + ana, manager);
        assertThat(body(detail).get("lockedUntil").isNull()).isFalse();
        assertThat(call(HttpMethod.POST, "/users/" + ana + "/unlock", manager).getResponse().getStatus()).isEqualTo(204);

        assertThat(login("ana@example.test", PASSWORD).getResponse().getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action = 'ACCOUNT_UNLOCKED' AND entity_id = ?",
                Integer.class, ana)).isEqualTo(1);
        assertError(call(HttpMethod.POST, "/users/999999/unlock", manager), 404, "NOT_FOUND");
    }

    @Test
    @DisplayName("[CT-USR-106] senha temporaria obriga a troca; a troca renova a sessao e derruba as outras")
    void temporaryPasswordMustBeChanged() throws Exception {
        MockHttpSession manager = session("rafael@example.test");
        long bruno = createUser(manager, "bruno@example.test");

        MockHttpSession first = session("bruno@example.test", TEMPORARY);
        MockHttpSession second = session("bruno@example.test", TEMPORARY);
        assertThat(body(call(HttpMethod.GET, "/auth/me", second)).get("areas").toString()).isEqualTo("[\"TROCAR_SENHA\"]");
        assertError(call(HttpMethod.GET, "/sales/catalog", second), 403, "PASSWORD_CHANGE_REQUIRED");

        assertError(call(HttpMethod.PUT, "/auth/password", second,
                "{\"currentPassword\":\"" + TEMPORARY + "\",\"newPassword\":\"bruno@example.test\"}"), 400, "VALIDATION_FAILED");
        assertError(call(HttpMethod.PUT, "/auth/password", second,
                "{\"currentPassword\":\"" + TEMPORARY + "\",\"newPassword\":\"" + TEMPORARY + "\"}"), 400, "VALIDATION_FAILED");
        assertError(call(HttpMethod.PUT, "/auth/password", second,
                "{\"currentPassword\":\"errada\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"), 401, "UNAUTHENTICATED");
        assertThat(jdbc.queryForObject("SELECT failed_login_attempts FROM app_users WHERE id = ?", Integer.class, bruno)).isEqualTo(1);

        MvcResult changed = call(HttpMethod.PUT, "/auth/password", second,
                "{\"currentPassword\":\"" + TEMPORARY + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        assertThat(changed.getResponse().getStatus()).isEqualTo(204);
        assertThat(call(HttpMethod.GET, "/sales/catalog", second).getResponse().getStatus()).isEqualTo(200);
        assertThat(body(call(HttpMethod.GET, "/auth/me", second)).get("areas").toString()).contains("VENDER");
        assertError(call(HttpMethod.GET, "/auth/me", first), 401, "SESSION_REVOKED");

        assertThat(login("bruno@example.test", TEMPORARY).getResponse().getStatus()).isEqualTo(401);
        assertThat(login("bruno@example.test", NEW_PASSWORD).getResponse().getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT failed_login_attempts FROM app_users WHERE id = ?", Integer.class, bruno)).isZero();
    }

    @Test
    @DisplayName("[CT-USR-106] redefinicao pelo GERENTE derruba a sessao e volta a exigir a troca")
    void managerResetRevokesAndRequiresChange() throws Exception {
        MockHttpSession seller = session("ana@example.test");
        MockHttpSession manager = session("rafael@example.test");
        assertThat(call(HttpMethod.POST, "/users/" + ana + "/password-reset", manager,
                "{\"temporaryPassword\":\"" + TEMPORARY + "\"}").getResponse().getStatus()).isEqualTo(204);
        assertError(call(HttpMethod.GET, "/sales/catalog", seller), 401, "SESSION_REVOKED");
        assertThat(login("ana@example.test", PASSWORD).getResponse().getStatus()).isEqualTo(401);
        MockHttpSession again = session("ana@example.test", TEMPORARY);
        assertError(call(HttpMethod.GET, "/sales/catalog", again), 403, "PASSWORD_CHANGE_REQUIRED");
    }

    @Test
    @DisplayName("[CT-USR-106] cinco senhas atuais erradas bloqueiam a conta e encerram a sessao")
    void wrongCurrentPasswordLocksAndRevokes() throws Exception {
        MockHttpSession seller = session("ana@example.test");
        String body = "{\"currentPassword\":\"errada\",\"newPassword\":\"" + NEW_PASSWORD + "\"}";
        for (int i = 0; i < 5; i++) assertError(call(HttpMethod.PUT, "/auth/password", seller, body), 401, "UNAUTHENTICATED");
        assertError(call(HttpMethod.GET, "/auth/me", seller), 401, "SESSION_REVOKED");
        assertThat(login("ana@example.test", PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action = 'ACCOUNT_LOCKED'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("[CT-SEC-121] desativar pela API derruba a sessao do vendedor e impede novo login")
    void deactivationRevokesTheSession() throws Exception {
        MockHttpSession seller = session("ana@example.test");
        MockHttpSession manager = session("rafael@example.test");
        MvcResult changed = call(HttpMethod.PATCH, "/users/" + ana + "/status", manager, "{\"active\":false,\"reason\":\"Desligamento\"}");
        assertThat(changed.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(changed).get("active").asBoolean()).isFalse();
        assertError(call(HttpMethod.GET, "/sales/catalog", seller), 401, "SESSION_REVOKED");
        assertThat(login("ana@example.test", PASSWORD).getResponse().getStatus()).isEqualTo(401);
        String detail = jdbc.queryForObject("SELECT detail::text FROM audit_event WHERE action = 'USER_STATUS_CHANGED'", String.class);
        assertThat(detail).contains("Desligamento").contains("false");
    }

    @Test
    @DisplayName("[CT-SEC-122] rebaixar pela API derruba a sessao; no novo login, 403 em /products")
    void demotionRevokesTheSession() throws Exception {
        MockHttpSession demoted = session("paula@example.test");
        assertThat(call(HttpMethod.GET, "/products", demoted).getResponse().getStatus()).isEqualTo(200);
        MockHttpSession manager = session("rafael@example.test");
        MvcResult changed = call(HttpMethod.PATCH, "/users/" + paula + "/role", manager, "{\"role\":\"VENDEDOR\",\"reason\":\"Mudanca de funcao\"}");
        assertThat(changed.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(changed).get("role").asText()).isEqualTo("VENDEDOR");

        assertError(call(HttpMethod.GET, "/products", demoted), 401, "SESSION_REVOKED");
        MockHttpSession again = session("paula@example.test");
        assertError(call(HttpMethod.GET, "/products", again), 403, "FORBIDDEN");
    }

    @Test
    @DisplayName("[CT-USR-101] busca com filtros, detalhe, edicao com versao e 404")
    void listUpdateAndVersion() throws Exception {
        MockHttpSession manager = session("rafael@example.test");
        JsonNode page = body(call(HttpMethod.GET, "/users?role=GERENTE&active=true&q=pa", manager));
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("email").asText()).isEqualTo("paula@example.test");
        assertThat(body(call(HttpMethod.GET, "/users?q=%25", manager)).get("totalElements").asLong()).isZero();

        JsonNode detail = body(call(HttpMethod.GET, "/users/" + ana, manager));
        int version = detail.get("version").asInt();
        String edit = "{\"name\":\"Ana Souza Lima\",\"email\":\"Ana.Lima@example.test\",\"version\":" + version + "}";
        MvcResult updated = call(HttpMethod.PUT, "/users/" + ana, manager, edit);
        assertThat(updated.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(updated).get("email").asText()).isEqualTo("ana.lima@example.test");
        assertThat(body(updated).get("version").asInt()).isEqualTo(version + 1);
        assertError(call(HttpMethod.PUT, "/users/" + ana, manager, edit), 409, "CONCURRENT_UPDATE");
        assertError(call(HttpMethod.GET, "/users/999999", manager), 404, "NOT_FOUND");
        assertError(call(HttpMethod.POST, "/users", manager,
                "{\"name\":\"X\",\"email\":\"x@example.test\",\"role\":\"VENDEDOR\",\"temporaryPassword\":\"" + TEMPORARY + "\",\"admin\":true}"),
                400, "MALFORMED_REQUEST");
    }

    @Test
    @DisplayName("[CT-USR-107] nenhuma resposta e nenhum audit_event contem senha ou hash")
    void passwordsNeverLeak() throws Exception {
        MockHttpSession manager = session("rafael@example.test");
        long bruno = createUser(manager, "bruno@example.test");
        call(HttpMethod.GET, "/users", manager);
        call(HttpMethod.GET, "/users/" + bruno, manager);
        call(HttpMethod.POST, "/users/" + bruno + "/password-reset", manager, "{\"temporaryPassword\":\"Outra-Provisoria-2026\"}");
        MockHttpSession user = session("bruno@example.test", "Outra-Provisoria-2026");
        call(HttpMethod.PUT, "/auth/password", user, "{\"currentPassword\":\"Outra-Provisoria-2026\",\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        call(HttpMethod.PATCH, "/users/" + bruno + "/status", manager, "{\"active\":false,\"reason\":\"Fim do contrato\"}");

        List<String> secrets = List.of(TEMPORARY, "Outra-Provisoria-2026", NEW_PASSWORD, PASSWORD, "$2a$", "$2b$", "$2y$",
                "password_hash", "passwordHash");
        for (String response : responses) assertThat(response).doesNotContain(secrets);
        for (String detail : jdbc.queryForList("SELECT coalesce(detail::text, '') FROM audit_event", String.class)) {
            assertThat(detail).doesNotContain(secrets);
        }
        assertThat(jdbc.queryForList("SELECT action FROM audit_event WHERE entity_id = ? ORDER BY id", String.class, bruno))
                .contains("USER_CREATED", "PASSWORD_RESET", "PASSWORD_CHANGED", "USER_STATUS_CHANGED");
    }
}
