package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.api.management.ManagementDtos.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Leituras do faturamento sobre as views do V6 (B-09): v_faturamento_diario, v_lancamento_realizado, v_alerta_estoque. */
@Repository
public class ManagementRepository {
    private final JdbcClient jdbc;

    public ManagementRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Linha de v_faturamento_diario; lucroBruto nulo quando algum item vendido no dia nao tem custo (C-20). */
    public record Day(LocalDate date, BigDecimal gross, BigDecimal refunds, BigDecimal net, long ordersPaid, long units,
                      BigDecimal revenueWithCost, BigDecimal grossProfit) { }

    public List<Day> days(DateRange range) {
        return jdbc.sql("""
                SELECT dia, faturamento_bruto, estornos, faturamento_liquido, pedidos_pagos, unidades, receita_com_custo,
                       lucro_bruto
                  FROM v_faturamento_diario WHERE dia BETWEEN :from AND :to ORDER BY dia""")
                .param("from", range.from()).param("to", range.to())
                .query((rs, n) -> new Day(rs.getObject("dia", LocalDate.class), rs.getBigDecimal("faturamento_bruto"),
                        rs.getBigDecimal("estornos"), rs.getBigDecimal("faturamento_liquido"), rs.getLong("pedidos_pagos"),
                        rs.getLong("unidades"), rs.getBigDecimal("receita_com_custo"), rs.getBigDecimal("lucro_bruto")))
                .list();
    }

    public List<PaymentMethodRevenue> byPaymentMethod(DateRange range) {
        return jdbc.sql("""
                SELECT payment_type, count(DISTINCT order_id) FILTER (WHERE tipo = 'VENDA') AS pedidos, sum(valor) AS valor
                  FROM v_lancamento_realizado WHERE data BETWEEN :from AND :to
                 GROUP BY payment_type ORDER BY sum(valor) DESC, payment_type""")
                .param("from", range.from()).param("to", range.to())
                .query((rs, n) -> new PaymentMethodRevenue(rs.getString("payment_type"), rs.getLong("pedidos"),
                        MoneyDto.of(rs.getBigDecimal("valor")))).list();
    }

    public List<SellerRevenue> bySeller(DateRange range) {
        return jdbc.sql("""
                SELECT l.seller_id, u.name, count(DISTINCT l.order_id) FILTER (WHERE l.tipo = 'VENDA') AS pedidos,
                       sum(l.valor) AS valor
                  FROM v_lancamento_realizado l JOIN app_users u ON u.id = l.seller_id
                 WHERE l.data BETWEEN :from AND :to
                 GROUP BY l.seller_id, u.name ORDER BY sum(l.valor) DESC, l.seller_id""")
                .param("from", range.from()).param("to", range.to())
                .query((rs, n) -> new SellerRevenue(new UserRef(rs.getLong("seller_id"), rs.getString("name")),
                        rs.getLong("pedidos"), MoneyDto.of(rs.getBigDecimal("valor")))).list();
    }

    /** Ate 20 produtos por receita liquida dos pagamentos confirmados no periodo; lucro nulo se faltar custo. */
    public List<ProductRevenue> topProducts(DateRange range) {
        return jdbc.sql("""
                SELECT pr.id, pr.sku, pr.name, sum(oi.quantity) AS unidades,
                       sum(oi.price * oi.quantity - oi.discount) AS receita,
                       CASE WHEN count(*) FILTER (WHERE oi.unit_cost IS NULL) = 0
                            THEN sum(oi.price * oi.quantity - oi.discount) - sum(oi.unit_cost * oi.quantity) END AS lucro
                  FROM order_item oi
                  JOIN payment p  ON p.order_id = oi.order_id AND p.status = 'APPROVED'
                  JOIN product pr ON pr.id = oi.product_id
                 WHERE p.payment_date::date BETWEEN :from AND :to
                 GROUP BY pr.id, pr.sku, pr.name
                 ORDER BY receita DESC, pr.id LIMIT 20""")
                .param("from", range.from()).param("to", range.to())
                .query((rs, n) -> new ProductRevenue(rs.getLong("id"), rs.getString("sku"), rs.getString("name"),
                        rs.getLong("unidades"), MoneyDto.of(rs.getBigDecimal("receita")), MoneyDto.of(rs.getBigDecimal("lucro"))))
                .list();
    }

    public long lowStockCount() {
        return jdbc.sql("SELECT count(*) FROM v_alerta_estoque").query(Long.class).single();
    }

    public record Pending(long count, BigDecimal amount) { }

    /** Pedidos aguardando pagamento agora (fotografia operacional, nao recortada pelo periodo: D-EXEC-02). */
    public Pending pending() {
        return jdbc.sql("""
                SELECT count(*) AS pedidos, coalesce(sum(p.amount), 0) AS valor
                  FROM orders o LEFT JOIN payment p ON p.order_id = o.id
                 WHERE o.status = 'PENDING_PAYMENT'""")
                .query((rs, n) -> new Pending(rs.getLong("pedidos"), rs.getBigDecimal("valor"))).single();
    }
}
