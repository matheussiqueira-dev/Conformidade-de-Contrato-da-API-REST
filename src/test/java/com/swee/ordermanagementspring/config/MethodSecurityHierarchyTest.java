package com.swee.ordermanagementspring.config;

import com.swee.ordermanagementspring.security.AreaResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** [CT-SEC-103] O GERENTE passa nas regras de metodo do VENDEDOR; o VENDEDOR nao alcanca as do GERENTE. */
@SpringJUnitConfig(MethodSecurityHierarchyTest.Config.class)
class MethodSecurityHierarchyTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean static RoleHierarchy roleHierarchy() { return SecurityConfig.roleHierarchy(); }
        @Bean static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy hierarchy) {
            return SecurityConfig.methodSecurityExpressionHandler(hierarchy);
        }
        @Bean UseCases useCases() { return new UseCases(); }
        @Bean AreaResolver areaResolver(RoleHierarchy hierarchy) { return new AreaResolver(hierarchy); }
    }

    static class UseCases {
        @PreAuthorize("hasRole('VENDEDOR')") public String sell() { return "venda"; }
        @PreAuthorize("hasRole('GERENTE')") public String manage() { return "gestao"; }
    }

    @Autowired UseCases useCases;
    @Autowired AreaResolver areas;

    private static void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("u", "p", "ROLE_" + role));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CT-SEC-103] GERENTE executa caso de uso do VENDEDOR pela hierarquia")
    void managerReachesSellerUseCase() {
        as("GERENTE");
        assertThat(useCases.sell()).isEqualTo("venda");
        assertThat(useCases.manage()).isEqualTo("gestao");
    }

    @Test
    @DisplayName("[RF-17] VENDEDOR nao alcanca caso de uso do GERENTE")
    void sellerDoesNotReachManagerUseCase() {
        as("VENDEDOR");
        assertThat(useCases.sell()).isEqualTo("venda");
        assertThatThrownBy(useCases::manage).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("[CA-17-03] areas seguem a hierarquia")
    void areasFollowHierarchy() {
        assertThat(areas.areas(AuthorityUtils.createAuthorityList("ROLE_VENDEDOR")))
                .containsExactly("VENDER", "VENDAS", "CLIENTES", "MEU_DESEMPENHO");
        assertThat(areas.areas(AuthorityUtils.createAuthorityList("ROLE_GERENTE")))
                .containsExactly("VENDER", "VENDAS", "CLIENTES", "MEU_DESEMPENHO", "GESTAO", "PRODUTOS", "ESTOQUE",
                        "PROMOCOES", "ANALISES");
    }
}
