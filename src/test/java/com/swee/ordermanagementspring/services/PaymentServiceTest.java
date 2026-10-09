package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.PaymentRequestDTO;
import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.enums.PaymentStatus;
import com.swee.ordermanagementspring.entities.payment.BoletoPayment;
import com.swee.ordermanagementspring.entities.payment.CardPayment;
import com.swee.ordermanagementspring.entities.payment.Payment;
import com.swee.ordermanagementspring.entities.payment.PixPayment;
import com.swee.ordermanagementspring.exceptions.PaymentException;
import com.swee.ordermanagementspring.exceptions.ResourceNotFoundException;
import com.swee.ordermanagementspring.repositories.OrderRepository;
import com.swee.ordermanagementspring.repositories.PaymentRepository;
import com.swee.ordermanagementspring.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unitarios de PaymentService com repositorios simulados (RF-08, RF-09). */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService service;

    private Order pendingOrder() {
        return TestData.order(OrderStatus.PENDING_PAYMENT, 1);
    }

    private void saveReturnsArgument() {
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("[CA-08-01] pagamento pendente aprovado deixa o pedido PAGO")
    void approvedPaymentMarksOrderPaid() {
        saveReturnsArgument();
        Order order = pendingOrder();
        PixPayment payment = new PixPayment(100.0, order, "marina@example.test", "Marina", null, null);

        Payment processed = service.processAndSave(payment);

        assertThat(processed.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @Tag("known-defect")
    @DisplayName("[D003] boleto vencido fica EXPIRED e o pedido continua aguardando pagamento")
    void expiredBoletoDoesNotMarkOrderPaid() {
        saveReturnsArgument();
        Order order = pendingOrder();
        BoletoPayment payment = new BoletoPayment(100.0, order, "34191790010104351004791020150008291070026000",
                LocalDate.now().minusDays(1));

        service.processAndSave(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @ParameterizedTest(name = "[D004] pagamento {0} nao pode ser processado novamente")
    @ValueSource(strings = {"APPROVED", "DECLINED", "EXPIRED"})
    @Tag("known-defect")
    void onlyPendingPaymentsCanBeProcessed(PaymentStatus status) {
        Order order = pendingOrder();
        CardPayment payment = new CardPayment(100.0, order, "4111111111111111", "MARINA", 1);
        payment.setStatus(status);

        assertThatThrownBy(() -> service.processAndSave(payment)).isInstanceOf(PaymentException.class);
        verify(paymentRepository, never()).save(any());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("[CT-PAY-004] criar pagamento para pedido inexistente retorna nao encontrado")
    void insertForMissingOrderFails() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.insert(TestData.payment("PIX", 100.0, 404L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest(name = "[CT-PAY-005] tipo {0} cria a subclasse correta")
    @ValueSource(strings = {"CARD", "PIX", "BOLETO", "pix"})
    void insertCreatesEachPaymentType(String type) {
        saveReturnsArgument();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));

        Payment saved = service.insert(TestData.payment(type, 100.0, 1L));

        Class<?> expected = switch (type.toUpperCase(Locale.ROOT)) {
            case "CARD" -> CardPayment.class;
            case "PIX" -> PixPayment.class;
            default -> BoletoPayment.class;
        };
        assertThat(saved).isInstanceOf(expected);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @Tag("known-defect")
    @DisplayName("[D016] tipo em minusculas funciona tambem com locale padrao turco")
    void typeParsingIsLocaleIndependent() {
        saveReturnsArgument();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertThat(service.insert(TestData.payment("pix", 100.0, 1L))).isInstanceOf(PixPayment.class);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    @DisplayName("[CT-PAY-006] tipo de pagamento desconhecido e rejeitado")
    void unknownTypeIsRejected() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));
        assertThatThrownBy(() -> service.insert(TestData.payment("CHEQUE", 100.0, 1L)))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    @Tag("known-defect")
    @DisplayName("[D006] boleto sem vencimento e rejeitado na criacao")
    void boletoRequiresDueDate() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));
        PaymentRequestDTO dto = TestData.payment("BOLETO", 100.0, 1L);
        dto.setDueDate(null);
        assertThatThrownBy(() -> service.insert(dto)).isInstanceOf(PaymentException.class);
    }

    @Test
    @Tag("known-defect")
    @DisplayName("[D012] valor do pagamento diferente do total do pedido e rejeitado")
    void amountMustMatchOrderTotal() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));
        assertThatThrownBy(() -> service.insert(TestData.payment("PIX", 0.01, 1L)))
                .isInstanceOf(PaymentException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("[CT-PAY-007] somente pagamento PENDING pode ser editado")
    void onlyPendingPaymentCanBeEdited() {
        CardPayment approved = new CardPayment(100.0, pendingOrder(), "4111111111111111", "MARINA", 1);
        approved.setStatus(PaymentStatus.APPROVED);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.update(5L, TestData.payment("CARD", 100.0, 1L)))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    @DisplayName("[CT-PAY-008] edicao do mesmo tipo atualiza os campos")
    void sameTypeUpdateChangesFields() {
        saveReturnsArgument();
        CardPayment existing = new CardPayment(100.0, pendingOrder(), "4000000000000002", "OUTRO", 1);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(existing));
        PaymentRequestDTO dto = TestData.payment("CARD", 100.0, 1L);
        dto.setInstallments(3);

        CardPayment updated = (CardPayment) service.update(5L, dto);

        assertThat(updated.getCardNumber()).isEqualTo("4111111111111111");
        assertThat(updated.getInstallments()).isEqualTo(3);
    }

    @Test
    @DisplayName("[CT-PAY-009] troca de tipo substitui o pagamento e mantem o vinculo com o pedido")
    void typeChangeReplacesPayment() {
        saveReturnsArgument();
        Order order = pendingOrder();
        PixPayment existing = new PixPayment(100.0, order, "marina@example.test", "Marina", null, null);
        order.setPayment(existing);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(existing));

        Payment updated = service.update(5L, TestData.payment("BOLETO", 100.0, 1L));

        verify(paymentRepository).delete(existing);
        assertThat(updated).isInstanceOf(BoletoPayment.class);
        assertThat(order.getPayment()).isSameAs(updated);
        assertThat(updated.getOrder()).isSameAs(order);
    }

    @Test
    @DisplayName("[CT-PAY-010] edicao de cartao sem numero e rejeitada")
    void cardUpdateRequiresNumber() {
        CardPayment existing = new CardPayment(100.0, pendingOrder(), "4111111111111111", "MARINA", 1);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(existing));
        PaymentRequestDTO dto = TestData.payment("CARD", 100.0, 1L);
        dto.setCardNumber(" ");
        assertThatThrownBy(() -> service.update(5L, dto)).isInstanceOf(PaymentException.class);
    }

    @Test
    @Tag("regression")
    @DisplayName("[D011] excluir pagamento inexistente retorna nao encontrado")
    void deletingMissingPaymentFails() {
        lenient().when(paymentRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
