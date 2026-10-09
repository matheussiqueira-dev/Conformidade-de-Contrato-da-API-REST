package com.swee.ordermanagementspring.entities.payment;

import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.enums.PaymentStatus;
import com.swee.ordermanagementspring.entities.enums.PixKeyType;
import com.swee.ordermanagementspring.exceptions.PaymentException;
import com.swee.ordermanagementspring.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Caixa-branca: cada teste percorre um caminho do grafo de fluxo de processPayment()
 * (ver docs/qualidade/caixa-branca.md). Unitario, sem Spring nem banco.
 */
class PaymentProcessingTest {

    private static PixPayment pix(String key, PixKeyType type) {
        return new PixPayment(100.0, TestData.order(OrderStatus.PENDING_PAYMENT, 1), key, "Marina", null, type);
    }

    private static BoletoPayment boleto(String barCode, LocalDate dueDate) {
        return new BoletoPayment(100.0, TestData.order(OrderStatus.PENDING_PAYMENT, 1), barCode, dueDate);
    }

    @Nested
    @DisplayName("PixPayment.processPayment - 7 caminhos independentes")
    class Pix {
        @ParameterizedTest(name = "[CT-WB-PIX-01] chave invalida ''{0}'' lanca PaymentException")
        @NullAndEmptySource
        void invalidKeyIsRejected(String key) {
            PixPayment payment = pix(key, null);
            assertThatThrownBy(payment::processPayment).isInstanceOf(PaymentException.class);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @ParameterizedTest(name = "[CT-WB-PIX-02] chave {0} e classificada como {1}")
        @CsvSource({
                "marina@example.test, EMAIL",
                "12345678901, CPF",
                "8199998888, PHONE",
                "123e4567-e89b-12d3-a456-426614174000, RANDOM",
                "+5581999998888, RANDOM"
        })
        void keyTypeIsDetected(String key, PixKeyType expected) {
            PixPayment payment = pix(key, null);
            assertThat(payment.processPayment()).isTrue();
            assertThat(payment.getPixKeyType()).isEqualTo(expected);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getTransactionId()).startsWith("PIX-");
            assertThat(payment.getPaymentDate()).isNotNull();
        }

        @Test
        @DisplayName("[CT-WB-PIX-03] tipo ja informado nao e sobrescrito pela deteccao")
        void presetKeyTypeIsKept() {
            PixPayment payment = pix("marina@example.test", PixKeyType.RANDOM);
            payment.processPayment();
            assertThat(payment.getPixKeyType()).isEqualTo(PixKeyType.RANDOM);
        }
    }

    @Nested
    @DisplayName("BoletoPayment.processPayment - vencimento como valor-limite")
    class Boleto {
        @ParameterizedTest(name = "[CT-WB-BOL-01] codigo de barras ''{0}'' e rejeitado")
        @NullAndEmptySource
        void invalidBarCodeIsRejected(String barCode) {
            BoletoPayment payment = boleto(barCode, LocalDate.now());
            assertThatThrownBy(payment::processPayment).isInstanceOf(PaymentException.class);
        }

        @ParameterizedTest(name = "[CT-BB-BOL-02] vencimento hoje{0} dia(s) -> {1}")
        @CsvSource({"-1, EXPIRED, false", "0, APPROVED, true", "1, APPROVED, true"})
        void dueDateBoundary(int offsetDays, PaymentStatus expected, boolean approved) {
            LocalDate today = LocalDate.of(2026, 10, 9);
            BoletoPayment payment = boleto("34191790010104351004791020150008291070026000", today.plusDays(offsetDays));
            assertThat(payment.processPayment(today)).isEqualTo(approved);
            assertThat(payment.getStatus()).isEqualTo(expected);
            assertThat(payment.getPaymentDate() != null).isEqualTo(approved);
        }

        @Test
        @Tag("regression")
        @DisplayName("[D006] boleto sem vencimento gera erro de negocio, nao NullPointerException")
        void missingDueDateIsABusinessError() {
            BoletoPayment payment = boleto("34191790010104351004791020150008291070026000", null);
            assertThatThrownBy(payment::processPayment).isInstanceOf(PaymentException.class);
        }
    }

    @Nested
    @DisplayName("CardPayment.processPayment")
    class Card {
        @ParameterizedTest(name = "[CT-WB-CARD-01] numero ''{0}'' e rejeitado")
        @NullAndEmptySource
        void invalidNumberIsRejected(String number) {
            CardPayment payment = new CardPayment(100.0, TestData.order(OrderStatus.PENDING_PAYMENT, 1), number, "MARINA", 1);
            assertThatThrownBy(payment::processPayment).isInstanceOf(PaymentException.class);
        }

        @Test
        @DisplayName("[CT-WB-CARD-02] cartao valido e aprovado com data de pagamento")
        void validCardIsApproved() {
            CardPayment payment = new CardPayment(100.0, TestData.order(OrderStatus.PENDING_PAYMENT, 1), "4111111111111111", "MARINA", 1);
            assertThat(payment.processPayment()).isTrue();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getPaymentDate()).isNotNull();
        }
    }
}
