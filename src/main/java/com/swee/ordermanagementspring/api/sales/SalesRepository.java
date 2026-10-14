package com.swee.ordermanagementspring.api.sales;

import com.swee.ordermanagementspring.api.common.Addresses;
import com.swee.ordermanagementspring.api.common.Addresses.Address;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs;
import com.swee.ordermanagementspring.api.common.Refs.CategoryRef;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.api.sales.SalesDtos.*;
import com.swee.ordermanagementspring.entities.StoreTime;
import com.swee.ordermanagementspring.entities.enums.Fulfillment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Consultas do PDV. Nenhuma devolve custo, margem ou saldo fisico (CA-19-01). */
@Repository
public class SalesRepository {
    private final JdbcClient jdbc;

    public SalesRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Produto como o PDV o enxerga: preco, situacao e disponivel (on_hand - reserved). */
    public record SaleProduct(long id, String sku, String name, BigDecimal price, boolean active, boolean sellable,
                              Long categoryId, String categoryName, int available) { }

    static OffsetDateTime local(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().atZone(StoreTime.ZONE).toOffsetDateTime();
    }

    static OffsetDateTime instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.atZoneSameInstant(StoreTime.ZONE).toOffsetDateTime();
    }

    public List<Pricing.Promotion> activePromotions() {
        return jdbc.sql("""
                SELECT id, name, kind, value, product_id, category_id, ends_at FROM promotion
                 WHERE active AND now() >= starts_at AND now() < ends_at ORDER BY id""")
                .query((rs, n) -> new Pricing.Promotion(rs.getLong("id"), rs.getString("name"), rs.getString("kind"),
                        rs.getBigDecimal("value"), (Long) rs.getObject("product_id", Long.class),
                        (Long) rs.getObject("category_id", Long.class), instant(rs, "ends_at"))).list();
    }

    public List<SaleProduct> products(Collection<Long> ids) {
        return jdbc.sql("""
                SELECT p.id, p.sku, p.name, p.price, p.active, p.sellable, p.category_id, c.name AS category_name,
                       b.on_hand - b.reserved AS available
                  FROM product p JOIN stock_balance b ON b.product_id = p.id LEFT JOIN category c ON c.id = p.category_id
                 WHERE p.id IN (:ids)""")
                .param("ids", ids).query(SalesRepository::saleProduct).list();
    }

    private static SaleProduct saleProduct(ResultSet rs, int n) throws SQLException {
        return new SaleProduct(rs.getLong("id"), rs.getString("sku"), rs.getString("name"), rs.getBigDecimal("price"),
                rs.getBoolean("active"), rs.getBoolean("sellable"), rs.getObject("category_id", Long.class),
                rs.getString("category_name"), rs.getInt("available"));
    }

    private static final String CATALOG_FILTER = """
              FROM product p JOIN stock_balance b ON b.product_id = p.id LEFT JOIN category c ON c.id = p.category_id
             WHERE p.active AND p.sellable
               AND (:q = '' OR p.sku = upper(:q) OR lower(p.name) LIKE :prefix ESCAPE '\\')
               AND (NOT :onlyAvailable OR b.on_hand - b.reserved > 0)""";

    public List<SaleProduct> catalog(String q, boolean onlyAvailable, int page, int size) {
        return jdbc.sql("""
                SELECT p.id, p.sku, p.name, p.price, p.active, p.sellable, p.category_id, c.name AS category_name,
                       b.on_hand - b.reserved AS available""" + CATALOG_FILTER + """

                 ORDER BY CASE WHEN p.sku = upper(:q) THEN 0 ELSE 1 END, lower(p.name), p.id
                 LIMIT :size OFFSET :offset""")
                .param("q", q).param("prefix", Refs.prefix(q)).param("onlyAvailable", onlyAvailable)
                .param("size", size).param("offset", Refs.offset(page, size))
                .query(SalesRepository::saleProduct).list();
    }

    public long catalogCount(String q, boolean onlyAvailable) {
        return jdbc.sql("SELECT count(*)" + CATALOG_FILTER)
                .param("q", q).param("prefix", Refs.prefix(q)).param("onlyAvailable", onlyAvailable)
                .query(Long.class).single();
    }

    /** Pedido existente para a chave de idempotencia do vendedor (D5). */
    public record IdempotentOrder(long id, String requestHash) { }

    public Optional<IdempotentOrder> byRequestKey(long sellerId, java.util.UUID key) {
        return jdbc.sql("SELECT id, request_hash FROM orders WHERE seller_id = :seller AND client_request_id = :key")
                .param("seller", sellerId).param("key", key)
                .query((rs, n) -> new IdempotentOrder(rs.getLong("id"), rs.getString("request_hash"))).optional();
    }

    /** Dados do pedido para conferencia de dono e estado, sem bloqueio. */
    public record OrderHead(long id, String status, Long sellerId) { }

    public Optional<OrderHead> head(long orderId, boolean forUpdate) {
        return jdbc.sql("SELECT id, status, seller_id FROM orders WHERE id = :id" + (forUpdate ? " FOR UPDATE" : ""))
                .param("id", orderId)
                .query((rs, n) -> new OrderHead(rs.getLong("id"), rs.getString("status"), rs.getObject("seller_id", Long.class)))
                .optional();
    }

    public Optional<SalesOrderResponse> order(long orderId) {
        return jdbc.sql("""
                SELECT o.id, o.status, o.moment, o.fulfillment, o.seller_id, s.name AS seller_name, o.customer_id,
                       c.name AS customer_name, p.id AS payment_id, p.payment_type, p.status AS payment_status,
                       a.id AS address_id, a.street, a.number, a.complement, a.neighborhood, a.city, a.state, a.zip_code,
                       o.manual_discount, o.manual_discount_percent, o.manual_discount_reason, o.discount_approved_by,
                       ap.name AS approver_name
                  FROM orders o
                  LEFT JOIN app_users s ON s.id = o.seller_id
                  LEFT JOIN app_users ap ON ap.id = o.discount_approved_by
                  LEFT JOIN customer c ON c.id = o.customer_id
                  LEFT JOIN payment p ON p.order_id = o.id
                  LEFT JOIN address a ON a.id = o.address_id
                 WHERE o.id = :id""").param("id", orderId).query((rs, n) -> toResponse(rs, items(orderId))).optional();
    }

    private List<SalesOrderItem> items(long orderId) {
        return jdbc.sql("""
                SELECT i.product_id, pr.sku, pr.name, i.quantity, i.price, i.discount
                  FROM order_item i JOIN product pr ON pr.id = i.product_id
                 WHERE i.order_id = :id ORDER BY i.product_id, i.id""")
                .param("id", orderId).query((rs, n) -> {
                    BigDecimal unit = rs.getBigDecimal("price");
                    int quantity = rs.getInt("quantity");
                    BigDecimal discount = rs.getBigDecimal("discount");
                    BigDecimal gross = Pricing.gross(unit, quantity);
                    return new SalesOrderItem(rs.getLong("product_id"), rs.getString("sku"), rs.getString("name"), quantity,
                            MoneyDto.of(unit), MoneyDto.of(discount), MoneyDto.of(gross.subtract(discount)));
                }).list();
    }

    private static SalesOrderResponse toResponse(ResultSet rs, List<SalesOrderItem> items) throws SQLException {
        BigDecimal subtotal = items.stream().map(i -> Pricing.gross(i.unitPrice().amount(), i.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = items.stream().map(i -> i.discount().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Long sellerId = rs.getObject("seller_id", Long.class);
        Long paymentId = rs.getObject("payment_id", Long.class);
        Address address = Addresses.read(rs);
        return new SalesOrderResponse(rs.getLong("id"), rs.getString("status"),
                sellerId == null ? null : new UserRef(sellerId, rs.getString("seller_name")),
                new UserRef(rs.getLong("customer_id"), rs.getString("customer_name")), items,
                MoneyDto.of(subtotal), MoneyDto.of(discount), MoneyDto.of(subtotal.subtract(discount)),
                paymentId == null ? null : new SalesOrderPayment(paymentId, rs.getString("payment_type"), rs.getString("payment_status")),
                Fulfillment.valueOf(rs.getString("fulfillment")), address, local(rs.getTimestamp("moment")), manual(rs));
    }

    private static ManualDiscountInfo manual(ResultSet rs) throws SQLException {
        BigDecimal percent = rs.getBigDecimal("manual_discount_percent");
        if (percent == null) return null;
        Long approver = rs.getObject("discount_approved_by", Long.class);
        return new ManualDiscountInfo(percent, MoneyDto.of(rs.getBigDecimal("manual_discount")), rs.getString("manual_discount_reason"),
                approver == null ? null : new UserRef(approver, rs.getString("approver_name")));
    }

    private static final String LIST_FILTER = """
              FROM orders o
              JOIN customer c ON c.id = o.customer_id
              LEFT JOIN app_users s ON s.id = o.seller_id
              LEFT JOIN payment p ON p.order_id = o.id
             WHERE (CAST(:seller AS bigint) IS NULL OR o.seller_id = :seller)
               AND (CAST(:status AS varchar) IS NULL OR o.status = :status)""";

    public List<SalesOrderSummary> list(Long sellerId, String status, int page, int size) {
        return jdbc.sql("""
                SELECT o.id, o.moment, o.status, o.fulfillment, o.customer_id, c.name AS customer_name, o.seller_id,
                       s.name AS seller_name, p.payment_type, p.status AS payment_status,
                       (SELECT coalesce(sum(i.price * i.quantity - i.discount), 0) FROM order_item i WHERE i.order_id = o.id) AS total,
                       (SELECT count(*) FROM order_item i WHERE i.order_id = o.id) AS items_count""" + LIST_FILTER + """

                 ORDER BY o.moment DESC, o.id DESC LIMIT :size OFFSET :offset""")
                .param("seller", sellerId).param("status", status).param("size", size).param("offset", Refs.offset(page, size))
                .query((rs, n) -> {
                    Long seller = rs.getObject("seller_id", Long.class);
                    return new SalesOrderSummary(rs.getLong("id"), local(rs.getTimestamp("moment")), rs.getString("status"),
                            new UserRef(rs.getLong("customer_id"), rs.getString("customer_name")),
                            seller == null ? null : new UserRef(seller, rs.getString("seller_name")),
                            MoneyDto.of(rs.getBigDecimal("total")), rs.getString("payment_type"), rs.getString("payment_status"),
                            Fulfillment.valueOf(rs.getString("fulfillment")), rs.getInt("items_count"));
                }).list();
    }

    public long count(Long sellerId, String status) {
        return jdbc.sql("SELECT count(*)" + LIST_FILTER).param("seller", sellerId).param("status", status)
                .query(Long.class).single();
    }

    static CategoryRef category(Long id, String name) {
        return id == null ? null : new CategoryRef(id, name);
    }
}
