package com.swee.ordermanagementspring.dto;

import com.swee.ordermanagementspring.support.TestData;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** DOM-02: limites de dinheiro na entrada da API (Bean Validation, antes de chegar ao servico). */
class MoneyValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest(name = "[DOM-02] preco {0} valido = {1}")
    @CsvSource({"0.01, true", "9999999999.99, true", "10000000000.00, false", "0, false", "-1.00, false"})
    void productPriceLimits(String price, boolean valid) {
        ProductRequestDTO dto = TestData.physicalProduct(1.0, 1.0);
        dto.setPrice(new BigDecimal(price));
        assertThat(validator.validate(dto).isEmpty()).isEqualTo(valid);
    }

    /** [D017] Valor-limite: o menor valor aceito e o que vale ao menos R$ 0,01 depois do HALF_UP. */
    @ParameterizedTest(name = "[D017] {0} -> valido = {1}")
    @CsvSource({"0.00, false", "0.004, false", "0.0049, false", "0.005, true", "0.01, true"})
    @Tag("regression")
    void subCentValuesThatRoundToZeroAreRejected(String value, boolean valid) {
        ProductRequestDTO product = TestData.physicalProduct(1.0, 1.0);
        product.setPrice(new BigDecimal(value));
        PaymentRequestDTO payment = TestData.payment("PIX", 1.0, 1L);
        payment.setAmount(new BigDecimal(value));
        OrderPaymentRequestDTO orderPayment = new OrderPaymentRequestDTO();
        orderPayment.setType("PIX");
        orderPayment.setAmount(new BigDecimal(value));

        assertThat(validator.validate(product).isEmpty()).as("preco do produto").isEqualTo(valid);
        assertThat(validator.validate(payment).isEmpty()).as("valor do pagamento").isEqualTo(valid);
        assertThat(validator.validate(orderPayment).isEmpty()).as("valor do pagamento no pedido").isEqualTo(valid);
    }

    @ParameterizedTest(name = "[DOM-02] pagamento {0} valido = {1}")
    @CsvSource({"100.00, true", "10000000000.00, false", "-0.01, false"})
    void paymentAmountLimits(String amount, boolean valid) {
        PaymentRequestDTO dto = TestData.payment("PIX", 1.0, 1L);
        dto.setAmount(new BigDecimal(amount));
        assertThat(validator.validate(dto).isEmpty()).isEqualTo(valid);
    }
}
