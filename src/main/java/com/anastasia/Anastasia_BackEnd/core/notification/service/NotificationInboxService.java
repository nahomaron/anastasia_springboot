package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationPreferenceEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationInboxItemResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationInboxPageResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationPreferencesResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.UpdateNotificationPreferencesRequest;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationPreferenceRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationInboxService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationInboxPageResponse listInbox(String status, String type, int page, int size) {
        ActorScope scope = resolveActorScope();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Set<NotificationType> types = parseTypes(type);
        Page<NotificationEntity> rows = types.isEmpty()
                ? notificationRepository.findInbox(
                scope.userId(), scope.tenantId(), NotificationChannelType.IN_APP, pageable)
                : notificationRepository.findInboxByTypes(
                scope.userId(), scope.tenantId(), NotificationChannelType.IN_APP, types, pageable);

        List<NotificationInboxItemResponse> items = rows.getContent().stream()
                .filter(n -> !"UNREAD".equalsIgnoreCase(status) || n.getReadAt() == null)
                .filter(n -> !"READ".equalsIgnoreCase(status) || n.getReadAt() != null)
                .map(this::toInboxItem)
                .toList();

        long unreadCount = notificationRepository.countUnread(scope.tenantId(), scope.userId());

        return new NotificationInboxPageResponse(
                items,
                rows.getNumber(),
                rows.getSize(),
                rows.getTotalPages(),
                rows.getTotalElements(),
                List.of(20, 50, 100, 200),
                unreadCount
        );
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        ActorScope scope = resolveActorScope();
        return notificationRepository.countUnread(scope.tenantId(), scope.userId());
    }

    @Transactional
    public NotificationInboxItemResponse markRead(Long notificationId) {
        ActorScope scope = resolveActorScope();
        NotificationEntity entity = notificationRepository
                .findByIdAndScope(notificationId, scope.userId(), scope.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (entity.getReadAt() == null) {
            entity.setReadAt(Instant.now());
        }
        return toInboxItem(notificationRepository.save(entity));
    }

    @Transactional
    public int markAllRead() {
        ActorScope scope = resolveActorScope();
        return notificationRepository.markAllRead(scope.tenantId(), scope.userId(), Instant.now());
    }

    @Transactional
    public void archive(Long notificationId) {
        ActorScope scope = resolveActorScope();
        NotificationEntity entity = notificationRepository
                .findByIdAndScope(notificationId, scope.userId(), scope.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        entity.setArchivedAt(Instant.now());
        notificationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences() {
        ActorScope scope = resolveActorScope();
        return findPreference(scope.tenantId(), scope.userId())
                .map(this::toPreferenceResponse)
                .orElseGet(this::defaultPreferenceResponse);
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(UpdateNotificationPreferencesRequest request) {
        ActorScope scope = resolveActorScope();
        NotificationPreferenceEntity preference = findOrCreatePreference(scope.tenantId(), scope.userId());
        preference.setEmailEnabled(Boolean.TRUE.equals(request.getEmailEnabled()));
        preference.setSmsEnabled(Boolean.TRUE.equals(request.getSmsEnabled()));
        preference.setInAppEnabled(Boolean.TRUE.equals(request.getInAppEnabled()));
        preference.setMutedTypes(request.getMutedTypes() == null
                ? EnumSet.noneOf(NotificationType.class)
                : EnumSet.copyOf(request.getMutedTypes()));
        NotificationPreferenceEntity saved = preferenceRepository.save(preference);
        return toPreferenceResponse(saved);
    }

    @Transactional(readOnly = true)
    public Set<NotificationChannelType> filterChannels(UUID tenantId, UUID userId, NotificationType type, Set<NotificationChannelType> requested) {
        if (userId == null || requested == null || requested.isEmpty()) {
            return requested;
        }
        NotificationPreferenceEntity pref = findPreference(tenantId, userId).orElse(null);
        if (pref == null) {
            return requested;
        }
        if (pref.getMutedTypes() != null && pref.getMutedTypes().contains(type)) {
            return EnumSet.noneOf(NotificationChannelType.class);
        }

        EnumSet<NotificationChannelType> allowed = EnumSet.noneOf(NotificationChannelType.class);
        for (NotificationChannelType channelType : requested) {
            if (channelType == NotificationChannelType.EMAIL && pref.isEmailEnabled()) {
                allowed.add(channelType);
            }
            if (channelType == NotificationChannelType.SMS && pref.isSmsEnabled()) {
                allowed.add(channelType);
            }
            if (channelType == NotificationChannelType.IN_APP && pref.isInAppEnabled()) {
                allowed.add(channelType);
            }
            if (channelType == NotificationChannelType.WHATSAPP) {
                allowed.add(channelType);
            }
        }
        return allowed;
    }

    private NotificationInboxItemResponse toInboxItem(NotificationEntity entity) {
        return new NotificationInboxItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getType(),
                entity.getReadAt() != null,
                entity.getCreatedAt(),
                entity.getReadAt()
        );
    }

    private NotificationPreferenceEntity findOrCreatePreference(UUID tenantId, UUID userId) {
        return findPreference(tenantId, userId)
                .orElseGet(() -> {
                    NotificationPreferenceEntity created = new NotificationPreferenceEntity();
                    created.setTenantId(tenantId);
                    created.setUserId(userId);
                    return preferenceRepository.save(created);
                });
    }

    private java.util.Optional<NotificationPreferenceEntity> findPreference(UUID tenantId, UUID userId) {
        if (tenantId == null) {
            return preferenceRepository.findByTenantIdIsNullAndUserId(userId);
        }
        return preferenceRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    private NotificationPreferencesResponse toPreferenceResponse(NotificationPreferenceEntity preference) {
        return new NotificationPreferencesResponse(
                preference.isEmailEnabled(),
                preference.isSmsEnabled(),
                preference.isInAppEnabled(),
                preference.getMutedTypes() == null ? Set.of() : Set.copyOf(preference.getMutedTypes())
        );
    }

    private NotificationPreferencesResponse defaultPreferenceResponse() {
        return new NotificationPreferencesResponse(true, false, true, Set.of());
    }

    private Set<NotificationType> parseTypes(String typeQuery) {
        if (typeQuery == null || typeQuery.isBlank() || "ALL".equalsIgnoreCase(typeQuery)) {
            return Set.of();
        }
        try {
            return Set.of(NotificationType.valueOf(typeQuery.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Set.of();
        }
    }

    private ActorScope resolveActorScope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("No authenticated user found");
        }

        UUID tenantId = TenantContext.getTenantId();
        UUID userId = principal.getUserUuid();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // If tenant context is provided, enforce strict tenant scope.
        if (tenantId != null && (user.getTenantId() == null || !tenantId.equals(user.getTenantId()))) {
            throw new IllegalStateException("Authenticated user is not in tenant scope");
        }

        return new ActorScope(tenantId, userId);
    }

    private record ActorScope(UUID tenantId, UUID userId) {
    }
}
