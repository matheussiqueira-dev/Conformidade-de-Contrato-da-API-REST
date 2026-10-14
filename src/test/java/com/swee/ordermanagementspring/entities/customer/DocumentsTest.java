package com.swee.ordermanagementspring.entities.customer;

import com.swee.ordermanagementspring.exceptions.CustomerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** N-06: documento gravado so com digitos, com ou sem mascara na entrada. */
class DocumentsTest {

    @Test
    void cpfAndCnpjKeepOnlyDigits() {
        assertThat(Documents.cpf("529.982.247-25")).isEqualTo("52998224725");
        assertThat(Documents.cnpj("12.345.678/0001-95")).isEqualTo("12345678000195");
        assertThat(new IndividualCustomer("PF", null, null, "529.982.247-25").getCpf()).isEqualTo("52998224725");
        assertThat(Documents.cpf(null)).isNull();
        assertThat(Documents.digitsOnly("1")).isEqualTo("1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "5299822472", "529982247250"})
    void cpfWithWrongLengthIsABusinessError(String cpf) {
        assertThatThrownBy(() -> Documents.cpf(cpf)).isInstanceOf(CustomerException.class);
    }

    @Test
    void cnpjWithWrongLengthIsABusinessError() {
        assertThatThrownBy(() -> Documents.cnpj("12.345.678/0001")).isInstanceOf(CustomerException.class);
    }
}
