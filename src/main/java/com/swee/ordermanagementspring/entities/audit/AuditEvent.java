package com.swee.ordermanagementspring.entities.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** D7: trilha de auditoria; so recebe insercao. Nunca guarda senha nem documento completo. */
@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Instant occurredAt;
    private Long actorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private AuditAction action;
    @Column(length = 40)
    private String entity;
    private Long entityId;
    @JdbcTypeCode(SqlTypes.JSON)
    private String detail;

    protected AuditEvent() {}

    public AuditEvent(Instant occurredAt, Long actorId, AuditAction action, String entity, Long entityId, String detail) {
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public Long getActorId() { return actorId; }
    public AuditAction getAction() { return action; }
    public String getEntity() { return entity; }
    public Long getEntityId() { return entityId; }
    public String getDetail() { return detail; }
}
