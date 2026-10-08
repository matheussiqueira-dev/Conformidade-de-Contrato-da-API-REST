package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.OrderRequestDTO;
import com.swee.ordermanagementspring.dto.OrderStatusUpdateDTO;
import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.client.CorporateClient;
import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.payment.BoletoPayment;
import com.swee.ordermanagementspring.entities.payment.CardPayment;
import com.swee.ordermanagementspring.entities.product.PhysicalProduct;
import com.swee.ordermanagementspring.exceptions.OrderException;
import com.swee.ordermanagementspring.exceptions.PaymentException;
import com.swee.ordermanagementspring.exceptions.ResourceNotFoundException;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import com.swee.ordermanagementspring.repositories.ClientRepository;
import com.swee.ordermanagementspring.repositories.OrderRepository;
import com.swee.ordermanagementspring.repositories.ProductRepository;
import com.swee.ordermanagementspring.security.AppPrincipal;
import com.swee.ordermanagementspring.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.swee.ordermanagementspring.services.OrderFixtures.orderRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras de pedido: status, endereco, cliente e pagamento (RF-05 a RF-07, RF-11, RF-12). */
@ExtendWith(MockitoExtension.class)
class OrderServiceRulesTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @InjectMocks
    private OrderService service;

    private AppPrincipal actor() {
        AppUser user = TestData.seller(7L);
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
        return new AppPrincipal(user);
    }

    private void catalogWithMouse() {
        lenient().when(productRepository.findById(1L))
                .thenReturn(Optional.of(new PhysicalProduct(100.0, "Mouse", "Fixture", 0.2)));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static OrderStatusUpdateDTO status(String value) {
        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setStatus(value);
        return dto;
    }

    /** Transicao de estados: so avanca no fluxo; DELIVERED e final. */
    @ParameterizedTest(name = "[CT-ST-{index}] {0} -> {1} = {2}")
    @CsvSource({
            "PENDING_PAYMENT, PROCESSING, OK",
            "PENDING_PAYMENT, paid, OK",
            "PAID, SHIPPED, OK",
            "SHIPPED, DELIVERED, OK",
            "SHIPPED, SHIPPED, OK",
            "PENDING_PAYMENT, CANCELADO, ERRO"
    })
    @Tag("regression")
    void statusTransitionTable(OrderStatus current, String requested, String expected) {
        Order order = TestData.order(current, 1);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(order)).thenReturn(order);

        if ("OK".equals(expected)) {
            assertThat(service.updateStatus(1L, status(requested)).getStatus().name())
                    .isEqualToIgnoringCase(requested);
        } else {
            assertThatThrownBy(() -> service.updateStatus(1L, status(requested))).isInstanceOf(OrderException.class);
            assertThat(order.getStatus()).isEqualTo(current);
        }
    }

    /** [D010] Regressao de status (ex.: entregue -> aguardando pagamento) deve ser bloqueada. */
    @ParameterizedTest(name = "[D010] {0} -> {1} e rejeitado")
    @CsvSource({
            "DELIVERED, PENDING_PAYMENT",
            "SHIPPED, PAID",
            "PAID, PENDING_PAYMENT"
    })
    @Tag("known-defect")
    void statusRegressionIsRejected(OrderStatus current, String requested) {
        Order order = TestData.order(current, 1);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(order)).thenReturn(order);

        assertThatThrownBy(() -> service.updateStatus(1L, status(requested))).isInstanceOf(OrderException.class);
        assertThat(order.getStatus()).isEqualTo(current);
    }

    /** Tabela de decisao RF-12: endereco pode mudar ate o envio. */
    @ParameterizedTest(name = "[CT-ADDR-DT] pedido {0}: alterar endereco = {1}")
    @CsvSource({"PENDING_PAYMENT, OK", "PROCESSING, OK", "PAID, OK", "SHIPPED, ERRO", "DELIVERED, ERRO"})
    void addressChangeDecisionTable(OrderStatus current, String expected) {
        Order order = TestData.order(current, 1);
        order.setShippingAddress(new com.swee.ordermanagementspring.entities.Address(
                "11111111", "SP", "Sao Paulo", "Centro", null, "1", "Rua Antiga"));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(order)).thenReturn(order);

        if ("OK".equals(expected)) {
            assertThat(service.updateAddress(2L, TestData.address()).getShippingAddress().getZipCode()).isEqualTo("50000000");
        } else {
            assertThatThrownBy(() -> service.updateAddress(2L, TestData.address())).isInstanceOf(OrderException.class);
            assertThat(order.getShippingAddress().getZipCode()).isEqualTo("11111111");
        }
    }

    @Test
    @DisplayName("[CA-05-04] pedido com clientId existente reutiliza o cliente")
    void existingClientIsReused() {
        catalogWithMouse();
        CorporateClient existing = new CorporateClient("Loja", "loja@example.test", LocalDate.of(2000, 1, 1), "1", "Loja");
        when(clientRepository.findById(5L)).thenReturn(Optional.of(existing));
        OrderRequestDTO dto = orderRequest(1L, 1);
        dto.setClient(null);
        dto.setClientId(5L);
        dto.getPayment().setAmount(100.0);

        assertThat(service.insert(dto, actor()).getClient()).isSameAs(existing);
    }

    @Test
    @DisplayName("[CT-ORDER-005] clientId inexistente retorna nao encontrado")
    void missingClientId() {
        OrderRequestDTO dto = orderRequest(1L, 1);
        dto.setClient(null);
        dto.setClientId(404L);
        when(clientRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.insert(dto, actor())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("[CA-05-03] pedido sem clientId e sem cliente novo e rejeitado")
    void clientIsRequired() {
        OrderRequestDTO dto = orderRequest(1L, 1);
        dto.setClient(null);
        assertThatThrownBy(() -> service.insert(dto, actor())).isInstanceOf(OrderException.class);
        verify(orderRepository, never()).save(any());
    }

    @ParameterizedTest(name = "[CT-ORDER-006] cliente novo {0} sem documento e rejeitado")
    @CsvSource({"INDIVIDUAL", "CORPORATE", "GOVERNO"})
    void newClientRules(String type) {
        OrderRequestDTO dto = orderRequest(1L, 1);
        dto.setClient("CORPORATE".equals(type) ? TestData.corporate(" ") : TestData.individual(""));
        dto.getClient().setType(type);
        assertThatThrownBy(() -> service.insert(dto, actor())).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("[CT-ORDER-007] produto inexistente retorna nao encontrado")
    void missingProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.insert(orderRequest(1L, 1), actor())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("[CT-ORDER-008] pedido com cartao e boleto cria o pagamento correspondente")
    void cardAndBoletoPayments() {
        catalogWithMouse();
        OrderRequestDTO card = orderRequest(1L, 1);
        card.getPayment().setType("CARD");
        card.getPayment().setAmount(100.0);
        card.getPayment().setCardNumber("4111111111111111");
        card.getPayment().setInstallments(2);
        assertThat(service.insert(card, actor()).getPayment()).isInstanceOf(CardPayment.class);

        OrderRequestDTO boleto = orderRequest(1L, 1);
        boleto.getPayment().setType("BOLETO");
        boleto.getPayment().setAmount(100.0);
        boleto.getPayment().setBarCode("34191790010104351004791020150008291070026000");
        boleto.getPayment().setDueDate(LocalDate.now().plusDays(2));
        assertThat(service.insert(boleto, actor()).getPayment()).isInstanceOf(BoletoPayment.class);
    }

    @ParameterizedTest(name = "[CT-ORDER-009] pagamento {0} sem dado obrigatorio e rejeitado")
    @CsvSource({"CARD", "PIX", "BOLETO", "CHEQUE"})
    void paymentRequiredFields(String type) {
        catalogWithMouse();
        OrderRequestDTO dto = orderRequest(1L, 1);
        dto.getPayment().setType(type);
        dto.getPayment().setAmount(100.0);
        dto.getPayment().setPixKey(null);
        assertThatThrownBy(() -> service.insert(dto, actor())).isInstanceOf(PaymentException.class);
    }

    @ParameterizedTest(name = "[D012] pagamento de {0} para pedido de 100,00 e rejeitado")
    @CsvSource({"99.99", "100.01", "0.01"})
    @Tag("known-defect")
    void paymentAmountMustEqualOrderTotal(double amount) {
        catalogWithMouse();
        OrderRequestDTO dto = orderRequest(1L, 1);
        dto.getPayment().setAmount(amount);
        assertThatThrownBy(() -> service.insert(dto, actor())).isInstanceOf(PaymentException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @Tag("known-defect")
    @DisplayName("[D011] excluir pedido inexistente retorna nao encontrado")
    void deletingMissingOrderFails() {
        lenient().when(orderRepository.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
