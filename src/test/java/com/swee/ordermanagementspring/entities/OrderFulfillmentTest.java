package com.swee.ordermanagementspring.entities;

import com.swee.ordermanagementspring.entities.enums.Fulfillment;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** N-07 e B-10: pedido legado com endereco grava DELIVERY; a hora do pedido segue o fuso da loja. */
class OrderFulfillmentTest {

    @Test
    void shippingAddressDefinesFulfillment() {
        Order order = new Order();
        assertThat(order.getFulfillment()).isEqualTo(Fulfillment.PICKUP);
        order.setShippingAddress(new Address("50000000", "PE", "Recife", "Boa Viagem", null, "2", "Rua das Flores"));
        assertThat(order.getFulfillment()).isEqualTo(Fulfillment.DELIVERY);
        order.setShippingAddress(null);
        assertThat(order.getFulfillment()).isEqualTo(Fulfillment.PICKUP);
    }

    @Test
    void momentUsesStoreTimeZone() {
        LocalDateTime expected = LocalDateTime.now(StoreTime.ZONE);
        assertThat(Duration.between(expected, new Order().getMoment()).abs()).isLessThan(Duration.ofMinutes(1));
        assertThat(StoreTime.clock().getZone()).isEqualTo(StoreTime.ZONE);
    }
}
