package com.swee.ordermanagementspring.entities;

import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.product.DigitalProduct;
import com.swee.ordermanagementspring.entities.product.PhysicalProduct;
import com.swee.ordermanagementspring.exceptions.ProductException;
import com.swee.ordermanagementspring.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unitarios de calculo: frete (valor-limite em 2 kg) e total do pedido. */
class ProductAndOrderCalculationTest {

    @ParameterizedTest(name = "[CT-BB-FRETE-01] peso {0} kg -> frete {1}")
    @CsvSource({"0.0, 20.00", "1.99, 20.00", "2.0, 20.00", "2.01, 20.08", "3.0, 28.00"})
    void shippingValueBoundaryAtTwoKilograms(double weight, BigDecimal expected) {
        PhysicalProduct product = new PhysicalProduct(new BigDecimal("10.0"), "Caixa", "Fixture", weight);
        assertThat(product.calculateShippingValue()).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("[CT-BB-FRETE-02] peso -0,01 kg e invalido")
    void negativeWeightIsRejected() {
        PhysicalProduct product = new PhysicalProduct(new BigDecimal("10.0"), "Caixa", "Fixture", -0.01);
        assertThatThrownBy(product::calculateShippingValue).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("[CT-BB-FRETE-03] produto digital tem frete zero")
    void digitalProductHasNoShipping() {
        assertThat(new DigitalProduct(new BigDecimal("10.0"), "E-book", "Fixture", "https://example.test/e-book").calculateShippingValue())
                .isZero();
    }

    @Test
    @DisplayName("[CT-MONEY-004] pedido sem itens tem total zero")
    void emptyOrderTotalsZero() {
        Order order = new Order(OrderStatus.PENDING_PAYMENT, TestData.client(), TestData.seller(1L));
        assertThat(order.total()).isZero();
    }

    @ParameterizedTest(name = "[CT-MONEY-002] quantidade {0} x 100,00 = {1}")
    @CsvSource({"1, 100.00", "2, 200.00", "10, 1000.00"})
    @Tag("regression")
    void itemSubtotalMultipliesQuantityOnce(int quantity, BigDecimal expected) {
        assertThat(TestData.order(OrderStatus.PENDING_PAYMENT, quantity).total()).isEqualTo(expected);
    }

    @Test
    @DisplayName("[CT-MONEY-001] soma de itens distintos: 4.500 + 1.200 = 5.700")
    void totalSumsAllItems() {
        Order order = new Order(OrderStatus.PENDING_PAYMENT, TestData.client(), TestData.seller(1L));
        PhysicalProduct phone = new PhysicalProduct(new BigDecimal("4500.0"), "iPhone", "Fixture", 0.2);
        PhysicalProduct earbuds = new PhysicalProduct(new BigDecimal("1200.0"), "AirPods", "Fixture", 0.1);
        order.setItems(List.of(new OrderItem(order, phone, 1, phone.getPrice()), new OrderItem(order, earbuds, 1, earbuds.getPrice())));
        assertThat(order.total()).isEqualTo(new BigDecimal("5700.00"));
    }

    @Test
    @DisplayName("[DOM-02] preco 100.00 x 2: subtotal 200.00 e preco historico 100.00")
    void subtotalKeepsHistoricalUnitPrice() {
        Order order = TestData.order(OrderStatus.PENDING_PAYMENT, 2);
        OrderItem item = order.getItems().get(0);
        assertThat(item.subTotal()).isEqualTo(new BigDecimal("200.00"));
        assertThat(item.getPrice()).isEqualTo(new BigDecimal("100.00"));
    }

    @ParameterizedTest(name = "[DOM-02] {0} normalizado com HALF_UP = {1}")
    @CsvSource({"1.005, 1.01", "1.004, 1.00", "100, 100.00", "0.015, 0.02"})
    void moneyUsesTwoDecimalsHalfUp(String value, String expected) {
        assertThat(Money.of(value)).isEqualTo(new BigDecimal(expected));
    }

    @Test
    @DisplayName("[DOM-02] 0.10 + 0.20 = 0.30 sem erro de ponto flutuante")
    void itemsWithCentsAddExactly() {
        Order order = new Order(OrderStatus.PENDING_PAYMENT, TestData.client(), TestData.seller(1L));
        PhysicalProduct a = new PhysicalProduct(new BigDecimal("0.10"), "Bala", "Fixture", 0.0);
        PhysicalProduct b = new PhysicalProduct(new BigDecimal("0.20"), "Chiclete", "Fixture", 0.0);
        order.setItems(List.of(new OrderItem(order, a, 1, a.getPrice()), new OrderItem(order, b, 1, b.getPrice())));
        assertThat(order.total()).isEqualTo(new BigDecimal("0.30"));
    }

    @Test
    @DisplayName("[CT-SEC-003] pedido novo exige vendedor")
    void newOrderRequiresSeller() {
        assertThatThrownBy(() -> new Order(OrderStatus.PENDING_PAYMENT, TestData.client(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
