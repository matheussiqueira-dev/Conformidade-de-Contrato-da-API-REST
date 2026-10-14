package com.swee.ordermanagementspring.api.backoffice;

import com.swee.ordermanagementspring.api.backoffice.BackofficeDtos.*;
import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.DbSessionContext;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.api.common.Refs.ProductRef;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.exceptions.ApiException;
import com.swee.ordermanagementspring.exceptions.ErrorCode;
import com.swee.ordermanagementspring.security.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Estoque do GERENTE (RF-22): entradas recalculam o custo medio pelo gatilho da V5; ajustes nao mexem no custo;
 * saida nunca deixa o disponivel negativo (UPDATE condicional e CHECK do banco como ultima barreira).
 */
@Service
@PreAuthorize("hasRole('GERENTE')")
public class StockService {
    /** Motivo do historico de preco quando a entrada traz um salePrice novo (REC-a). */
    static final String ENTRY_PRICE_REASON = "Entrada de estoque";

    private final BackofficeRepository repository;
    private final JdbcClient jdbc;
    private final DbSessionContext session;
    private final AuditService audit;

    public StockService(BackofficeRepository repository, JdbcClient jdbc, DbSessionContext session, AuditService audit) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.session = session;
        this.audit = audit;
    }

    private static ApiException notFound(long id) {
        return ApiException.notFound("Product not found, id: " + id);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockProduct> list(String q, Boolean belowMin, Boolean sellable, int page, int size) {
        String term = q == null ? "" : q.trim();
        return new PageResponse<>(repository.stock(term, belowMin, sellable, page, size), page, size,
                repository.stockCount(term, belowMin, sellable));
    }

    @Transactional
    public StockEntryResponse entry(CurrentUser user, long productId, StockEntryRequest request) {
        session.apply(user.id(), ENTRY_PRICE_REASON);
        var product = repository.stock(productId).orElseThrow(() -> notFound(productId));
        BigDecimal unitCost = request.unitCost() == null ? null : request.unitCost().value();
        if (request.salePrice() != null) {
            BigDecimal salePrice = request.salePrice().value();
            if (salePrice.signum() <= 0) throw ApiException.badRequest("salePrice: must be greater than 0.00");
            // O gatilho so grava historico quando o preco muda de fato.
            jdbc.sql("UPDATE product SET price = :price WHERE id = :id").param("price", salePrice).param("id", productId).update();
        }
        record Entry(long id, OffsetDateTime createdAt) { }
        Entry entry = jdbc.sql("""
                INSERT INTO stock_entry(product_id, quantity, unit_cost, created_by, note)
                VALUES (:product, :quantity, :cost, :user, :note) RETURNING id, created_at""")
                .param("product", productId).param("quantity", request.quantity()).param("cost", unitCost)
                .param("user", user.id()).param("note", blankToNull(request.note()))
                .query((rs, n) -> new Entry(rs.getLong("id"), BackofficeRepository.instant(rs, "created_at"))).single();
        return new StockEntryResponse(entry.id(), new ProductRef(productId, product.sku(), product.name()), request.quantity(),
                MoneyDto.of(unitCost), blankToNull(request.note()), new UserRef(user.id(), user.name()), entry.createdAt(),
                repository.stock(productId).orElseThrow());
    }

    @Transactional
    public StockMovement adjust(CurrentUser user, long productId, StockAdjustmentRequest request) {
        session.apply(user.id(), null);
        if (repository.stock(productId).isEmpty()) throw notFound(productId);
        boolean in = request.direction() == Direction.IN;
        if (in && request.reason() != AdjustmentReason.INVENTARIO) {
            throw ApiException.badRequest("reason: an IN adjustment only accepts INVENTARIO");
        }
        int updated = in
                ? jdbc.sql("UPDATE stock_balance SET on_hand = on_hand + :q, updated_at = now() WHERE product_id = :id")
                        .param("q", request.quantity()).param("id", productId).update()
                : jdbc.sql("""
                        UPDATE stock_balance SET on_hand = on_hand - :q, updated_at = now()
                         WHERE product_id = :id AND on_hand - reserved >= :q""")
                        .param("q", request.quantity()).param("id", productId).update();
        if (updated == 0) {
            int available = repository.stock(productId).orElseThrow().available();
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.STOCK_INSUFFICIENT,
                    "Adjustment would leave product " + productId + " below its reserved quantity",
                    List.of("productId=" + productId + "; available=" + Math.max(available, 0)));
        }
        long id = jdbc.sql("""
                INSERT INTO stock_movement(product_id, kind, quantity, reason, note, created_by)
                VALUES (:product, :kind, :q, :reason, :note, :user) RETURNING id""")
                .param("product", productId).param("kind", in ? "AJUSTE_ENTRADA" : "AJUSTE_SAIDA")
                .param("q", request.quantity()).param("reason", request.reason().name())
                .param("note", blankToNull(request.note())).param("user", user.id()).query(Long.class).single();
        audit.record(AuditAction.STOCK_ADJUSTED, user.id(), "stock_movement", id, null);
        return repository.movement(id).orElseThrow();
    }

    @Transactional
    public StockProduct changeMinLevel(CurrentUser user, long productId, MinLevelRequest request) {
        session.apply(user.id(), null);
        int updated = jdbc.sql("UPDATE stock_balance SET min_level = :min, updated_at = now() WHERE product_id = :id")
                .param("min", request.minLevel()).param("id", productId).update();
        if (updated == 0) throw notFound(productId);
        return repository.stock(productId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovement> movements(Long productId, MovementKind kind, LocalDate from, LocalDate to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) throw ApiException.badRequest("from: must not be after to");
        String kindName = kind == null ? null : kind.name();
        return new PageResponse<>(repository.movements(productId, kindName, from, to, page, size), page, size,
                repository.movementCount(productId, kindName, from, to));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
