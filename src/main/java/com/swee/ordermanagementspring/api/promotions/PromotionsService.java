package com.swee.ordermanagementspring.api.promotions;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.DbSessionContext;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.api.common.Refs;
import com.swee.ordermanagementspring.api.common.Refs.CategoryRef;
import com.swee.ordermanagementspring.api.common.Refs.ProductRef;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.api.promotions.PromotionsDtos.Promotion;
import com.swee.ordermanagementspring.api.promotions.PromotionsDtos.PromotionRequest;
import com.swee.ordermanagementspring.api.sales.SalesDtos.ActivePromotion;
import com.swee.ordermanagementspring.entities.StoreTime;
import com.swee.ordermanagementspring.exceptions.ApiException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Promocoes (RF-23). So o GERENTE libera e encerra (o gatilho da V5 tambem exige GERENTE como autor); o VENDEDOR
 * consulta as vigentes, que o PDV aplica por promotionId explicito (C-08).
 */
@Service
public class PromotionsService {
    private final JdbcClient jdbc;
    private final DbSessionContext session;

    public PromotionsService(JdbcClient jdbc, DbSessionContext session) {
        this.jdbc = jdbc;
        this.session = session;
    }

    private static OffsetDateTime instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).atZoneSameInstant(StoreTime.ZONE).toOffsetDateTime();
    }

    private static final String COLUMNS = """
            SELECT pr.id, pr.name, pr.kind, pr.value, pr.product_id, p.sku, p.name AS product_name, pr.category_id,
                   c.name AS category_name, pr.starts_at, pr.ends_at, pr.active, pr.created_by, u.name AS created_by_name,
                   pr.created_at
              FROM promotion pr
              LEFT JOIN product p ON p.id = pr.product_id
              LEFT JOIN category c ON c.id = pr.category_id
              JOIN app_users u ON u.id = pr.created_by""";

    private static Promotion promotion(ResultSet rs, int n) throws SQLException {
        Long product = rs.getObject("product_id", Long.class);
        Long category = rs.getObject("category_id", Long.class);
        return new Promotion(rs.getLong("id"), rs.getString("name"), rs.getString("kind"), rs.getBigDecimal("value"),
                product == null ? null : new ProductRef(product, rs.getString("sku"), rs.getString("product_name")),
                category == null ? null : new CategoryRef(category, rs.getString("category_name")),
                instant(rs, "starts_at"), instant(rs, "ends_at"), rs.getBoolean("active"),
                new UserRef(rs.getLong("created_by"), rs.getString("created_by_name")), instant(rs, "created_at"));
    }

    private Promotion find(long id) {
        return jdbc.sql(COLUMNS + " WHERE pr.id = :id").param("id", id).query(PromotionsService::promotion).optional()
                .orElseThrow(() -> ApiException.notFound("Promotion not found, id: " + id));
    }

    /** Vigentes agora; com productId, so as que valem para o produto (dele, da categoria dele ou da loja toda). */
    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public List<ActivePromotion> active(Long productId) {
        return jdbc.sql("""
                SELECT v.promotion_id, v.promocao, v.tipo, v.valor, v.product_id, v.category_id, v.fim
                  FROM v_promocoes_ativas v
                 WHERE CAST(:product AS bigint) IS NULL
                    OR v.product_id = :product
                    OR (v.product_id IS NULL AND v.category_id IS NULL)
                    OR v.category_id = (SELECT category_id FROM product WHERE id = :product)
                 ORDER BY v.promotion_id""")
                .param("product", productId)
                .query((rs, n) -> new ActivePromotion(rs.getLong("promotion_id"), rs.getString("promocao"), rs.getString("tipo"),
                        rs.getBigDecimal("valor"), rs.getObject("product_id", Long.class), rs.getObject("category_id", Long.class),
                        instant(rs, "fim"))).list();
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Transactional(readOnly = true)
    public PageResponse<Promotion> list(Boolean active, int page, int size) {
        List<Promotion> content = jdbc.sql(COLUMNS + """

                 WHERE (CAST(:active AS boolean) IS NULL OR pr.active = :active)
                 ORDER BY pr.created_at DESC, pr.id DESC LIMIT :size OFFSET :offset""")
                .param("active", active).param("size", size).param("offset", Refs.offset(page, size))
                .query(PromotionsService::promotion).list();
        long total = jdbc.sql("SELECT count(*) FROM promotion pr WHERE (CAST(:active AS boolean) IS NULL OR pr.active = :active)")
                .param("active", active).query(Long.class).single();
        return new PageResponse<>(content, page, size, total);
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Transactional
    public Promotion create(CurrentUser user, PromotionRequest request) {
        session.apply(user.id(), null);
        if (request.productId() != null && jdbc.sql("SELECT count(*) FROM product WHERE id = :id")
                .param("id", request.productId()).query(Long.class).single() == 0) {
            throw ApiException.notFound("Product not found, id: " + request.productId());
        }
        if (request.categoryId() != null && jdbc.sql("SELECT count(*) FROM category WHERE id = :id")
                .param("id", request.categoryId()).query(Long.class).single() == 0) {
            throw ApiException.notFound("Category not found, id: " + request.categoryId());
        }
        long id = jdbc.sql("""
                INSERT INTO promotion(name, kind, value, product_id, category_id, starts_at, ends_at, created_by)
                VALUES (:name, :kind, :value, :product, :category, :starts, :ends, :user) RETURNING id""")
                .param("name", request.name().trim()).param("kind", request.kind().name()).param("value", request.value())
                .param("product", request.productId()).param("category", request.categoryId())
                .param("starts", request.startsAt()).param("ends", request.endsAt()).param("user", user.id())
                .query(Long.class).single();
        return find(id);
    }

    /** Encerra desativando; repetir nao tem efeito. */
    @PreAuthorize("hasRole('GERENTE')")
    @Transactional
    public Promotion close(CurrentUser user, long id) {
        session.apply(user.id(), null);
        find(id);
        jdbc.sql("UPDATE promotion SET active = false WHERE id = :id AND active").param("id", id).update();
        return find(id);
    }
}
