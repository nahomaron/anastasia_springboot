package com.anastasia.Anastasia_BackEnd.core.auth.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformAdminBootstrapAuditService {

    private final PlatformAdminBootstrapAuditRepository auditRepository;

    public void recordAttempt(
            String attemptedEmail,
            PlatformAdminBootstrapAuditOutcome outcome,
            String detail,
            String ipAddress,
            String userAgent,
            UUID createdUserId
    ) {
        PlatformAdminBootstrapAuditEvent event = new PlatformAdminBootstrapAuditEvent();
        event.setAttemptedEmail(normalize(attemptedEmail, 320));
        event.setOutcome(outcome);
        event.setDetail(normalize(detail, 512));
        event.setIpAddress(normalize(ipAddress, 128));
        event.setUserAgent(normalize(userAgent, 512));
        event.setCreatedUserId(createdUserId);
        event.setOccurredAt(Instant.now());
        auditRepository.save(event);
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
