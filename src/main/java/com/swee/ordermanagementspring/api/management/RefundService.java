package com.swee.ordermanagementspring.api.management;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.DbSessionContext;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs.UserRef;
import com.swee.ordermanagementspring.api.management.ManagementDtos.RefundRequest;
import com.swee.ordermanagementspring.api.management.ManagementDtos.RefundResponse;
import com.swee.ordermanagementspring.exceptions.ApiException;
import com.swee.ordermanagementspring.exceptions.ErrorCode;
import com.swee.ordermanagementspring.entities.StoreTime;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Estorno parcial ou total (C-07, RF-09): so pagamento APPROVED (senao 409), valor ate o saldo estornavel (senao 400).
 * O pagamento e bloqueado durante a operacao para dois estornos simultaneos nao passarem do valor pago.
 */
@Service
@PreAuthorize("hasRole('GERENTE')")
public class RefundService {
    private final JdbcClient jdbc;
    private final DbSessionContext session;

    public RefundService(JdbcClient jdbc, DbSessionContext session) {
        this.jdbc = jdbc;
        this.session = session;
    }

    @Transactional
    public RefundResponse refund(CurrentUser user, long paymentId, RefundRequest request) {
        session.apply(user.id(), request.reason().trim());
        record Payment(BigDecimal amount, String status) { }
        Payment payment = jdbc.sql("SELECT amount, status FROM payment WHERE id = :id FOR UPDATE").param("id", paymentId)
                .query((rs, n) -> new Payment(rs.getBigDecimal("amount"), rs.getString("status"))).optional()
                .orElseThrow(() -> ApiException.notFound("Payment not found, id: " + paymentId));
        if (!"APPROVED".equals(payment.status())) {
            throw ApiException.conflict(ErrorCode.STATE_CONFLICT, "Only approved payments can be refunded; payment is " + payment.status());
        }
        BigDecimal refunded = jdbc.sql("SELECT coalesce(sum(amount), 0) FROM refund WHERE payment_id = :id").param("id", paymentId)
                .query(BigDecimal.class).single();
        BigDecimal remaining = payment.amount().subtract(refunded);
        BigDecimal amount = request.amount().value();
        if (amount.signum() <= 0 || amount.compareTo(remaining) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                    "amount: must be greater than 0.00 and at most the refundable balance",
                    List.of("refundable=" + MoneyDto.of(remaining).amount()));
        }
        record Created(long id, OffsetDateTime at) { }
        Created created = jdbc.sql("""
                INSERT INTO refund(payment_id, amount, reason, created_by) VALUES (:payment, :amount, :reason, :user)
                RETURNING id, created_at""")
                .param("payment", paymentId).param("amount", amount).param("reason", request.reason().trim())
                .param("user", user.id())
                .query((rs, n) -> new Created(rs.getLong("id"),
                        rs.getObject("created_at", OffsetDateTime.class).atZoneSameInstant(StoreTime.ZONE).toOffsetDateTime()))
                .single();
        BigDecimal total = refunded.add(amount);
        return new RefundResponse(created.id(), paymentId, MoneyDto.of(amount), request.reason().trim(), MoneyDto.of(total),
                MoneyDto.of(payment.amount().subtract(total)), new UserRef(user.id(), user.name()), created.at());
    }
}
