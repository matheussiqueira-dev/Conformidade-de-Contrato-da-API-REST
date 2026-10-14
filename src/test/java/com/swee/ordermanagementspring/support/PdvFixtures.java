package com.swee.ordermanagementspring.support;

import com.swee.ordermanagementspring.config.BusinessTables;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.security.AppPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

/** Massa sintetica do PDV, criada por SQL sobre o esquema do Flyway (tabelas de negocio esvaziadas antes). */
public final class PdvFixtures {
    private final JdbcClient jdbc;
    public final long manager;
    public final long ana;
    public final long bruno;
    public final long customer;
    public final long category;

    public PdvFixtures(JdbcTemplate template, JdbcClient jdbc) {
        BusinessTables.truncate(template);
        this.jdbc = jdbc;
        manager = user("Rafael Gerente", "gerente@example.test", "GERENTE");
        ana = user("Ana Souza", "ana@example.test", "VENDEDOR");
        bruno = user("Bruno Lima", "bruno@example.test", "VENDEDOR");
        customer = jdbc.sql("INSERT INTO customer(customer_type, name, email, cpf) VALUES ('INDIVIDUAL', 'Marina Costa', "
                + "'marina@cliente.test', '10000000108') RETURNING id").query(Long.class).single();
        category = jdbc.sql("INSERT INTO category(name) VALUES ('Acessorios') RETURNING id").query(Long.class).single();
    }

    private long user(String name, String email, String role) {
        return jdbc.sql("INSERT INTO app_users(name, email, password_hash, role) VALUES (:n, :e, 'sem-login', :r) RETURNING id")
                .param("n", name).param("e", email).param("r", role).query(Long.class).single();
    }

    /** Produto vendavel com entrada de estoque (o gatilho cria saldo e custo medio). */
    public long product(String sku, String name, String price, int stock, String unitCost) {
        long id = jdbc.sql("""
                INSERT INTO product(product_type, name, price, weight, sku, category_id)
                VALUES ('PHYSICAL', :name, :price, 0.1, :sku, :category) RETURNING id""")
                .param("name", name).param("price", new BigDecimal(price)).param("sku", sku).param("category", category)
                .query(Long.class).single();
        if (stock > 0) {
            jdbc.sql("INSERT INTO stock_entry(product_id, quantity, unit_cost, created_by) VALUES (:p, :q, :c, :u)")
                    .param("p", id).param("q", stock).param("c", unitCost == null ? null : new BigDecimal(unitCost))
                    .param("u", manager).update();
        }
        return id;
    }

    public void set(String sql, Object... args) {
        var spec = jdbc.sql(sql);
        for (Object arg : args) spec = spec.param(arg);
        spec.update();
    }

    public int available(long product) {
        return jdbc.sql("SELECT on_hand - reserved FROM stock_balance WHERE product_id = :p").param("p", product)
                .query(Integer.class).single();
    }

    public String balance(long product) {
        return jdbc.sql("SELECT on_hand || '/' || reserved FROM stock_balance WHERE product_id = :p").param("p", product)
                .query(String.class).single();
    }

    public long count(String sql, Object... args) {
        var spec = jdbc.sql(sql);
        for (Object arg : args) spec = spec.param(arg);
        return spec.query(Long.class).single();
    }

    public AppPrincipal principal(long id) {
        String role = jdbc.sql("SELECT role FROM app_users WHERE id = :id").param("id", id).query(String.class).single();
        String name = jdbc.sql("SELECT name FROM app_users WHERE id = :id").param("id", id).query(String.class).single();
        AppUser user = new AppUser(name, "u" + id + "@example.test", "hash", UserRole.valueOf(role));
        ReflectionTestUtils.setField(user, "id", id);
        return new AppPrincipal(user);
    }
}
