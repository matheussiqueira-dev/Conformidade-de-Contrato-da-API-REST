package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.DbSessionContext;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.api.management.ManagementDtos.*;
import com.swee.ordermanagementspring.exceptions.ApiException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;

/**
 * Metas mensais e desempenho (RF-10 a RF-12) sobre as views do V6. Realizado = pagamentos confirmados menos estornos;
 * meta zero = "Sem meta" sem percentual (DOM-08); o VENDEDOR so ve o proprio desempenho (CA-11-02, DOM-03).
 */
@Service
public class GoalsService {
    private final JdbcClient jdbc;
    private final DbSessionContext session;

    public GoalsService(JdbcClient jdbc, DbSessionContext session) {
        this.jdbc = jdbc;
        this.session = session;
    }

    static LocalDate month(String period) {
        return YearMonth.parse(period).atDay(1);
    }

    static GoalState state(BigDecimal goal, BigDecimal realized) {
        if (goal.signum() == 0) return GoalState.SEM_META;
        return realized.compareTo(goal) >= 0 ? GoalState.META_ATINGIDA : GoalState.EM_ANDAMENTO;
    }

    static BigDecimal achievement(BigDecimal goal, BigDecimal realized) {
        return goal.signum() == 0 ? null : realized.multiply(BigDecimal.valueOf(100)).divide(goal, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal realized(LocalDate month, Long sellerId) {
        return jdbc.sql("""
                SELECT coalesce(sum(valor), 0) FROM v_lancamento_realizado
                 WHERE data >= :month AND data < CAST(:month AS date) + interval '1 month'
                   AND (CAST(:seller AS bigint) IS NULL OR seller_id = :seller)""")
                .param("month", month).param("seller", sellerId).query(BigDecimal.class).single();
    }

    // ---------------- metas ----------------

    @PreAuthorize("hasRole('GERENTE')")
    @Transactional(readOnly = true)
    public MonthlyGoals get(String period) {
        LocalDate month = month(period);
        record Goal(long id, BigDecimal amount) { }
        Goal goal = jdbc.sql("SELECT id, amount FROM monthly_goal WHERE period = :month").param("month", month)
                .query((rs, n) -> new Goal(rs.getLong("id"), rs.getBigDecimal("amount"))).optional()
                .orElseThrow(() -> ApiException.notFound("No goals for period " + period));
        List<SellerGoal> sellers = jdbc.sql("""
                SELECT u.id, u.name, sg.amount FROM seller_goal sg JOIN app_users u ON u.id = sg.seller_id
                 WHERE sg.monthly_goal_id = :goal ORDER BY lower(u.name), u.id""")
                .param("goal", goal.id())
                .query((rs, n) -> new SellerGoal(new UserRef(rs.getLong("id"), rs.getString("name")),
                        MoneyDto.of(rs.getBigDecimal("amount")))).list();
        return new MonthlyGoals(period, MoneyDto.of(goal.amount()), sellers);
    }

    /** O servico valida a distribuicao antes do banco (400); o gatilho adiado da V5 fica como ultima barreira. */
    @PreAuthorize("hasRole('GERENTE')")
    @Transactional
    public MonthlyGoals save(CurrentUser user, MonthlyGoalsRequest request) {
        session.apply(user.id(), null);
        BigDecimal store = request.storeGoal().value();
        var seen = new HashSet<Long>();
        BigDecimal distributed = BigDecimal.ZERO;
        for (SellerGoalRequest seller : request.sellers()) {
            if (!seen.add(seller.sellerId())) throw ApiException.badRequest("sellers: sellerId " + seller.sellerId() + " repeated");
            long sellers = jdbc.sql("SELECT count(*) FROM app_users WHERE id = :id AND role = 'VENDEDOR' AND active")
                    .param("id", seller.sellerId()).query(Long.class).single();
            if (sellers == 0) throw ApiException.badRequest("sellers: user " + seller.sellerId() + " is not an active seller");
            distributed = distributed.add(seller.goal().value());
        }
        if (!request.sellers().isEmpty() && distributed.compareTo(store) != 0) {
            throw ApiException.badRequest("sellers: distribution " + distributed + " must equal the store goal " + store);
        }
        long goalId = jdbc.sql("""
                INSERT INTO monthly_goal(period, amount, created_by) VALUES (:month, :amount, :user)
                ON CONFLICT (period) DO UPDATE SET amount = EXCLUDED.amount RETURNING id""")
                .param("month", month(request.period())).param("amount", store).param("user", user.id())
                .query(Long.class).single();
        jdbc.sql("DELETE FROM seller_goal WHERE monthly_goal_id = :goal").param("goal", goalId).update();
        for (SellerGoalRequest seller : request.sellers()) {
            jdbc.sql("INSERT INTO seller_goal(monthly_goal_id, seller_id, amount) VALUES (:goal, :seller, :amount)")
                    .param("goal", goalId).param("seller", seller.sellerId()).param("amount", seller.goal().value()).update();
        }
        return get(request.period());
    }

    // ---------------- desempenho ----------------

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public SellerPerformance mine(CurrentUser user, String period) {
        LocalDate month = month(period);
        // v_meu_desempenho filtra pela identidade da transacao (app.user_id).
        session.apply(user.id(), null);
        BigDecimal goal = jdbc.sql("SELECT meta FROM v_meu_desempenho WHERE periodo = :month").param("month", month)
                .query(BigDecimal.class).optional().orElse(BigDecimal.ZERO);
        BigDecimal realized = realized(month, user.id());
        return new SellerPerformance(period, new UserRef(user.id(), user.name()), MoneyDto.of(goal), MoneyDto.of(realized),
                MoneyDto.of(goal.subtract(realized).max(BigDecimal.ZERO)), achievement(goal, realized), state(goal, realized));
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Transactional(readOnly = true)
    public ManagementSummary summary(String period) {
        LocalDate month = month(period);
        BigDecimal goal = jdbc.sql("SELECT amount FROM monthly_goal WHERE period = :month").param("month", month)
                .query(BigDecimal.class).optional().orElse(BigDecimal.ZERO);
        BigDecimal realized = realized(month, null);
        List<SellerMetric> team = jdbc.sql("""
                SELECT seller_id, vendedor, meta, realizado FROM v_desempenho_vendedor
                 WHERE periodo = :month ORDER BY realizado DESC, seller_id""")
                .param("month", month)
                .query((rs, n) -> {
                    BigDecimal sellerGoal = rs.getBigDecimal("meta");
                    BigDecimal sellerRealized = rs.getBigDecimal("realizado");
                    return new SellerMetric(new UserRef(rs.getLong("seller_id"), rs.getString("vendedor")),
                            MoneyDto.of(sellerGoal), MoneyDto.of(sellerRealized), achievement(sellerGoal, sellerRealized),
                            state(sellerGoal, sellerRealized));
                }).list();
        return new ManagementSummary(period, MoneyDto.of(goal), MoneyDto.of(realized),
                MoneyDto.of(goal.subtract(realized).max(BigDecimal.ZERO)), achievement(goal, realized), state(goal, realized), team);
    }
}
