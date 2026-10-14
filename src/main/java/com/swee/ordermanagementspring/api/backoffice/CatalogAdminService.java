package com.swee.ordermanagementspring.api.backoffice;

import com.swee.ordermanagementspring.api.backoffice.BackofficeDtos.*;
import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.DbSessionContext;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.exceptions.ApiException;
import com.swee.ordermanagementspring.exceptions.ErrorCode;
import com.swee.ordermanagementspring.security.AuditService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Produtos, insumos e categorias (RF-22). SKU em maiusculas e unico (C-16); preco so pelo PATCH /price, com motivo
 * gravado pelo gatilho da V5; produto com venda ou estoque movimentado nao e excluido, e sim inativado.
 */
@Service
@PreAuthorize("hasRole('GERENTE')")
public class CatalogAdminService {
    private final BackofficeRepository repository;
    private final JdbcClient jdbc;
    private final DbSessionContext session;
    private final AuditService audit;

    public CatalogAdminService(BackofficeRepository repository, JdbcClient jdbc, DbSessionContext session, AuditService audit) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.session = session;
        this.audit = audit;
    }

    private static ApiException productNotFound(long id) {
        return ApiException.notFound("Product not found, id: " + id);
    }

    private void requireCategory(Long categoryId) {
        if (categoryId != null && !repository.exists("category", categoryId)) {
            throw ApiException.notFound("Category not found, id: " + categoryId);
        }
    }

    private static BigDecimal positive(BigDecimal price) {
        if (price.signum() <= 0) throw ApiException.badRequest("price: must be greater than 0.00");
        return price;
    }

    private static String sku(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }

    // ---------------- produtos ----------------

    @Transactional(readOnly = true)
    public PageResponse<Product> list(String q, Boolean sellable, Boolean active, int page, int size) {
        String term = q == null ? "" : q.trim();
        return new PageResponse<>(repository.products(term, sellable, active, page, size), page, size,
                repository.productCount(term, sellable, active));
    }

    @Transactional(readOnly = true)
    public Product get(long id) {
        return repository.product(id).orElseThrow(() -> productNotFound(id));
    }

    @Transactional
    public Product create(CurrentUser user, ProductCreateRequest request) {
        session.apply(user.id(), null);
        requireCategory(request.categoryId());
        BigDecimal price = positive(request.price().value());
        boolean physical = request instanceof PhysicalProductCreate;
        long id = jdbc.sql("""
                INSERT INTO product(product_type, sku, name, description, category_id, sellable, price, weight, download_link)
                VALUES (:type, :sku, :name, :description, :category, :sellable, :price, :weight, :link) RETURNING id""")
                .param("type", physical ? "PHYSICAL" : "DIGITAL").param("sku", sku(request.sku()))
                .param("name", request.name().trim()).param("description", trimOrNull(request.description()))
                .param("category", request.categoryId()).param("sellable", request.sellable()).param("price", price)
                .param("weight", request instanceof PhysicalProductCreate p ? p.weightKg().doubleValue() : null)
                .param("link", request instanceof DigitalProductCreate d ? d.downloadLink().trim() : null)
                .query(Long.class).single();
        // D-NOITE-12: o preco de cadastro abre o historico (old_price nulo); o gatilho so cobre UPDATE.
        jdbc.sql("""
                INSERT INTO product_price_history(product_id, old_price, new_price, changed_by, reason)
                VALUES (:product, NULL, :price, :user, 'Cadastro')""")
                .param("product", id).param("price", price).param("user", user.id()).update();
        if (request.minLevel() != null) {
            jdbc.sql("UPDATE stock_balance SET min_level = :min WHERE product_id = :id")
                    .param("min", request.minLevel()).param("id", id).update();
        }
        return get(id);
    }

    @Transactional
    public Product update(CurrentUser user, long id, ProductUpdateRequest request) {
        session.apply(user.id(), null);
        jdbc.sql("SELECT id FROM product WHERE id = :id FOR UPDATE").param("id", id).query(Long.class).optional()
                .orElseThrow(() -> productNotFound(id));
        Product current = get(id);
        boolean physical = "PHYSICAL".equals(current.type());
        if (physical && request.downloadLink() != null) throw ApiException.badRequest("downloadLink: only digital products have a link");
        if (!physical && request.weightKg() != null) throw ApiException.badRequest("weightKg: only physical products have a weight");
        requireCategory(request.categoryId());
        jdbc.sql("""
                UPDATE product SET sku = :sku, name = :name, description = coalesce(:description, description),
                       category_id = coalesce(:category, category_id), sellable = :sellable,
                       weight = coalesce(:weight, weight), download_link = coalesce(:link, download_link), updated_at = now()
                 WHERE id = :id""")
                .param("sku", sku(request.sku())).param("name", request.name().trim())
                .param("description", trimOrNull(request.description())).param("category", request.categoryId())
                .param("sellable", request.sellable())
                .param("weight", request.weightKg() == null ? null : request.weightKg().doubleValue())
                .param("link", trimOrNull(request.downloadLink())).param("id", id).update();
        return get(id);
    }

    @Transactional
    public void delete(CurrentUser user, long id) {
        session.apply(user.id(), null);
        if (!repository.exists("product", id)) throw productNotFound(id);
        long history = jdbc.sql("""
                SELECT (SELECT count(*) FROM order_item WHERE product_id = :id) + (SELECT count(*) FROM stock_movement WHERE product_id = :id)
                     + (SELECT count(*) FROM stock_entry WHERE product_id = :id) + (SELECT count(*) FROM promotion WHERE product_id = :id)""")
                .param("id", id).query(Long.class).single();
        if (history > 0) {
            throw ApiException.conflict(ErrorCode.RESOURCE_IN_USE, "Product has sales, stock or promotion history; deactivate it instead");
        }
        jdbc.sql("DELETE FROM product_price_history WHERE product_id = :id").param("id", id).update();
        jdbc.sql("DELETE FROM stock_balance WHERE product_id = :id").param("id", id).update();
        jdbc.sql("DELETE FROM product WHERE id = :id").param("id", id).update();
    }

    @Transactional
    public Product changePrice(CurrentUser user, long id, PriceChangeRequest request) {
        session.apply(user.id(), request.reason().trim());
        BigDecimal price = positive(request.newPrice().value());
        int updated = jdbc.sql("UPDATE product SET price = :price WHERE id = :id").param("price", price).param("id", id).update();
        if (updated == 0) throw productNotFound(id);
        return get(id);
    }

    @Transactional
    public Product changeStatus(CurrentUser user, long id, ProductStatusRequest request) {
        session.apply(user.id(), null);
        int updated = jdbc.sql("UPDATE product SET active = :active, updated_at = now() WHERE id = :id AND active <> :active")
                .param("active", request.active()).param("id", id).update();
        if (updated == 0 && !repository.exists("product", id)) throw productNotFound(id);
        if (updated > 0) {
            audit.record(AuditAction.PRODUCT_STATUS_CHANGED, user.id(), "product", id,
                    "{\"active\":" + request.active() + "}");
        }
        return get(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<PriceChange> priceHistory(long id, int page, int size) {
        if (!repository.exists("product", id)) throw productNotFound(id);
        return new PageResponse<>(repository.priceHistory(id, page, size), page, size, repository.priceHistoryCount(id));
    }

    // ---------------- categorias ----------------

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return repository.categories();
    }

    @Transactional
    public Category createCategory(CategoryRequest request) {
        String name = request.name().trim();
        long id = jdbc.sql("INSERT INTO category(name) VALUES (:name) RETURNING id").param("name", name).query(Long.class).single();
        return new Category(id, name);
    }

    @Transactional
    public Category renameCategory(long id, CategoryRequest request) {
        String name = request.name().trim();
        int updated = jdbc.sql("UPDATE category SET name = :name WHERE id = :id").param("name", name).param("id", id).update();
        if (updated == 0) throw ApiException.notFound("Category not found, id: " + id);
        return new Category(id, name);
    }
}
