package com.swee.ordermanagementspring.api.discounts;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.DbSessionContext;
import com.swee.ordermanagementspring.api.discounts.DiscountDtos.DiscountPolicy;
import com.swee.ordermanagementspring.api.discounts.DiscountDtos.DiscountPolicyRequest;
import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.security.AuditService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Alcadas de desconto manual: so o GERENTE consulta e altera, com motivo e auditoria (G-04, regra 6). */
@Service
@PreAuthorize("hasRole('GERENTE')")
public class DiscountPolicyService {
    private final DiscountPolicyRepository policies;
    private final JdbcClient jdbc;
    private final DbSessionContext session;
    private final AuditService audit;
    private final ObjectMapper mapper;

    public DiscountPolicyService(DiscountPolicyRepository policies, JdbcClient jdbc, DbSessionContext session, AuditService audit,
                                 ObjectMapper mapper) {
        this.policies = policies;
        this.jdbc = jdbc;
        this.session = session;
        this.audit = audit;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DiscountPolicy> list() {
        return policies.list();
    }

    @Transactional
    public DiscountPolicy update(CurrentUser user, UserRole role, DiscountPolicyRequest request) {
        session.apply(user.id(), request.reason().trim());
        var before = policies.forRole(role);
        jdbc.sql("""
                INSERT INTO discount_policy(role, max_percent, allow_below_cost, updated_by, updated_at)
                VALUES (:role, :max, :below, :user, now())
                ON CONFLICT (role) DO UPDATE SET max_percent = EXCLUDED.max_percent,
                       allow_below_cost = EXCLUDED.allow_below_cost, updated_by = EXCLUDED.updated_by, updated_at = now()""")
                .param("role", role.name()).param("max", request.maxPercent()).param("below", request.allowBelowCost())
                .param("user", user.id()).update();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("role", role.name());
        detail.put("fromPercent", before.maxPercent());
        detail.put("toPercent", request.maxPercent());
        detail.put("allowBelowCost", request.allowBelowCost());
        detail.put("reason", request.reason().trim());
        audit.record(AuditAction.DISCOUNT_POLICY_CHANGED, user.id(), "discount_policy", null, mapper.writeValueAsString(detail));
        return policies.list().stream().filter(p -> p.role() == role).findFirst().orElseThrow();
    }
}
