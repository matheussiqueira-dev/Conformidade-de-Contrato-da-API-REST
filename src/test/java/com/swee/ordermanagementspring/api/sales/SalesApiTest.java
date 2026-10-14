package com.swee.ordermanagementspring.api.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swee.ordermanagementspring.support.PdvFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * PDV de ponta a ponta (MockMvc + PostgreSQL do Flyway): catalogo, cotacao, venda, confirmacao e cancelamento.
 * Casos CT-SALE-103/104 e CT-STOCK-101..107 do plano de testes.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
class SalesApiTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate template;
    @Autowired JdbcClient jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private PdvFixtures fx;
    private long mouse;
    private long cable;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fx = new PdvFixtures(template, jdbc);
        mouse = fx.product("MOU-001", "Mouse sem fio", "100.00", 10, "60.00");
        cable = fx.product("CAB-001", "Cabo USB", "19.99", 5, "8.00");
    }

    // ---------------- apoio ----------------

    private MvcResult call(MockHttpServletRequestBuilder request, long actor) throws Exception {
        return mvc.perform(request.with(user(fx.principal(actor))).with(csrf())).andReturn();
    }

    private MvcResult postJson(String uri, long actor, String body) throws Exception {
        return call(post(uri).contentType(MediaType.APPLICATION_JSON).content(body), actor);
    }

    private MvcResult sell(long actor, UUID key, String body) throws Exception {
        return call(post("/sales/orders").header("Idempotency-Key", key.toString())
                .contentType(MediaType.APPLICATION_JSON).content(body), actor);
    }

    private String order(String items) {
        return """
                {"customerId":%d,"items":[%s],"payment":{"type":"PIX"},"fulfillment":"PICKUP"}"""
                .formatted(fx.customer, items);
    }

    private static String item(long product, int quantity) {
        return "{\"productId\":%d,\"quantity\":%d}".formatted(product, quantity);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        assertThat(body(result).get("code").asText()).isEqualTo(code);
    }

    private long created(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        long id = body(result).get("id").asLong();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/sales/orders/" + id);
        return id;
    }

    private long movements(long product, String kind) {
        return fx.count("SELECT count(*) FROM stock_movement WHERE product_id = ? AND kind = ?", product, kind);
    }

    // ---------------- catalogo e cotacao ----------------

    @Test
    @DisplayName("[RF-19] catalogo: SKU exato primeiro, sem insumo, inativo ou sem saldo, sem custo")
    void catalogListsOnlySellableAvailableProducts() throws Exception {
        long supply = fx.product("INS-001", "Mouse pad insumo", "5.00", 3, "1.00");
        fx.set("UPDATE product SET sellable = false WHERE id = ?", supply);
        long inactive = fx.product("MOU-002", "Mouse antigo", "50.00", 3, "20.00");
        fx.set("UPDATE product SET active = false WHERE id = ?", inactive);
        long empty = fx.product("MOU-003", "Mouse gamer", "300.00", 0, null);

        JsonNode page = body(call(get("/sales/catalog").param("q", "mou"), fx.allan));
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("id").asLong()).isEqualTo(mouse);
        assertThat(page.get("content").get(0).has("avgCost")).isFalse();
        assertThat(page.get("content").get(0).get("available").asInt()).isEqualTo(10);
        assertThat(page.get("content").get(0).get("price").get("amount").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(page.get("content").get(0).get("activePromotion").isNull()).isTrue();

        JsonNode all = body(call(get("/sales/catalog").param("q", "mou").param("onlyAvailable", "false"), fx.allan));
        assertThat(all.get("totalElements").asLong()).isEqualTo(2);
        assertThat(all.get("content").findValuesAsText("sku")).doesNotContain("INS-001", "MOU-002").contains("MOU-003");

        JsonNode bySku = body(call(get("/sales/catalog").param("q", "cab-001"), fx.allan));
        assertThat(bySku.get("content").get(0).get("id").asLong()).isEqualTo(cable);
        assertThat(empty).isPositive();
    }

    @Test
    @DisplayName("[C-09] catalogo mostra a promocao vigente de maior desconto")
    void catalogShowsBestActivePromotion() throws Exception {
        long promo = promotion("PERCENT", "10", mouse);
        promotion("AMOUNT", "5.00", mouse);

        JsonNode item = body(call(get("/sales/catalog").param("q", "MOU-001"), fx.allan)).get("content").get(0);
        assertThat(item.get("activePromotion").get("id").asLong()).isEqualTo(promo);
        assertThat(item.get("activePromotion").get("kind").asText()).isEqualTo("PERCENT");
    }

    private long promotion(String kind, String value, long product) {
        return jdbc.sql("""
                INSERT INTO promotion(name, kind, value, product_id, starts_at, ends_at, created_by)
                VALUES ('Promo', :kind, :value, :product, now() - interval '1 day', now() + interval '1 day', :manager)
                RETURNING id""").param("kind", kind).param("value", new BigDecimal(value)).param("product", product)
                .param("manager", fx.manager).query(Long.class).single();
    }

    @Test
    @DisplayName("[C-13] cotacao calcula no servidor e nao reserva; falta de estoque responde 200 com available")
    void quoteComputesTotalsWithoutSideEffects() throws Exception {
        long promo = promotion("PERCENT", "10", mouse);
        MvcResult result = postJson("/sales/quote", fx.allan, """
                {"items":[{"productId":%d,"quantity":2,"promotionId":%d},{"productId":%d,"quantity":7}]}"""
                .formatted(mouse, promo, cable));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode quote = body(result);
        assertThat(quote.get("subtotal").get("amount").decimalValue()).isEqualByComparingTo("339.93");
        assertThat(quote.get("discountTotal").get("amount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(quote.get("total").get("amount").decimalValue()).isEqualByComparingTo("319.93");
        assertThat(quote.get("items").get(1).get("available").asInt()).isEqualTo(5);
        assertThat(fx.balance(mouse)).isEqualTo("10/0");
        assertThat(fx.balance(cable)).isEqualTo("5/0");
    }

    @Test
    @DisplayName("[C-13] cotacao: produto inexistente 404, inativo 409, promocao de outro produto 409")
    void quoteRejectsUnknownOrInactiveProducts() throws Exception {
        assertError(postJson("/sales/quote", fx.allan, "{\"items\":[" + item(999_999, 1) + "]}"), 404, "NOT_FOUND");
        fx.set("UPDATE product SET active = false WHERE id = ?", cable);
        assertError(postJson("/sales/quote", fx.allan, "{\"items\":[" + item(cable, 1) + "]}"), 409, "STATE_CONFLICT");
        long promo = promotion("AMOUNT", "1.00", cable);
        assertError(postJson("/sales/quote", fx.allan, """
                {"items":[{"productId":%d,"quantity":1,"promotionId":%d}]}""".formatted(mouse, promo)), 409, "STATE_CONFLICT");
    }

    @ParameterizedTest(name = "[CT-STOCK-101] quantidade {0} responde 400")
    @ValueSource(strings = {"0", "-1", "1000", "1.5", "\"2\"x"})
    void invalidQuantityIsRejected(String quantity) throws Exception {
        MvcResult result = postJson("/sales/quote", fx.allan, "{\"items\":[{\"productId\":%d,\"quantity\":%s}]}".formatted(mouse, quantity));
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        MvcResult sale = sell(fx.allan, UUID.randomUUID(), order("{\"productId\":%d,\"quantity\":%s}".formatted(mouse, quantity)));
        assertThat(sale.getResponse().getStatus()).isEqualTo(400);
        assertThat(fx.balance(mouse)).isEqualTo("10/0");
    }

    @Test
    @DisplayName("[C-14] produto repetido, lista vazia ou mais de 50 itens respondem 400")
    void itemListLimits() throws Exception {
        assertError(postJson("/sales/quote", fx.allan, "{\"items\":[" + item(mouse, 1) + "," + item(mouse, 2) + "]}"),
                400, "VALIDATION_FAILED");
        assertError(postJson("/sales/quote", fx.allan, "{\"items\":[]}"), 400, "VALIDATION_FAILED");
        String many = String.join(",", java.util.stream.LongStream.rangeClosed(1, 51).mapToObj(i -> item(i, 1)).toList());
        assertError(postJson("/sales/quote", fx.allan, "{\"items\":[" + many + "]}"), 400, "VALIDATION_FAILED");
    }

    // ---------------- venda ----------------

    @Test
    @DisplayName("[RF-19] venda: 201, vendedor = sessao, totais do servidor e reserva sem saida")
    void saleReservesStockAndUsesSessionSeller() throws Exception {
        MvcResult result = sell(fx.allan, UUID.randomUUID(), order(item(mouse, 2) + "," + item(cable, 1)));
        long id = created(result);
        JsonNode sale = body(result);

        assertThat(sale.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(sale.get("seller").get("id").asLong()).isEqualTo(fx.allan);
        assertThat(sale.get("customer").get("id").asLong()).isEqualTo(fx.customer);
        assertThat(sale.get("total").get("amount").decimalValue()).isEqualByComparingTo("219.99");
        assertThat(sale.get("payment").get("status").asText()).isEqualTo("PENDING");
        assertThat(sale.get("shippingAddress").isNull()).isTrue();
        assertThat(sale.get("items")).hasSize(2);
        assertThat(fx.balance(mouse)).isEqualTo("10/2");
        assertThat(fx.balance(cable)).isEqualTo("5/1");
        assertThat(movements(mouse, "RESERVA")).isEqualTo(1);
        assertThat(movements(mouse, "SAIDA")).isZero();
        assertThat(fx.count("SELECT amount * 100 FROM payment WHERE order_id = ?", id)).isEqualTo(21999);
    }

    @ParameterizedTest(name = "[invariante] propriedade {0} no corpo responde 400")
    @ValueSource(strings = {"\"sellerId\":2", "\"total\":{\"amount\":1,\"currency\":\"BRL\"}", "\"status\":\"PAID\""})
    void unknownPropertiesAreRejected(String extra) throws Exception {
        String body = order(item(mouse, 1)).replaceFirst("\\{", "{" + extra + ",");
        assertError(sell(fx.allan, UUID.randomUUID(), body), 400, "MALFORMED_REQUEST");
        String priced = order("{\"productId\":%d,\"quantity\":1,\"price\":1.00}".formatted(mouse));
        assertError(sell(fx.allan, UUID.randomUUID(), priced), 400, "MALFORMED_REQUEST");
        assertThat(fx.count("SELECT count(*) FROM orders")).isZero();
    }

    @Test
    @DisplayName("[DEC-ERP-04] cartao grava so o final; numero completo no corpo responde 400")
    void cardStoresOnlyLastFourDigits() throws Exception {
        String card = order(item(cable, 1)).replace("{\"type\":\"PIX\"}",
                "{\"type\":\"CARD\",\"cardLast4\":\"4242\",\"installments\":3,\"authorizationCode\":\"AUT123\"}");
        long id = created(sell(fx.allan, UUID.randomUUID(), card));
        assertThat(jdbc.sql("SELECT coalesce(card_number, '-') || '|' || card_last4 || '|' || installments FROM payment WHERE order_id = :id")
                .param("id", id).query(String.class).single()).isEqualTo("-|4242|3");

        String full = card.replace("\"cardLast4\":\"4242\"", "\"cardLast4\":\"4242\",\"cardNumber\":\"4111111111114242\"");
        assertError(sell(fx.allan, UUID.randomUUID(), full), 400, "MALFORMED_REQUEST");
        String noLast4 = card.replace("\"cardLast4\":\"4242\",", "");
        assertError(sell(fx.allan, UUID.randomUUID(), noLast4), 400, "VALIDATION_FAILED");
    }

    @Test
    @DisplayName("[REC-c] entrega exige endereco; o endereco volta na resposta")
    void deliveryRequiresAddress() throws Exception {
        String delivery = order(item(cable, 1)).replace("\"PICKUP\"", "\"DELIVERY\"");
        assertError(sell(fx.allan, UUID.randomUUID(), delivery), 400, "VALIDATION_FAILED");

        String withAddress = delivery.replace("\"fulfillment\"", """
                "shippingAddress":{"street":"Rua A","number":"10","neighborhood":"Centro","city":"Fortaleza",
                "state":"CE","zipCode":"60000000"},"fulfillment\"""");
        MvcResult result = sell(fx.allan, UUID.randomUUID(), withAddress);
        created(result);
        assertThat(body(result).get("shippingAddress").get("city").asText()).isEqualTo("Fortaleza");
        assertThat(body(result).get("fulfillment").asText()).isEqualTo("DELIVERY");
    }

    @Test
    @DisplayName("[RF-19] venda sem Idempotency-Key ou com cliente inexistente")
    void saleNeedsKeyAndCustomer() throws Exception {
        MvcResult noKey = call(post("/sales/orders").contentType(MediaType.APPLICATION_JSON).content(order(item(mouse, 1))), fx.allan);
        assertError(noKey, 400, "VALIDATION_FAILED");
        MvcResult badKey = call(post("/sales/orders").header("Idempotency-Key", "abc")
                .contentType(MediaType.APPLICATION_JSON).content(order(item(mouse, 1))), fx.allan);
        assertError(badKey, 400, "MALFORMED_REQUEST");
        String unknownCustomer = order(item(mouse, 1)).replace("\"customerId\":" + fx.customer, "\"customerId\":999999");
        assertError(sell(fx.allan, UUID.randomUUID(), unknownCustomer), 404, "NOT_FOUND");
        assertThat(fx.balance(mouse)).isEqualTo("10/0");
    }

    @Test
    @DisplayName("[CT-SALE-103] mesma chave e mesmo corpo devolvem o mesmo pedido, sem segunda reserva")
    void repeatedKeyReturnsSameOrder() throws Exception {
        UUID key = UUID.randomUUID();
        long id = created(sell(fx.allan, key, order(item(mouse, 2))));

        MvcResult again = sell(fx.allan, key, order(item(mouse, 2)));
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(again).get("id").asLong()).isEqualTo(id);
        assertThat(fx.balance(mouse)).isEqualTo("10/2");
        assertThat(movements(mouse, "RESERVA")).isEqualTo(1);

        assertError(sell(fx.allan, key, order(item(mouse, 3))), 409, "IDEMPOTENCY_CONFLICT");

        // A chave e por vendedor: a mesma chave de outro vendedor cria outro pedido e nunca devolve o do Allan.
        long other = created(sell(fx.francisco, key, order(item(mouse, 2))));
        assertThat(other).isNotEqualTo(id);
        assertThat(fx.balance(mouse)).isEqualTo("10/4");
    }

    @Test
    @DisplayName("[CT-STOCK-102] venda acima do disponivel responde 409 e nao altera saldo")
    void saleAboveAvailableIsRejected() throws Exception {
        MvcResult result = sell(fx.allan, UUID.randomUUID(), order(item(cable, 6)));
        assertError(result, 409, "STOCK_INSUFFICIENT");
        assertThat(body(result).get("details").get(0).asText()).contains("available=5");
        assertThat(fx.balance(cable)).isEqualTo("5/0");
        assertThat(fx.count("SELECT count(*) FROM orders")).isZero();
    }

    @Test
    @DisplayName("[CT-STOCK-103] falha no segundo item desfaz a reserva do primeiro")
    void failedSecondItemRollsBackFirstReservation() throws Exception {
        assertError(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 3) + "," + item(cable, 9))), 409, "STOCK_INSUFFICIENT");
        assertThat(fx.balance(mouse)).isEqualTo("10/0");
        assertThat(movements(mouse, "RESERVA")).isZero();
        assertThat(fx.count("SELECT count(*) FROM orders")).isZero();
    }

    @Test
    @DisplayName("[CT-SALE-104] insumo ou inativo nao vendem; preco alterado depois nao muda item antigo")
    void inactiveOrSupplyIsNotSoldAndPriceIsFrozen() throws Exception {
        long supply = fx.product("INS-001", "Etiqueta", "1.00", 50, "0.10");
        fx.set("UPDATE product SET sellable = false WHERE id = ?", supply);
        assertError(sell(fx.allan, UUID.randomUUID(), order(item(supply, 1))), 409, "STATE_CONFLICT");

        long id = created(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 1))));
        fx.set("UPDATE product SET price = 150.00 WHERE id = ?", mouse);
        JsonNode sale = body(call(get("/sales/orders/" + id), fx.allan));
        assertThat(sale.get("items").get(0).get("unitPrice").get("amount").decimalValue()).isEqualByComparingTo("100.00");

        fx.set("UPDATE product SET active = false WHERE id = ?", mouse);
        assertError(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 1))), 409, "STATE_CONFLICT");
    }

    @Test
    @DisplayName("[C-08] promocao explicita entra no item; vencida responde 409")
    void explicitPromotionIsApplied() throws Exception {
        long promo = promotion("AMOUNT", "10.00", mouse);
        MvcResult result = sell(fx.allan, UUID.randomUUID(), order("{\"productId\":%d,\"quantity\":2,\"promotionId\":%d}".formatted(mouse, promo)));
        created(result);
        assertThat(body(result).get("discountTotal").get("amount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(body(result).get("total").get("amount").decimalValue()).isEqualByComparingTo("180.00");

        fx.set("UPDATE promotion SET ends_at = now() - interval '1 minute', starts_at = now() - interval '2 days' WHERE id = ?", promo);
        assertError(sell(fx.allan, UUID.randomUUID(), order("{\"productId\":%d,\"quantity\":1,\"promotionId\":%d}".formatted(mouse, promo))),
                409, "STATE_CONFLICT");
    }

    // ---------------- consulta ----------------

    @Test
    @DisplayName("[C-10] VENDEDOR lista e ve so as proprias vendas; GERENTE ve todas")
    void sellersSeeOnlyTheirOwnOrders() throws Exception {
        long allans = created(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 1))));
        long franciscos = created(sell(fx.francisco, UUID.randomUUID(), order(item(cable, 1))));

        JsonNode mine = body(call(get("/sales/orders"), fx.allan));
        assertThat(mine.get("totalElements").asLong()).isEqualTo(1);
        assertThat(mine.get("content").get(0).get("id").asLong()).isEqualTo(allans);
        assertThat(mine.get("content").get(0).get("itemsCount").asInt()).isEqualTo(1);
        assertThat(mine.get("content").get(0).get("total").get("amount").decimalValue()).isEqualByComparingTo("100.00");
        assertError(call(get("/sales/orders").param("sellerId", Long.toString(fx.francisco)), fx.allan), 403, "FORBIDDEN");
        assertThat(call(get("/sales/orders").param("sellerId", Long.toString(fx.allan)), fx.allan).getResponse().getStatus()).isEqualTo(200);
        assertError(call(get("/sales/orders/" + franciscos), fx.allan), 404, "NOT_FOUND");

        assertThat(body(call(get("/sales/orders"), fx.manager)).get("totalElements").asLong()).isEqualTo(2);
        JsonNode filtered = body(call(get("/sales/orders").param("sellerId", Long.toString(fx.francisco)), fx.manager));
        assertThat(filtered.get("content").get(0).get("id").asLong()).isEqualTo(franciscos);
        assertThat(body(call(get("/sales/orders").param("status", "PAID"), fx.manager)).get("totalElements").asLong()).isZero();
        assertThat(call(get("/sales/orders/" + franciscos), fx.manager).getResponse().getStatus()).isEqualTo(200);
        assertError(call(get("/sales/orders").param("status", "UNKNOWN"), fx.manager), 400, "MALFORMED_REQUEST");
        assertError(call(get("/sales/orders").param("size", "101"), fx.manager), 400, "VALIDATION_FAILED");
    }

    @Test
    @DisplayName("[regressao] GET /orders legado lista venda de retirada (sem endereco) sem erro 500")
    void legacyOrderListAcceptsPickupSales() throws Exception {
        long id = created(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 1))));
        MvcResult legacy = call(get("/orders"), fx.manager);
        assertThat(legacy.getResponse().getStatus()).as(legacy.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(body(legacy).get(0).get("id").asLong()).isEqualTo(id);
        assertThat(call(get("/orders/" + id), fx.manager).getResponse().getStatus()).isEqualTo(200);
    }

    // ---------------- pagamento e cancelamento ----------------

    @Test
    @DisplayName("[CT-STOCK-106] confirmacao baixa estoque uma vez, grava o custo e repetir nao tem efeito")
    void paymentConfirmationIsIdempotent() throws Exception {
        long id = created(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 2))));

        MvcResult paid = postJson("/sales/orders/" + id + "/payment", fx.allan, "{\"authorizationCode\":\"NSU123\"}");
        assertThat(paid.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(paid).get("status").asText()).isEqualTo("PAID");
        assertThat(body(paid).get("payment").get("status").asText()).isEqualTo("APPROVED");
        assertThat(fx.balance(mouse)).isEqualTo("8/0");
        assertThat(fx.count("SELECT unit_cost * 100 FROM order_item WHERE order_id = ?", id)).isEqualTo(6000);

        MvcResult again = call(post("/sales/orders/" + id + "/payment"), fx.allan);
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(fx.balance(mouse)).isEqualTo("8/0");
        assertThat(movements(mouse, "SAIDA")).isEqualTo(1);
    }

    @Test
    @DisplayName("[DEC-ERP-06] pedido de outro vendedor: 404 para o VENDEDOR, liberado para o GERENTE")
    void ownershipOnPaymentAndCancel() throws Exception {
        long id = created(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 1))));
        assertError(call(post("/sales/orders/" + id + "/payment"), fx.francisco), 404, "NOT_FOUND");
        assertError(call(post("/orders/" + id + "/cancel"), fx.francisco), 404, "NOT_FOUND");
        assertThat(fx.balance(mouse)).isEqualTo("10/1");

        assertThat(call(post("/orders/" + id + "/cancel"), fx.manager).getResponse().getStatus()).isEqualTo(200);
        assertError(call(post("/orders/999999/cancel"), fx.manager), 404, "NOT_FOUND");
    }

    @Test
    @DisplayName("[CT-STOCK-107] cancelamento libera a reserva uma vez; pedido pago nao cancela")
    void cancelReleasesReservation() throws Exception {
        long id = created(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 3))));
        MvcResult canceled = call(post("/orders/" + id + "/cancel"), fx.allan);
        assertThat(canceled.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(canceled).get("status").asText()).isEqualTo("CANCELED");
        assertThat(body(canceled).get("payment").get("status").asText()).isEqualTo("DECLINED");
        assertThat(fx.balance(mouse)).isEqualTo("10/0");

        assertThat(call(post("/orders/" + id + "/cancel"), fx.allan).getResponse().getStatus()).isEqualTo(200);
        assertThat(movements(mouse, "LIBERACAO")).isEqualTo(1);
        assertThat(fx.count("SELECT count(*) FROM audit_event WHERE action = 'ORDER_CANCELED' AND entity_id = ?", id)).isEqualTo(1);
        assertError(call(post("/sales/orders/" + id + "/payment"), fx.allan), 409, "STATE_CONFLICT");

        long paid = created(sell(fx.allan, UUID.randomUUID(), order(item(mouse, 1))));
        call(post("/sales/orders/" + paid + "/payment"), fx.allan);
        assertError(call(post("/orders/" + paid + "/cancel"), fx.allan), 409, "STATE_CONFLICT");
        assertThat(fx.balance(mouse)).isEqualTo("9/0");
    }
}
