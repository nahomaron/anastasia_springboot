package com.anastasia.Anastasia_BackEnd.common.auditing;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuditLog record(AuditEntry entry) {
        AuditLog auditLog = new AuditLog();
        resolveActor(entry.actorUserId()).ifPresent(auditLog::setUser);
        auditLog.setActorIdentifier(trim(entry.actorIdentifier(), 191));
        auditLog.setAction(entry.eventType().name());
        auditLog.setTenantId(entry.tenantId());
        auditLog.setTargetType(trim(entry.targetType(), 64));
        auditLog.setTargetId(trim(entry.targetId(), 128));
        auditLog.setResult(trim(entry.result(), 32));
        auditLog.setReason(trim(entry.reason(), 512));
        auditLog.setContext(trim(entry.context(), 2000));
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setIpAddress(trim(resolveClientIp(), 96));
        auditLog.setUserAgent(trim(resolveUserAgent(), 512));
        return auditRepository.save(auditLog);
    }

    public void record(
            AuditEventType eventType,
            String result,
            UUID actorUserId,
            String actorIdentifier,
            UUID tenantId,
            String targetType,
            String targetId,
            String reason,
            String context
    ) {
        record(AuditEntry.builder()
                .eventType(eventType)
                .result(result)
                .actorUserId(actorUserId)
                .actorIdentifier(actorIdentifier)
                .tenantId(tenantId)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .context(context)
                .build());
    }

    private Optional<UserEntity> resolveActor(UUID actorUserId) {
        if (actorUserId == null) {
            return Optional.empty();
        }
        return userRepository.findById(actorUserId);
    }

    private String resolveClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwardedFor = trim(request.getHeader("X-Forwarded-For"), 256);
        if (forwardedFor != null) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = trim(request.getHeader("X-Real-IP"), 256);
        return realIp != null ? realIp : request.getRemoteAddr();
    }

    private String resolveUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    @Builder
    public record AuditEntry(
            AuditEventType eventType,
            String result,
            UUID actorUserId,
            String actorIdentifier,
            UUID tenantId,
            String targetType,
            String targetId,
            String reason,
            String context
    ) {
    }
}
