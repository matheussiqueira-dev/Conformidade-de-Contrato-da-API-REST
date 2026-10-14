package com.swee.ordermanagementspring.api.backoffice;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs.CategoryRef;
import com.swee.ordermanagementspring.api.common.Refs.NamedRef;
import com.swee.ordermanagementspring.api.common.Refs.ProductRef;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** DTOs do backoffice do GERENTE (RF-22): produtos, categorias e estoque. Preco muda so pelo PATCH /price (C-17). */
public final class BackofficeDtos {
    private BackofficeDtos() {
    }

    static final String SKU = "^[A-Za-z0-9][A-Za-z0-9._-]{0,39}$";

    // ---------------- produtos ----------------

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PhysicalProductCreate.class, name = "PHYSICAL"),
            @JsonSubTypes.Type(value = DigitalProductCreate.class, name = "DIGITAL")})
    public sealed interface ProductCreateRequest permits PhysicalProductCreate, DigitalProductCreate {
        String sku();
        String name();
        String description();
        Long categoryId();
        Boolean sellable();
        MoneyDto price();
        Integer minLevel();
    }

    public record PhysicalProductCreate(@NotNull @Pattern(regexp = SKU) String sku,
                                        @NotBlank @Size(min = 2, max = 120) String name,
                                        @Size(max = 255) String description,
                                        @Min(1) Long categoryId,
                                        @NotNull Boolean sellable,
                                        @NotNull @Valid MoneyDto price,
                                        @NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal weightKg,
                                        @Min(0) @Max(100_000) Integer minLevel) implements ProductCreateRequest { }

    public record DigitalProductCreate(@NotNull @Pattern(regexp = SKU) String sku,
                                       @NotBlank @Size(min = 2, max = 120) String name,
                                       @Size(max = 255) String description,
                                       @Min(1) Long categoryId,
                                       @NotNull Boolean sellable,
                                       @NotNull @Valid MoneyDto price,
                                       @NotBlank @URL @Size(max = 255) String downloadLink,
                                       @Min(0) @Max(100_000) Integer minLevel) implements ProductCreateRequest { }

    /** Campos opcionais ausentes mantem o valor atual (mesma regra do cliente, D-NOITE-09). */
    public record ProductUpdateRequest(@NotNull @Pattern(regexp = SKU) String sku,
                                       @NotBlank @Size(min = 2, max = 120) String name,
                                       @Size(max = 255) String description,
                                       @Min(1) Long categoryId,
                                       @NotNull Boolean sellable,
                                       @DecimalMin("0") @DecimalMax("1000") BigDecimal weightKg,
                                       @URL @Size(min = 1, max = 255) String downloadLink) { }

    public record PriceChangeRequest(@NotNull @Valid MoneyDto newPrice, @NotBlank @Size(min = 5, max = 200) String reason) { }

    public record ProductStatusRequest(@NotNull Boolean active) { }

    public record Product(long id, String type, String sku, String name, String description, CategoryRef category,
                          MoneyDto price, boolean sellable, boolean active, BigDecimal weightKg, String downloadLink,
                          OffsetDateTime updatedAt) { }

    public record PriceChange(long id, MoneyDto oldPrice, MoneyDto newPrice, String reason, UserRef changedBy,
                              OffsetDateTime changedAt) { }

    // ---------------- categorias ----------------

    public record CategoryRequest(@NotBlank @Size(min = 2, max = 80) String name) { }

    public record Category(long id, String name) { }

    // ---------------- estoque ----------------

    public record StockProduct(long productId, String sku, String name, boolean sellable, boolean active, int onHand,
                               int reserved, int available, int minLevel, boolean belowMin, MoneyDto avgCost) { }

    /** REC-a: unitCost obrigatorio mas pode ser null (custo desconhecido); salePrice diferente altera o preco. */
    public record StockEntryRequest(@NotNull @Min(1) @Max(100_000) Integer quantity,
                                    @Valid MoneyDto unitCost,
                                    @Valid MoneyDto salePrice,
                                    @Size(max = 200) String note) { }

    public record StockEntryResponse(long id, ProductRef product, int quantity, MoneyDto unitCost, String note,
                                     UserRef createdBy, OffsetDateTime createdAt, StockProduct stock) { }

    public enum Direction { IN, OUT }

    public enum AdjustmentReason { PERDA, AVARIA, USO_INTERNO, INVENTARIO, DEVOLUCAO_FORNECEDOR }

    public enum MovementKind { ENTRADA, RESERVA, LIBERACAO, SAIDA, DEVOLUCAO, AJUSTE_ENTRADA, AJUSTE_SAIDA }

    public record StockAdjustmentRequest(@NotNull Direction direction,
                                         @NotNull @Min(1) @Max(100_000) Integer quantity,
                                         @NotNull AdjustmentReason reason,
                                         @Size(max = 200) String note) { }

    public record MinLevelRequest(@NotNull @Min(0) @Max(100_000) Integer minLevel) { }

    public record StockMovement(long id, ProductRef product, String kind, int quantity, String reason, String note,
                                Long orderId, NamedRef createdBy, OffsetDateTime createdAt) { }
}
