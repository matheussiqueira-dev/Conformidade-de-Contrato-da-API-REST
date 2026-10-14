package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.audit.AuditEvent;
import com.swee.ordermanagementspring.repositories.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;

/** D7: grava audit_event em transacao propria, para que a falha de login tambem fique registrada. */
@Service
public class AuditService {
    private final AuditEventRepository events;
    private final Clock clock;

    public AuditService(AuditEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, Long actorId, String entity, Long entityId, String detailJson) {
        events.save(new AuditEvent(clock.instant(), actorId, action, entity, entityId, detailJson));
    }

    /** E-mail tentado so como SHA-256 (nunca em claro), normalizado como no login. */
    public static String emailDetail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return "{\"emailSha256\":\"" + HexFormat.of().formatHex(hash) + "\"}";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
