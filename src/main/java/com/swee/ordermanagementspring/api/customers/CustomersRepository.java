package com.swee.ordermanagementspring.api.customers;

import com.swee.ordermanagementspring.api.common.Addresses;
import com.swee.ordermanagementspring.api.common.Refs;
import com.swee.ordermanagementspring.api.common.Refs.NamedRef;
import com.swee.ordermanagementspring.api.customers.CustomerDtos.CustomerDetail;
import com.swee.ordermanagementspring.api.customers.CustomerDtos.CustomerOrderItem;
import com.swee.ordermanagementspring.api.customers.CustomerDtos.CustomerSummary;
import com.swee.ordermanagementspring.entities.StoreTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

/** Consultas de cliente. Busca por prefixo do nome usa ix_customer_name_prefix; documento exato usa as UNIQUE. */
@Repository
public class CustomersRepository {
    private final JdbcClient jdbc;

    public CustomersRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    static OffsetDateTime local(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().atZone(StoreTime.ZONE).toOffsetDateTime();
    }

    /** Filtro da busca (C-11): todos, documento exato ou prefixo do nome. */
    public record Search(String where, String document, String prefix) {
        static Search of(String q) {
            String term = q == null ? "" : q.trim();
            String digits = DocumentFormat.searchDigits(term);
            if (digits != null) return new Search("(c.cpf = :document OR c.cnpj = :document)", digits, "");
            if (term.isEmpty()) return new Search("true", "", "");
            return new Search("lower(c.name) LIKE :prefix ESCAPE '\\'", "", Refs.prefix(term));
        }
    }

    public List<CustomerSummary> search(Search search, boolean fullDocument, int page, int size) {
        return jdbc.sql("""
                SELECT c.id, c.customer_type, c.name, c.cpf, c.cnpj, c.email, c.phone,
                       (SELECT count(*) FROM orders o WHERE o.customer_id = c.id) AS orders_count,
                       (SELECT max(o.moment) FROM orders o WHERE o.customer_id = c.id) AS last_order_at
                  FROM customer c WHERE\s""" + search.where() + """

                 ORDER BY lower(c.name), c.id LIMIT :size OFFSET :offset""")
                .param("document", search.document()).param("prefix", search.prefix())
                .param("size", size).param("offset", Refs.offset(page, size))
                .query((rs, n) -> new CustomerSummary(rs.getLong("id"), rs.getString("customer_type"), rs.getString("name"),
                        DocumentFormat.of(rs.getString("cpf"), rs.getString("cnpj"), fullDocument), rs.getString("email"),
                        rs.getString("phone"), rs.getLong("orders_count"), local(rs.getTimestamp("last_order_at"))))
                .list();
    }

    public long count(Search search) {
        return jdbc.sql("SELECT count(*) FROM customer c WHERE " + search.where())
                .param("document", search.document()).param("prefix", search.prefix()).query(Long.class).single();
    }

    public Optional<CustomerDetail> detail(long id, boolean fullDocument) {
        return jdbc.sql("""
                SELECT c.id, c.customer_type, c.name, c.company_name, c.cpf, c.cnpj, c.email, c.phone, c.birth_date,
                       c.created_at, c.version, u.name AS created_by_name,
                       a.id AS address_id, a.street, a.number, a.complement, a.neighborhood, a.city, a.state, a.zip_code
                  FROM customer c
                  LEFT JOIN address a ON a.id = c.address_id
                  LEFT JOIN app_users u ON u.id = c.created_by
                 WHERE c.id = :id""").param("id", id)
                .query((rs, n) -> detail(rs, fullDocument)).optional();
    }

    private static CustomerDetail detail(ResultSet rs, boolean fullDocument) throws SQLException {
        String creator = rs.getString("created_by_name");
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
        return new CustomerDetail(rs.getLong("id"), rs.getString("customer_type"), rs.getString("name"),
                rs.getString("company_name"), DocumentFormat.of(rs.getString("cpf"), rs.getString("cnpj"), fullDocument),
                rs.getString("email"), rs.getString("phone"), rs.getObject("birth_date", LocalDate.class), Addresses.read(rs),
                createdAt.atZoneSameInstant(StoreTime.ZONE).toOffsetDateTime(),
                creator == null ? null : new NamedRef(creator), rs.getInt("version"));
    }

    /** Tipo, endereco e versao atuais, com bloqueio da linha para a edicao. */
    public record Head(String type, Long addressId, int version) { }

    public Optional<Head> head(long id) {
        return jdbc.sql("SELECT customer_type, address_id, version FROM customer WHERE id = :id FOR UPDATE").param("id", id)
                .query((rs, n) -> new Head(rs.getString("customer_type"), rs.getObject("address_id", Long.class),
                        rs.getInt("version"))).optional();
    }

    /** Pedido do historico antes de decidir o que o usuario pode ver. */
    public record HistoryRow(long id, Timestamp moment, String status, String fulfillment, Long sellerId, String sellerName,
                             BigDecimal total) { }

    public List<HistoryRow> history(long customerId, int page, int size) {
        return jdbc.sql("""
                SELECT o.id, o.moment, o.status, o.fulfillment, o.seller_id, s.name AS seller_name,
                       (SELECT coalesce(sum(i.price * i.quantity - i.discount), 0) FROM order_item i WHERE i.order_id = o.id) AS total
                  FROM orders o LEFT JOIN app_users s ON s.id = o.seller_id
                 WHERE o.customer_id = :customer
                 ORDER BY o.moment DESC NULLS LAST, o.id DESC LIMIT :size OFFSET :offset""")
                .param("customer", customerId).param("size", size).param("offset", Refs.offset(page, size))
                .query((rs, n) -> new HistoryRow(rs.getLong("id"), rs.getTimestamp("moment"), rs.getString("status"),
                        rs.getString("fulfillment"), rs.getObject("seller_id", Long.class), rs.getString("seller_name"),
                        rs.getBigDecimal("total"))).list();
    }

    public long historyCount(long customerId) {
        return jdbc.sql("SELECT count(*) FROM orders WHERE customer_id = :customer").param("customer", customerId)
                .query(Long.class).single();
    }

    public Map<Long, List<CustomerOrderItem>> items(Collection<Long> orderIds) {
        Map<Long, List<CustomerOrderItem>> items = new LinkedHashMap<>();
        if (orderIds.isEmpty()) return items;
        jdbc.sql("""
                SELECT i.order_id, p.name, i.quantity FROM order_item i JOIN product p ON p.id = i.product_id
                 WHERE i.order_id IN (:ids) ORDER BY i.order_id, i.product_id, i.id""")
                .param("ids", orderIds)
                .query(rs -> {
                    items.computeIfAbsent(rs.getLong("order_id"), k -> new ArrayList<>())
                            .add(new CustomerOrderItem(rs.getString("name"), rs.getInt("quantity")));
                });
        return items;
    }

    public boolean exists(long id) {
        return jdbc.sql("SELECT count(*) FROM customer WHERE id = :id").param("id", id).query(Long.class).single() > 0;
    }
}
