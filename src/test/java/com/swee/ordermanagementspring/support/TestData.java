package com.swee.ordermanagementspring.support;

import com.swee.ordermanagementspring.dto.AddressRequestDTO;
import com.swee.ordermanagementspring.dto.ClientRequestDTO;
import com.swee.ordermanagementspring.dto.PaymentRequestDTO;
import com.swee.ordermanagementspring.dto.ProductRequestDTO;
import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.entities.OrderItem;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.entities.client.IndividualClient;
import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.product.PhysicalProduct;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Massa sintetica compartilhada pelos testes. Nenhum dado pessoal real. */
public final class TestData {
    private TestData() {
    }

    public static AppUser seller(long id) {
        AppUser user = new AppUser("Vendedor sintetico", "seller" + id + "@example.test", "synthetic-hash", UserRole.VENDEDOR);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static IndividualClient client() {
        return new IndividualClient("Marina Costa", "marina@example.test", LocalDate.of(1995, 4, 12), "12345678901");
    }

    /** Pedido em memoria com um item de 100,00 x quantidade. */
    public static Order order(OrderStatus status, int quantity) {
        Order order = new Order(status, client(), seller(7L));
        PhysicalProduct product = new PhysicalProduct(new BigDecimal("100.0"), "Mouse", "Fixture sintetica", 0.2);
        order.setItems(List.of(new OrderItem(order, product, quantity, product.getPrice())));
        return order;
    }

    public static AddressRequestDTO address() {
        AddressRequestDTO address = new AddressRequestDTO();
        address.setStreet("Rua das Flores");
        address.setNumber("10");
        address.setNeighborhood("Boa Viagem");
        address.setCity("Recife");
        address.setState("PE");
        address.setZipCode("50000000");
        return address;
    }

    public static ClientRequestDTO individual(String cpf) {
        ClientRequestDTO client = new ClientRequestDTO();
        client.setType("INDIVIDUAL");
        client.setName("Marina Costa");
        client.setEmail("marina@example.test");
        client.setBirthDate(LocalDate.of(1995, 4, 12));
        client.setCpf(cpf);
        return client;
    }

    public static ClientRequestDTO corporate(String cnpj) {
        ClientRequestDTO client = individual(null);
        client.setType("CORPORATE");
        client.setCnpj(cnpj);
        client.setCompanyName("Loja Sintetica LTDA");
        return client;
    }

    public static ProductRequestDTO physicalProduct(Double price, Double weight) {
        ProductRequestDTO product = new ProductRequestDTO();
        product.setType("PHYSICAL");
        product.setName("Mouse");
        product.setDescription("Fixture sintetica");
        product.setPrice(price == null ? null : BigDecimal.valueOf(price));
        product.setWeight(weight);
        return product;
    }

    public static ProductRequestDTO digitalProduct(String downloadLink) {
        ProductRequestDTO product = new ProductRequestDTO();
        product.setType("DIGITAL");
        product.setName("E-book");
        product.setPrice(new BigDecimal("30.0"));
        product.setDownloadLink(downloadLink);
        return product;
    }

    public static PaymentRequestDTO payment(String type, double amount, Long orderId) {
        PaymentRequestDTO payment = new PaymentRequestDTO();
        payment.setType(type);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setOrderId(orderId);
        payment.setCardNumber("4111111111111111");
        payment.setCardHolder("MARINA COSTA");
        payment.setInstallments(1);
        payment.setPixKey("marina@example.test");
        payment.setPixHolderName("Marina Costa");
        payment.setBarCode("34191790010104351004791020150008291070026000");
        payment.setDueDate(LocalDate.now().plusDays(3));
        return payment;
    }
}
