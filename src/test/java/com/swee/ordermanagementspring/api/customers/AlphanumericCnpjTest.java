package com.swee.ordermanagementspring.api.customers;

import com.swee.ordermanagementspring.api.customers.CustomerDtos.CorporateCustomerCreate;
import com.swee.ordermanagementspring.entities.customer.Documents;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [CT-CRM-106] CNPJ alfanumerico (Receita, inscricoes a partir de 07/2026; D-EVO-17). Exemplo oficial
 * 12.ABC.345/01DE-35: digitos verificadores pelo modulo 11 com valor ASCII - 48.
 */
class AlphanumericCnpjTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static CorporateCustomerCreate company(String cnpj) {
        return new CorporateCustomerCreate("Empresa Nova", "Empresa Nova LTDA", cnpj, "empresa@cliente.test", "8533334444", null);
    }

    @ParameterizedTest(name = "[CT-CRM-106] aceita {0}")
    @ValueSource(strings = {"12.ABC.345/01DE-35", "12ABC34501DE35", "AB12CD34EF5602", "11.222.333/0001-81", "11222333000181"})
    void acceptsNumericAndAlphanumeric(String cnpj) {
        assertThat(VALIDATOR.validate(company(cnpj))).isEmpty();
    }

    @ParameterizedTest(name = "[CT-CRM-106] rejeita {0}")
    @ValueSource(strings = {"12.ABC.345/01DE-36", "12.abc.345/01de-35", "12ABC34501DE3A", "11.222.333/0001-82", "1234"})
    void rejectsWrongCheckDigitsLowercaseOrShape(String cnpj) {
        assertThat(VALIDATOR.validate(company(cnpj))).isNotEmpty();
    }

    @Test
    @DisplayName("[CT-CRM-106] normaliza para maiusculas sem mascara e mascara as pontas para o VENDEDOR")
    void normalizesAndMasks() {
        assertThat(Documents.cnpjKey("12.abc.345/01de-35")).isEqualTo("12ABC34501DE35");
        assertThat(DocumentFormat.of(null, "12ABC34501DE35", true)).isEqualTo("12.ABC.345/01DE-35");
        assertThat(DocumentFormat.of(null, "12ABC34501DE35", false)).isEqualTo("**.ABC.345/01DE-**");
    }

    @Test
    @DisplayName("[CT-CRM-106] busca: CNPJ alfanumerico completo e documento exato; nome continua prefixo")
    void searchRecognizesAlphanumericCnpj() {
        assertThat(DocumentFormat.searchDocument("12.ABC.345/01DE-35")).isEqualTo("12ABC34501DE35");
        assertThat(DocumentFormat.searchDocument("12abc34501de35")).isEqualTo("12ABC34501DE35");
        assertThat(DocumentFormat.searchDocument("529.982.247-25")).isEqualTo("52998224725");
        assertThat(DocumentFormat.searchDocument("Marina Costa")).isNull();
        assertThat(DocumentFormat.searchDocument("ABCDEFGHIJKL12")).isNull();
    }
}
