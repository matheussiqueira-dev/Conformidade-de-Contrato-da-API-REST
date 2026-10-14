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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Unitarios de calculo: frete (valor-limite em 2 kg) e total do pedido. */
class ProductAndOrderCalculationTest {

    @ParameterizedTest(name = "[CT-BB-FRETE-01] peso {0} kg -> frete {1}")
    @CsvSource({"0.0, 20.0", "1.99, 20.0", "2.0, 20.0", "2.01, 20.08", "3.0, 28.0"})
    void shippingValueBoundaryAtTwoKilograms(double weight, double expected) {
        PhysicalProduct product = new PhysicalProduct(10.0, "Caixa", "Fixture", weight);
        assertThat(product.calculateShippingValue()).isCloseTo(expected, within(0.0001));
    }

    @Test
    @DisplayName("[CT-BB-FRETE-02] peso -0,01 kg e invalido")
    void negativeWeightIsRejected() {
        PhysicalProduct product = new PhysicalProduct(10.0, "Caixa", "Fixture", -0.01);
        assertThatThrownBy(product::calculateShippingValue).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("[CT-BB-FRETE-03] produto digital tem frete zero")
    void digitalProductHasNoShipping() {
        assertThat(new DigitalProduct(10.0, "E-book", "Fixture", "https://example.test/e-book").calculateShippingValue())
                .isZero();
    }

    @Test
    @DisplayName("[CT-MONEY-004] pedido sem itens tem total zero")
    void emptyOrderTotalsZero() {
        Order order = new Order(OrderStatus.PENDING_PAYMENT, TestData.customer(), TestData.seller(1L));
        assertThat(order.total()).isZero();
    }

    @ParameterizedTest(name = "[CT-MONEY-002] quantidade {0} x 100,00 = {1}")
    @CsvSource({"1, 100.0", "2, 200.0", "10, 1000.0"})
    @Tag("regression")
    void itemSubtotalMultipliesQuantityOnce(int quantity, double expected) {
        assertThat(TestData.order(OrderStatus.PENDING_PAYMENT, quantity).total()).isEqualTo(expected);
    }

    @Test
    @DisplayName("[CT-MONEY-001] soma de itens distintos: 4.500 + 1.200 = 5.700")
    void totalSumsAllItems() {
        Order order = new Order(OrderStatus.PENDING_PAYMENT, TestData.customer(), TestData.seller(1L));
        PhysicalProduct phone = new PhysicalProduct(4500.0, "iPhone", "Fixture", 0.2);
        PhysicalProduct earbuds = new PhysicalProduct(1200.0, "AirPods", "Fixture", 0.1);
        order.setItems(List.of(new OrderItem(order, phone, 1, 4500.0), new OrderItem(order, earbuds, 1, 1200.0)));
        assertThat(order.total()).isEqualTo(5700.0);
    }

    @Test
    @DisplayName("[CT-SEC-003] pedido novo exige vendedor")
    void newOrderRequiresSeller() {
        assertThatThrownBy(() -> new Order(OrderStatus.PENDING_PAYMENT, TestData.customer(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
