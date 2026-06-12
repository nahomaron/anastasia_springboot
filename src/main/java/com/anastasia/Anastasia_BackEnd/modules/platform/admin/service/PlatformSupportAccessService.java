package com.anastasia.Anastasia_BackEnd.modules.platform.admin.service;

import com.anastasia.Anastasia_BackEnd.common.auditing.AuditEventType;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditLogService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.SupportAccessActionResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.SupportAccessSessionEndRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.SupportAccessSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.SupportAccessSessionStartRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessActionEntity;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessActionOutcome;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessActionType;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessScope;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository.SupportAccessActionRepository;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository.SupportAccessSessionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSettingsEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSettingsRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformSupportAccessService {

    public static final String SESSION_HEADER = "X-Support-Access-Session";

    private final SupportAccessSessionRepository supportAccessSessionRepository;
    private final SupportAccessActionRepository supportAccessActionRepository;
    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SupportAccessSessionResponse startSession(UUID actorUserId, SupportAccessSessionStartRequest request) {
        UserEntity actor = requireUser(actorUserId);
        TenantEntity tenant = requireTenant(request.getTenantId());
        String reason = normalizeRequired(request.getReason(), 512, "Support access reason is required.");
        SupportAccessScope scope = request.getScope();
        Instant now = Instant.now();

        if (!isSupportAccessEnabled(tenant.getId())) {
            SupportAccessSessionEntity denied = supportAccessSessionRepository.save(SupportAccessSessionEntity.builder()
                    .actor(actor)
                    .tenant(tenant)
                    .reason(reason)
                    .scope(scope)
                    .status(SupportAccessSessionStatus.DENIED)
                    .denialReason("Tenant has disabled Anastasis support access.")
                    .endedAt(now)
                    .lastActivityAt(now)
                    .createdBy(actorUserId)
                    .updatedBy(actorUserId)
                    .build());
            auditSupportAccess(actorUserId, actor.getEmail(), tenant.getId(), denied.getId(), "DENIED",
                    "tenant-disabled", "Support access denied for tenant " + tenant.getId());
            return toSessionResponse(denied, List.of());
        }

        SupportAccessSessionEntity session = supportAccessSessionRepository.save(SupportAccessSessionEntity.builder()
                .actor(actor)
                .tenant(tenant)
                .reason(reason)
                .scope(scope)
                .status(SupportAccessSessionStatus.ACTIVE)
                .startedAt(now)
                .lastActivityAt(now)
                .createdBy(actorUserId)
                .updatedBy(actorUserId)
                .build());
        auditSupportAccess(actorUserId, actor.getEmail(), tenant.getId(), session.getId(), "SUCCESS",
                null, "Support access session started with scope " + scope);
        return toSessionResponse(session, List.of());
    }

    @Transactional
    public SupportAccessSessionResponse endSession(UUID actorUserId, UUID sessionId, SupportAccessSessionEndRequest request) {
        SupportAccessSessionEntity session = supportAccessSessionRepository.findByIdAndActor_Uuid(sessionId, actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Support access session not found."));
        if (session.getStatus() == SupportAccessSessionStatus.ENDED) {
            return toSessionResponse(session, listActionsForSessionIds(Set.of(session.getId())).getOrDefault(session.getId(), List.of()));
        }

        Instant now = Instant.now();
        session.setStatus(SupportAccessSessionStatus.ENDED);
        session.setEndedAt(now);
        session.setLastActivityAt(now);
        session.setEndReason(normalizeOptional(request != null ? request.getEndReason() : null, 512));
        session.setUpdatedBy(actorUserId);
        SupportAccessSessionEntity saved = supportAccessSessionRepository.save(session);
        auditSupportAccess(actorUserId, saved.getActor().getEmail(), saved.getTenant().getId(), saved.getId(), "SUCCESS",
                saved.getEndReason(), "Support access session ended");
        return toSessionResponse(saved, listActionsForSessionIds(Set.of(saved.getId())).getOrDefault(saved.getId(), List.of()));
    }

    @Transactional(readOnly = true)
    public SupportAccessSessionEntity authorizeSession(UUID actorUserId, UUID tenantId, UUID sessionId, SupportAccessScope requiredScope) {
        SupportAccessSessionEntity session = supportAccessSessionRepository.findByIdAndActor_Uuid(sessionId, actorUserId)
                .orElseThrow(() -> new AccessDeniedException("An active support access session is required."));
        if (session.getStatus() != SupportAccessSessionStatus.ACTIVE) {
            throw new AccessDeniedException("The support access session is no longer active.");
        }
        if (!session.getTenant().getId().equals(tenantId)) {
            throw new AccessDeniedException("The support access session does not match the requested tenant.");
        }
        if (requiredScope == SupportAccessScope.READ_WRITE && !session.getScope().allowsWrites()) {
            throw new AccessDeniedException("This support access session is read-only.");
        }
        if (!isSupportAccessEnabled(tenantId)) {
            throw new AccessDeniedException("This tenant has disabled Anastasis support access.");
        }
        return session;
    }

    @Transactional(readOnly = true)
    public SupportAccessSessionEntity authorizeCurrentRequestSession(UUID actorUserId, UUID tenantId, SupportAccessScope requiredScope) {
        UUID sessionId = currentRequestSessionId();
        if (sessionId == null) {
            throw new AccessDeniedException("An active support access session header is required.");
        }
        return authorizeSession(actorUserId, tenantId, sessionId, requiredScope);
    }

    @Transactional
    public void recordAction(
            SupportAccessSessionEntity session,
            SupportAccessActionType actionType,
            String httpMethod,
            String requestPath,
            int responseStatus,
            String detail
    ) {
        Instant now = Instant.now();
        session.setLastActivityAt(now);
        session.setUpdatedBy(session.getActor().getUuid());
        supportAccessSessionRepository.save(session);

        supportAccessActionRepository.save(SupportAccessActionEntity.builder()
                .session(session)
                .actor(session.getActor())
                .tenant(session.getTenant())
                .actionType(actionType)
                .httpMethod(normalizeRequired(httpMethod, 16, "HTTP method is required."))
                .requestPath(normalizeRequired(requestPath, 512, "Request path is required."))
                .responseStatus(responseStatus)
                .outcome(responseStatus >= 400 ? SupportAccessActionOutcome.FAILED : SupportAccessActionOutcome.SUCCESS)
                .detail(normalizeOptional(detail, 1024))
                .occurredAt(now)
                .build());
    }

    @Transactional(readOnly = true)
    public List<SupportAccessSessionResponse> listRecentTenantHistory(UUID tenantId) {
        List<SupportAccessSessionEntity> sessions = supportAccessSessionRepository.findTop20ByTenant_IdOrderByCreatedAtDesc(tenantId);
        Map<UUID, List<SupportAccessActionEntity>> actionsBySessionId = listActionsForSessionIds(
                sessions.stream().map(SupportAccessSessionEntity::getId).collect(Collectors.toSet())
        );
        return sessions.stream()
                .map(session -> toSessionResponse(session, actionsBySessionId.getOrDefault(session.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isSupportAccessEnabled(UUID tenantId) {
        return tenantSettingsRepository.findById(tenantId)
                .map(TenantSettingsEntity::isSupportAccessEnabled)
                .orElse(true);
    }

    private Map<UUID, List<SupportAccessActionEntity>> listActionsForSessionIds(Set<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<SupportAccessActionEntity>> grouped = new LinkedHashMap<>();
        for (SupportAccessActionEntity action : supportAccessActionRepository.findBySession_IdInOrderByOccurredAtDesc(sessionIds)) {
            grouped.computeIfAbsent(action.getSession().getId(), ignored -> new ArrayList<>()).add(action);
        }
        return grouped;
    }

    private SupportAccessSessionResponse toSessionResponse(SupportAccessSessionEntity session, List<SupportAccessActionEntity> actions) {
        return SupportAccessSessionResponse.builder()
                .sessionId(session.getId())
                .actorUserId(session.getActor().getUuid())
                .actorName(session.getActor().getFullName())
                .actorEmail(session.getActor().getEmail())
                .tenantId(session.getTenant().getId())
                .tenantName(session.getTenant().getDisplayName())
                .reason(session.getReason())
                .scope(session.getScope())
                .status(session.getStatus())
                .denialReason(session.getDenialReason())
                .endReason(session.getEndReason())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .lastActivityAt(session.getLastActivityAt())
                .createdAt(session.getCreatedAt())
                .actions(actions.stream().map(this::toActionResponse).toList())
                .build();
    }

    private SupportAccessActionResponse toActionResponse(SupportAccessActionEntity action) {
        return SupportAccessActionResponse.builder()
                .actionId(action.getId())
                .actionType(action.getActionType())
                .httpMethod(action.getHttpMethod())
                .requestPath(action.getRequestPath())
                .responseStatus(action.getResponseStatus())
                .outcome(action.getOutcome())
                .detail(action.getDetail())
                .occurredAt(action.getOccurredAt())
                .build();
    }

    private UserEntity requireUser(UUID actorUserId) {
        return userRepository.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Support access actor not found."));
    }

    private TenantEntity requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found."));
    }

    private String normalizeRequired(String value, int maxLength, String message) {
        String normalized = normalizeOptional(value, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private UUID currentRequestSessionId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        String raw = servletRequestAttributes.getRequest().getHeader(SESSION_HEADER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid support access session header.");
        }
    }

    private void auditSupportAccess(
            UUID actorUserId,
            String actorIdentifier,
            UUID tenantId,
            UUID sessionId,
            String result,
            String reason,
            String context
    ) {
        auditLogService.record(
                AuditEventType.SUPPORT_ACCESS_GRANTED,
                result,
                actorUserId,
                actorIdentifier,
                tenantId,
                "SUPPORT_ACCESS_SESSION",
                sessionId != null ? sessionId.toString() : null,
                reason,
                context
        );
    }
}
