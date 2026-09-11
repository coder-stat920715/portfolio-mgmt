package com.interview.portfolio.service;

import com.interview.portfolio.entity.AuditLog;
import com.interview.portfolio.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interview concept - Propagation.REQUIRES_NEW.
 * This method ALWAYS starts a brand-new, independent physical transaction, suspending
 * whatever transaction (if any) is currently active on the calling thread. Consequently:
 *   - If the audit write itself fails, it does NOT roll back the caller's transaction.
 *   - If the CALLER's transaction later rolls back (e.g. a trade is rejected due to
 *     insufficient funds), this audit record has ALREADY been committed independently
 *     and survives - which is exactly the compliance requirement: "always record what
 *     was attempted, regardless of outcome."
 * Contrast with Propagation.REQUIRED (the default) used elsewhere, which joins the
 * existing transaction if one exists, or starts one if not - meaning a rollback of the
 * outer transaction rolls back everything done under REQUIRED as well.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String entityName, Long entityId, String action, String performedBy, String details) {
        AuditLog log = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }
}
