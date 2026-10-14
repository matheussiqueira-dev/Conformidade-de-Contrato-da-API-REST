package com.swee.ordermanagementspring.api.discounts;

import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.api.discounts.DiscountDtos.DiscountPolicy;
import com.swee.ordermanagementspring.entities.StoreTime;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Alcadas de desconto manual (V11) e custo medio atual dos produtos, que nunca sai para o VENDEDOR. */
@Repository
public class DiscountPolicyRepository {
    private final JdbcClient jdbc;

    public DiscountPolicyRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Regra aplicada a um papel. */
    public record Limit(BigDecimal maxPercent, boolean allowBelowCost) { }

    public Limit forRole(UserRole role) {
        return jdbc.sql("SELECT max_percent, allow_below_cost FROM discount_policy WHERE role = :role")
                .param("role", role.name())
                .query((rs, n) -> new Limit(rs.getBigDecimal("max_percent"), rs.getBoolean("allow_below_cost")))
                .optional()
                // Papel sem linha na tabela nao concede desconto manual (opcao mais restritiva).
                .orElse(new Limit(BigDecimal.ZERO.setScale(2), false));
    }

    public List<DiscountPolicy> list() {
        return jdbc.sql("""
                SELECT d.role, d.max_percent, d.allow_below_cost, d.updated_by, u.name AS updated_by_name, d.updated_at
                  FROM discount_policy d LEFT JOIN app_users u ON u.id = d.updated_by
                 ORDER BY (SELECT rank FROM role r WHERE r.code = d.role) DESC""")
                .query((rs, n) -> {
                    Long by = rs.getObject("updated_by", Long.class);
                    OffsetDateTime at = rs.getObject("updated_at", OffsetDateTime.class);
                    return new DiscountPolicy(UserRole.valueOf(rs.getString("role")), rs.getBigDecimal("max_percent"),
                            rs.getBoolean("allow_below_cost"), by == null ? null : new UserRef(by, rs.getString("updated_by_name")),
                            at.atZoneSameInstant(StoreTime.ZONE).toOffsetDateTime());
                }).list();
    }

    /** Custo medio atual por produto; produto sem custo conhecido fica fora do mapa (nao barra a venda). */
    public Map<Long, BigDecimal> averageCosts(Collection<Long> productIds) {
        Map<Long, BigDecimal> costs = new HashMap<>();
        if (productIds.isEmpty()) return costs;
        jdbc.sql("SELECT product_id, avg_cost FROM stock_balance WHERE product_id IN (:ids) AND avg_cost IS NOT NULL")
                .param("ids", productIds)
                .query((rs, n) -> costs.put(rs.getLong("product_id"), rs.getBigDecimal("avg_cost"))).list();
        return costs;
    }
}
