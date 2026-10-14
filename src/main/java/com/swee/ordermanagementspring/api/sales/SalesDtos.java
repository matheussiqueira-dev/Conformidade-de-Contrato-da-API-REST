package com.swee.ordermanagementspring.api.sales;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.swee.ordermanagementspring.api.common.Addresses.Address;
import com.swee.ordermanagementspring.api.common.Addresses.AddressRequest;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs.CategoryRef;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.entities.enums.Fulfillment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** DTOs do PDV (contrato v4). Nenhum campo de vendedor, preco, desconto ou total entra pelo corpo. */
public final class SalesDtos {
    private SalesDtos() {
    }

    // ---------------- requisicoes ----------------

    public record SaleItemRequest(@NotNull @Positive Long productId,
                                  @NotNull @Min(1) @Max(999) Integer quantity,
                                  @Positive Long promotionId) { }

    private static boolean distinctProducts(List<SaleItemRequest> items) {
        if (items == null) return true;
        var seen = new HashSet<Long>();
        return items.stream().filter(Objects::nonNull).map(SaleItemRequest::productId).filter(Objects::nonNull).allMatch(seen::add);
    }

    public record QuoteRequest(@NotNull @Size(min = 1, max = 50) List<@NotNull @Valid SaleItemRequest> items) {
        @AssertTrue(message = "items: productId must not repeat")
        public boolean isEachProductOnce() {
            return distinctProducts(items);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PixPaymentRequest.class, name = "PIX"),
            @JsonSubTypes.Type(value = CardPaymentRequest.class, name = "CARD"),
            @JsonSubTypes.Type(value = BoletoPaymentRequest.class, name = "BOLETO")})
    public sealed interface SalePaymentRequest permits PixPaymentRequest, CardPaymentRequest, BoletoPaymentRequest {
        String type();
    }

    public record PixPaymentRequest() implements SalePaymentRequest {
        public String type() { return "PIX"; }
    }

    /** DEC-ERP-04: so os 4 ultimos digitos; o numero completo nem tem campo (propriedade extra -> 400). */
    public record CardPaymentRequest(@NotNull @Pattern(regexp = "^[0-9]{4}$") String cardLast4,
                                     @NotNull @Min(1) @Max(12) Integer installments,
                                     @Pattern(regexp = "^[A-Za-z0-9]{1,20}$") String authorizationCode) implements SalePaymentRequest {
        public String type() { return "CARD"; }
    }

    public record BoletoPaymentRequest(@NotNull @FutureOrPresent LocalDate dueDate) implements SalePaymentRequest {
        public String type() { return "BOLETO"; }
    }

    public record SalesOrderRequest(@NotNull @Positive Long customerId,
                                    @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid SaleItemRequest> items,
                                    @NotNull @Valid SalePaymentRequest payment,
                                    @NotNull Fulfillment fulfillment,
                                    @Valid AddressRequest shippingAddress) {
        @AssertTrue(message = "shippingAddress is required for DELIVERY")
        public boolean isAddressPresentForDelivery() {
            return fulfillment != Fulfillment.DELIVERY || shippingAddress != null;
        }

        @AssertTrue(message = "items: productId must not repeat")
        public boolean isEachProductOnce() {
            return distinctProducts(items);
        }
    }

    public record PaymentConfirmationRequest(@Pattern(regexp = "^[A-Za-z0-9]{1,20}$") String authorizationCode) { }

    // ---------------- respostas ----------------

    public record ActivePromotion(long id, String name, String kind, BigDecimal value, Long productId, Long categoryId,
                                  OffsetDateTime endsAt) { }

    public record CatalogItem(long id, String sku, String name, CategoryRef category, MoneyDto price, int available,
                              ActivePromotion activePromotion) { }

    public record QuoteItem(long productId, int quantity, MoneyDto unitPrice, MoneyDto discount, MoneyDto subtotal,
                            int available) { }

    public record QuoteResponse(List<QuoteItem> items, MoneyDto subtotal, MoneyDto discountTotal, MoneyDto total) { }

    public record SalesOrderItem(long productId, String sku, String name, int quantity, MoneyDto unitPrice,
                                 MoneyDto discount, MoneyDto subtotal) { }

    public record SalesOrderPayment(long id, String type, String status) { }

    public record SalesOrderResponse(long id, String status, UserRef seller, UserRef customer, List<SalesOrderItem> items,
                                     MoneyDto subtotal, MoneyDto discountTotal, MoneyDto total, SalesOrderPayment payment,
                                     Fulfillment fulfillment, Address shippingAddress, OffsetDateTime createdAt) { }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SalesOrderSummary(long id, OffsetDateTime createdAt, String status, UserRef customer, UserRef seller,
                                    MoneyDto total, String paymentType, String paymentStatus, Fulfillment fulfillment,
                                    int itemsCount) { }
}
