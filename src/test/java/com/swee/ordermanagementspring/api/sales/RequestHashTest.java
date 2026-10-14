package com.swee.ordermanagementspring.api.sales;

import com.swee.ordermanagementspring.api.sales.SalesDtos.SalesOrderRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** [CT-SALE-102] request_hash nao depende da ordem das chaves do JSON, mas depende da ordem dos itens e dos valores. */
class RequestHashTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final RequestHash hash = new RequestHash(mapper);

    @Test
    @DisplayName("[CT-SALE-102] mesmo corpo com chaves em outra ordem gera o mesmo hash")
    void keyOrderDoesNotMatter() {
        String first = """
                {"customerId":1,"fulfillment":"PICKUP","payment":{"type":"CARD","cardLast4":"1234","installments":2},
                 "items":[{"productId":5,"quantity":2},{"productId":3,"quantity":1}]}""";
        String second = """
                {"items":[{"quantity":2,"productId":5},{"quantity":1,"productId":3}],
                 "payment":{"installments":2,"cardLast4":"1234","type":"CARD"},"fulfillment":"PICKUP","customerId":1}""";

        assertThat(hash.ofJson(first)).isEqualTo(hash.ofJson(second)).matches("^[0-9a-f]{64}$");
        assertThat(hash.of(mapper.readValue(first, SalesOrderRequest.class)))
                .isEqualTo(hash.of(mapper.readValue(second, SalesOrderRequest.class)));
    }

    @Test
    @DisplayName("[CT-SALE-102] quantidade, item ou forma de pagamento diferentes mudam o hash")
    void contentChangesHash() {
        String base = """
                {"customerId":1,"fulfillment":"PICKUP","payment":{"type":"PIX"},"items":[{"productId":5,"quantity":2}]}""";
        SalesOrderRequest request = mapper.readValue(base, SalesOrderRequest.class);

        assertThat(hash.of(request)).isNotEqualTo(hash.of(mapper.readValue(base.replace("\"quantity\":2", "\"quantity\":3"),
                SalesOrderRequest.class)));
        assertThat(hash.of(request)).isNotEqualTo(hash.of(mapper.readValue(base.replace("PIX", "BOLETO\",\"dueDate\":\"2099-01-01"),
                SalesOrderRequest.class)));
        assertThat(hash.ofJson("{\"items\":[1,2]}")).isNotEqualTo(hash.ofJson("{\"items\":[2,1]}"));
    }
}
