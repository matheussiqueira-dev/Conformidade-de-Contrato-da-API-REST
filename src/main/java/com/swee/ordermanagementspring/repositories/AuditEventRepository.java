package com.swee.ordermanagementspring.repositories;

import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.audit.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByActionOrderByIdAsc(AuditAction action);
}
