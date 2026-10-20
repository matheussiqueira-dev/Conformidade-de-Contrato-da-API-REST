package com.swee.ordermanagementspring.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** [CT-USR-101] Politica de senha (4.5) por analise de valor limite, sem banco. */
class StrongPasswordValidatorTest {

    @ParameterizedTest(name = "[CT-USR-101] rejeita \"{0}\"")
    @ValueSource(strings = {"", "onze-chars!", "PASSWORD1234", "senha1234567", "Qwertyuiop12", "zzzzzzzzzzzzzzzz"})
    void rejectsShortCommonOrRepeated(String password) {
        assertThat(StrongPasswordValidator.acceptable(password)).isFalse();
    }

    @ParameterizedTest(name = "[CT-USR-101] aceita \"{0}\"")
    @ValueSource(strings = {"doze-chars!!", "cavalo bateria grampo", "Provisoria-Loja-2026"})
    void acceptsLongUncommonPasswords(String password) {
        assertThat(StrongPasswordValidator.acceptable(password)).isTrue();
    }

    @Test
    @DisplayName("[CT-USR-101] limite superior: 72 caracteres e 72 bytes UTF-8 (BCrypt)")
    void upperBoundIsSeventyTwoBytes() {
        assertThat(StrongPasswordValidator.acceptable("a1".repeat(36))).isTrue();
        assertThat(StrongPasswordValidator.acceptable("a1".repeat(36) + "b")).isFalse();
        assertThat(StrongPasswordValidator.acceptable("çã".repeat(18))).isTrue();
        assertThat(StrongPasswordValidator.acceptable("çã".repeat(19))).isFalse();
    }

    @Test
    @DisplayName("[CT-USR-101] senha igual ao e-mail, ignorando caixa e espacos")
    void detectsPasswordEqualToEmail() {
        assertThat(StrongPasswordValidator.sameAsEmail(" AllanBarros@SBI.tech ", "allanbarros@sbi.tech")).isTrue();
        assertThat(StrongPasswordValidator.sameAsEmail("allanbarros@sbi.tech-2026", "allanbarros@sbi.tech")).isFalse();
        assertThat(StrongPasswordValidator.acceptable(null)).isTrue();
    }
}
