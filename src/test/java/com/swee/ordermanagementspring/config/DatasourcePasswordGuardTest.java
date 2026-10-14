package com.swee.ordermanagementspring.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** [D021] Senha do banco ausente falha na inicializacao com mensagem clara, nao como erro de autenticacao. */
class DatasourcePasswordGuardTest {

    @Test
    @Tag("regression")
    @DisplayName("[D021] sem spring.datasource.password a inicializacao para e cita as variaveis")
    void missingPasswordFailsFast() {
        assertThatThrownBy(() -> DatasourcePasswordGuard.check(new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_DATASOURCE_PASSWORD")
                .hasMessageContaining("TEST_DB_PASSWORD");
    }

    @ParameterizedTest(name = "[D021] valor ''{0}'' e tratado como senha ausente")
    @ValueSource(strings = {"${TEST_DB_PASSWORD}", "${SPRING_DATASOURCE_PASSWORD}", "", "   "})
    @Tag("regression")
    void unresolvedOrBlankPasswordFailsFast(String value) {
        MockEnvironment environment = new MockEnvironment().withProperty(DatasourcePasswordGuard.PROPERTY, value);
        assertThatThrownBy(() -> DatasourcePasswordGuard.check(environment)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("[D021] senha resolvida pela variavel de ambiente libera a inicializacao")
    void resolvedPasswordPasses() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("TEST_DB_PASSWORD", "senha com espaco")
                .withProperty(DatasourcePasswordGuard.PROPERTY, "${TEST_DB_PASSWORD}");
        assertThatCode(() -> DatasourcePasswordGuard.check(environment)).doesNotThrowAnyException();
    }
}