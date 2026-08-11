package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.domain.entity.AuditLog;
import com.adwitiya.feedbackportal.repository.AuditLogRepository;
import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, Object entityId, String details) {
        AppUserDetails actor = SecurityUtils.currentUser().orElse(null);
        write(action, entityType, entityId, details,
                actor != null ? actor.id() : null,
                actor != null ? actor.email() : "anonymous",
                null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthEvent(String action, String email, String ipAddress, String details) {
        write(action, "User", null, details, null, email, ipAddress);
    }

    private void write(String action, String entityType, Object entityId, String details,
                       Long actorId, String actorEmail, String ipAddress) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .actorId(actorId)
                    .actorEmail(actorEmail)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId != null ? String.valueOf(entityId) : null)
                    .details(details)
                    .ipAddress(ipAddress)
                    .createdAt(Instant.now())
                    .build());
        } catch (RuntimeException ex) {
            log.error("Failed to write audit record for action {}", action, ex);
        }
    }
}
