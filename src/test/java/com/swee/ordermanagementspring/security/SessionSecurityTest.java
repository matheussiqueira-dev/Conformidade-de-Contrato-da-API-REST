package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.config.SecurityConfig;
import com.swee.ordermanagementspring.controllers.AuthController;
import com.swee.ordermanagementspring.entities.auth.*;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SessionSecurityTest.TestConfig.class)
@WebAppConfiguration
class SessionSecurityTest {
    @Configuration @EnableWebMvc @Import({SecurityConfig.class, AuthController.class,
            com.swee.ordermanagementspring.exceptions.GlobalExceptionHandler.class})
    static class TestConfig {
        @Bean DatabaseUserDetailsService users() {
            var repo = mock(AppUserRepository.class);
            var user = new AppUser("Ana", "seller@example.test", new BCryptPasswordEncoder(4).encode("synthetic-test-password"), UserRole.VENDEDOR);
            ReflectionTestUtils.setField(user, "id", 1L);
            when(repo.findByEmail(anyString())).thenReturn(Optional.empty());
            when(repo.findByEmail("seller@example.test")).thenReturn(Optional.of(user));
            return new DatabaseUserDetailsService(repo);
        }
    }
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    private MvcResult csrf(MockHttpSession session) throws Exception {
        var request = get("/auth/csrf");
        if (session != null) request.session(session);
        return mvc.perform(request).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
    }
    private String token(MvcResult response) {
        return ((org.springframework.security.web.csrf.CsrfToken) response.getRequest().getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName())).getToken();
    }
    private String credentials(String password) { return "{\"email\":\"seller@example.test\",\"password\":\"" + password + "\"}"; }

    @Test void unauthenticatedReadReturnsJson401() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"));
    }
    @Test void loginWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/auth/login").contentType("application/json").content(credentials("synthetic-test-password"))).andExpect(status().isForbidden());
    }
    @Test void bcryptByteLimitIsValidatedBeforeAuthentication() throws Exception {
        var initial = csrf(null);
        mvc.perform(post("/auth/login").session((MockHttpSession) initial.getRequest().getSession())
                .header("X-CSRF-TOKEN", token(initial)).contentType("application/json")
                .content(credentials("é".repeat(40)))).andExpect(status().isBadRequest());
    }
    @Test void wrongPasswordDoesNotEstablishSession() throws Exception {
        var initial = csrf(null);
        var session = (MockHttpSession) initial.getRequest().getSession();
        mvc.perform(post("/auth/login").session(session).header("X-CSRF-TOKEN", token(initial)).contentType("application/json")
                .content(credentials("wrong-password"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void sessionRotatesPersistsAndLogoutInvalidatesIt() throws Exception {
        var initial = csrf(null);
        var session = (MockHttpSession) initial.getRequest().getSession();
        String originalId = session.getId();
        mvc.perform(post("/auth/login").session(session).header("X-CSRF-TOKEN", token(initial)).contentType("application/json")
                .content(credentials("synthetic-test-password"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("VENDEDOR"))
                .andExpect(jsonPath("$.user.password").doesNotExist()).andExpect(jsonPath("$.accessToken").doesNotExist());
        Assertions.assertNotEquals(originalId, session.getId());
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/products").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/orders").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/auth/logout").session(session)).andExpect(status().isForbidden());
        var refreshed = csrf(session);
        mvc.perform(post("/auth/logout").session(session).header("X-CSRF-TOKEN", token(refreshed)))
                .andExpect(status().isNoContent());
        Assertions.assertTrue(session.isInvalid());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }
}
