package com.swee.ordermanagementspring.api.backoffice;

import com.swee.ordermanagementspring.api.backoffice.BackofficeDtos.*;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs;
import com.swee.ordermanagementspring.api.common.Refs.CategoryRef;
import com.swee.ordermanagementspring.api.common.Refs.NamedRef;
import com.swee.ordermanagementspring.api.common.Refs.ProductRef;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.entities.StoreTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Leituras do backoffice. Filtros opcionais usam CAST(:p AS tipo) IS NULL; nada de texto do usuario no SQL. */
@Repository
public class BackofficeRepository {
    private final JdbcClient jdbc;

    public BackofficeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    static OffsetDateTime instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.atZoneSameInstant(StoreTime.ZONE).toOffsetDateTime();
    }

    /** Busca do backoffice: SKU exato ou prefixo do nome, como no catalogo do PDV. */
    private static final String PRODUCT_SEARCH = """
            (:q = '' OR p.sku = upper(:q) OR lower(p.name) LIKE :prefix ESCAPE '\\')""";

    // ---------------- produtos ----------------

    private static final String PRODUCT_COLUMNS = """
            SELECT p.id, p.product_type, p.sku, p.name, p.description, p.category_id, c.name AS category_name, p.price,
                   p.sellable, p.active, p.weight, p.download_link, p.updated_at
              FROM product p LEFT JOIN category c ON c.id = p.category_id""";

    private static Product product(ResultSet rs, int n) throws SQLException {
        Long category = rs.getObject("category_id", Long.class);
        boolean physical = "PHYSICAL".equals(rs.getString("product_type"));
        Double weight = rs.getObject("weight", Double.class);
        return new Product(rs.getLong("id"), rs.getString("product_type"), rs.getString("sku"), rs.getString("name"),
                rs.getString("description"), category == null ? null : new CategoryRef(category, rs.getString("category_name")),
                MoneyDto.of(rs.getBigDecimal("price")), rs.getBoolean("sellable"), rs.getBoolean("active"),
                physical && weight != null ? BigDecimal.valueOf(weight) : null,
                physical ? null : rs.getString("download_link"), instant(rs, "updated_at"));
    }

    public Optional<Product> product(long id) {
        return jdbc.sql(PRODUCT_COLUMNS + " WHERE p.id = :id").param("id", id).query(BackofficeRepository::product).optional();
    }

    private static final String PRODUCT_FILTER = " WHERE " + PRODUCT_SEARCH + """

               AND (CAST(:sellable AS boolean) IS NULL OR p.sellable = :sellable)
               AND (CAST(:active AS boolean) IS NULL OR p.active = :active)""";

    public List<Product> products(String q, Boolean sellable, Boolean active, int page, int size) {
        return jdbc.sql(PRODUCT_COLUMNS + PRODUCT_FILTER + " ORDER BY lower(p.name), p.id LIMIT :size OFFSET :offset")
                .param("q", q).param("prefix", Refs.prefix(q)).param("sellable", sellable).param("active", active)
                .param("size", size).param("offset", Refs.offset(page, size))
                .query(BackofficeRepository::product).list();
    }

    public long productCount(String q, Boolean sellable, Boolean active) {
        return jdbc.sql("SELECT count(*) FROM product p" + PRODUCT_FILTER)
                .param("q", q).param("prefix", Refs.prefix(q)).param("sellable", sellable).param("active", active)
                .query(Long.class).single();
    }

    public List<PriceChange> priceHistory(long productId, int page, int size) {
        return jdbc.sql("""
                SELECT h.id, h.old_price, h.new_price, h.reason, h.changed_by, u.name AS changed_by_name, h.changed_at
                  FROM product_price_history h LEFT JOIN app_users u ON u.id = h.changed_by
                 WHERE h.product_id = :product ORDER BY h.changed_at DESC, h.id DESC LIMIT :size OFFSET :offset""")
                .param("product", productId).param("size", size).param("offset", Refs.offset(page, size))
                .query((rs, n) -> {
                    Long user = rs.getObject("changed_by", Long.class);
                    return new PriceChange(rs.getLong("id"), MoneyDto.of(rs.getBigDecimal("old_price")),
                            MoneyDto.of(rs.getBigDecimal("new_price")), rs.getString("reason"),
                            user == null ? null : new UserRef(user, rs.getString("changed_by_name")), instant(rs, "changed_at"));
                }).list();
    }

    public long priceHistoryCount(long productId) {
        return jdbc.sql("SELECT count(*) FROM product_price_history WHERE product_id = :product").param("product", productId)
                .query(Long.class).single();
    }

    public boolean exists(String table, long id) {
        String sql = switch (table) {
            case "product" -> "SELECT count(*) FROM product WHERE id = :id";
            case "category" -> "SELECT count(*) FROM category WHERE id = :id";
            default -> throw new IllegalArgumentException(table);
        };
        return jdbc.sql(sql).param("id", id).query(Long.class).single() > 0;
    }

    // ---------------- categorias ----------------

    public List<Category> categories() {
        return jdbc.sql("SELECT id, name FROM category ORDER BY lower(name), id")
                .query((rs, n) -> new Category(rs.getLong("id"), rs.getString("name"))).list();
    }

    // ---------------- estoque ----------------

    private static final String STOCK_COLUMNS = """
            SELECT p.id, p.sku, p.name, p.sellable, p.active, b.on_hand, b.reserved, b.min_level, b.avg_cost
              FROM product p JOIN stock_balance b ON b.product_id = p.id""";

    private static StockProduct stock(ResultSet rs, int n) throws SQLException {
        int onHand = rs.getInt("on_hand");
        int reserved = rs.getInt("reserved");
        int minLevel = rs.getInt("min_level");
        int available = onHand - reserved;
        return new StockProduct(rs.getLong("id"), rs.getString("sku"), rs.getString("name"), rs.getBoolean("sellable"),
                rs.getBoolean("active"), onHand, reserved, available, minLevel, available <= minLevel,
                MoneyDto.of(rs.getBigDecimal("avg_cost")));
    }

    public Optional<StockProduct> stock(long productId) {
        return jdbc.sql(STOCK_COLUMNS + " WHERE p.id = :id").param("id", productId).query(BackofficeRepository::stock).optional();
    }

    private static final String STOCK_FILTER = " WHERE " + PRODUCT_SEARCH + """

               AND (CAST(:belowMin AS boolean) IS NULL OR ((b.on_hand - b.reserved) <= b.min_level) = :belowMin)
               AND (CAST(:sellable AS boolean) IS NULL OR p.sellable = :sellable)""";

    public List<StockProduct> stock(String q, Boolean belowMin, Boolean sellable, int page, int size) {
        return jdbc.sql(STOCK_COLUMNS + STOCK_FILTER + " ORDER BY lower(p.name), p.id LIMIT :size OFFSET :offset")
                .param("q", q).param("prefix", Refs.prefix(q)).param("belowMin", belowMin).param("sellable", sellable)
                .param("size", size).param("offset", Refs.offset(page, size))
                .query(BackofficeRepository::stock).list();
    }

    public long stockCount(String q, Boolean belowMin, Boolean sellable) {
        return jdbc.sql("SELECT count(*) FROM product p JOIN stock_balance b ON b.product_id = p.id" + STOCK_FILTER)
                .param("q", q).param("prefix", Refs.prefix(q)).param("belowMin", belowMin).param("sellable", sellable)
                .query(Long.class).single();
    }

    private static final String MOVEMENT_COLUMNS = """
            SELECT m.id, m.product_id, p.sku, p.name, m.kind, m.quantity, m.reason, m.note, m.order_id,
                   u.name AS created_by_name, m.created_at
              FROM stock_movement m JOIN product p ON p.id = m.product_id LEFT JOIN app_users u ON u.id = m.created_by""";

    private static StockMovement movement(ResultSet rs, int n) throws SQLException {
        String creator = rs.getString("created_by_name");
        return new StockMovement(rs.getLong("id"), new ProductRef(rs.getLong("product_id"), rs.getString("sku"), rs.getString("name")),
                rs.getString("kind"), rs.getInt("quantity"), rs.getString("reason"), rs.getString("note"),
                rs.getObject("order_id", Long.class), creator == null ? null : new NamedRef(creator), instant(rs, "created_at"));
    }

    public Optional<StockMovement> movement(long id) {
        return jdbc.sql(MOVEMENT_COLUMNS + " WHERE m.id = :id").param("id", id).query(BackofficeRepository::movement).optional();
    }

    /** C-21: datas fechadas no fuso da loja; "to" inclui o dia inteiro. */
    private static final String MOVEMENT_FILTER = " " + """
             WHERE (CAST(:product AS bigint) IS NULL OR m.product_id = :product)
               AND (CAST(:kind AS varchar) IS NULL OR m.kind = :kind)
               AND (CAST(:from AS date) IS NULL OR m.created_at >= (CAST(:from AS date)::timestamp AT TIME ZONE 'America/Fortaleza'))
               AND (CAST(:to AS date) IS NULL OR m.created_at < ((CAST(:to AS date) + 1)::timestamp AT TIME ZONE 'America/Fortaleza'))""";

    public List<StockMovement> movements(Long productId, String kind, LocalDate from, LocalDate to, int page, int size) {
        return jdbc.sql(MOVEMENT_COLUMNS + MOVEMENT_FILTER + " ORDER BY m.created_at DESC, m.id DESC LIMIT :size OFFSET :offset")
                .param("product", productId).param("kind", kind).param("from", from).param("to", to)
                .param("size", size).param("offset", Refs.offset(page, size))
                .query(BackofficeRepository::movement).list();
    }

    public long movementCount(Long productId, String kind, LocalDate from, LocalDate to) {
        return jdbc.sql("SELECT count(*) FROM stock_movement m" + MOVEMENT_FILTER)
                .param("product", productId).param("kind", kind).param("from", from).param("to", to)
                .query(Long.class).single();
    }
}
