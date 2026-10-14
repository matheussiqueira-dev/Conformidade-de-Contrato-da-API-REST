package com.swee.ordermanagementspring.api.customers;

import com.swee.ordermanagementspring.entities.customer.Documents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** [CT-CRM-101] Mascara e formatacao de CPF/CNPJ; normalizacao nao perde digitos. */
class DocumentFormatTest {

    @Test
    @DisplayName("[CT-CRM-101] CPF: GERENTE ve formatado, VENDEDOR ve ***.456.789-**")
    void cpf() {
        assertThat(DocumentFormat.of("12345678909", null, true)).isEqualTo("123.456.789-09");
        assertThat(DocumentFormat.of("12345678909", null, false)).isEqualTo("***.456.789-**");
    }

    @Test
    @DisplayName("[CT-CRM-101] CNPJ: GERENTE ve formatado, VENDEDOR ve **.345.678/0001-**")
    void cnpj() {
        assertThat(DocumentFormat.of(null, "12345678000195", true)).isEqualTo("12.345.678/0001-95");
        assertThat(DocumentFormat.of(null, "12345678000195", false)).isEqualTo("**.345.678/0001-**");
    }

    @Test
    @DisplayName("[CT-CRM-101] documento fora do tamanho nunca e exibido")
    void invalidStoredDocumentIsNotShown() {
        assertThatThrownBy(() -> DocumentFormat.of("123", null, true)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "[CT-CRM-101] normaliza {0} -> {1}")
    @CsvSource({"123.456.789-09, 12345678909", "12.345.678/0001-95, 12345678000195", "12345678909, 12345678909"})
    void normalizationKeepsAllDigits(String raw, String digits) {
        assertThat(Documents.digitsOnly(raw)).isEqualTo(digits);
    }

    @ParameterizedTest(name = "[C-11] busca \"{0}\" -> documento {1}")
    @CsvSource(value = {"123.456.789-09|12345678909", "12.345.678/0001-95|12345678000195", "123 456|123456",
            "Allan|null", "Allan 123|null", "''|null", "R2D2|null"}, delimiter = '|', nullValues = "null")
    void searchDigits(String q, String expected) {
        assertThat(DocumentFormat.searchDigits(q)).isEqualTo(expected);
    }
}
