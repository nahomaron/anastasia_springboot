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
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.TenantEntitlementAccessService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final TenantEntitlementAccessService entitlementAccessService;

    @Transactional(readOnly = true)
    public NotificationInboxPageResponse listInbox(String status, String type, int page, int size) {
        ActorScope scope = resolveActorScope();
        UUID effectiveTenantId = effectiveNotificationTenantId(scope);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Set<NotificationType> types = parseTypes(type);
        Page<NotificationEntity> rows = types.isEmpty()
                ? notificationRepository.findInbox(
                scope.userId(), effectiveTenantId, NotificationChannelType.IN_APP, pageable)
                : notificationRepository.findInboxByTypes(
                scope.userId(), effectiveTenantId, NotificationChannelType.IN_APP, types, pageable);

        List<NotificationInboxItemResponse> items = rows.getContent().stream()
                .filter(n -> !"UNREAD".equalsIgnoreCase(status) || n.getReadAt() == null)
                .filter(n -> !"READ".equalsIgnoreCase(status) || n.getReadAt() != null)
                .map(this::toInboxItem)
                .toList();

        long unreadCount = notificationRepository.countUnread(effectiveTenantId, scope.userId());

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
        return notificationRepository.countUnread(effectiveNotificationTenantId(scope), scope.userId());
    }

    @Transactional
    public NotificationInboxItemResponse markRead(Long notificationId) {
        ActorScope scope = resolveActorScope();
        UUID effectiveTenantId = effectiveNotificationTenantId(scope);
        NotificationEntity entity = notificationRepository
                .findByIdAndScope(notificationId, scope.userId(), effectiveTenantId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (entity.getReadAt() == null) {
            entity.setReadAt(Instant.now());
        }
        return toInboxItem(notificationRepository.save(entity));
    }

    @Transactional
    public int markAllRead() {
        ActorScope scope = resolveActorScope();
        return notificationRepository.markAllRead(effectiveNotificationTenantId(scope), scope.userId(), Instant.now());
    }

    @Transactional
    public void archive(Long notificationId) {
        ActorScope scope = resolveActorScope();
        UUID effectiveTenantId = effectiveNotificationTenantId(scope);
        NotificationEntity entity = notificationRepository
                .findByIdAndScope(notificationId, scope.userId(), effectiveTenantId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        entity.setArchivedAt(Instant.now());
        notificationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences() {
        ActorScope scope = resolveActorScope();
        return findPreference(effectiveNotificationTenantId(scope), scope.userId())
                .map(this::toPreferenceResponse)
                .orElseGet(this::defaultPreferenceResponse);
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(UpdateNotificationPreferencesRequest request) {
        ActorScope scope = resolveActorScope();
        NotificationPreferenceEntity preference = findOrCreatePreference(effectiveNotificationTenantId(scope), scope.userId());
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
        UUID tenantId = entity.getTenantId();
        if (tenantId == null && entity.getTenant() != null) {
            tenantId = entity.getTenant().getId();
        }
        return new NotificationInboxItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getType(),
                entity.getReadAt() != null,
                entity.getCreatedAt(),
                entity.getReadAt(),
                tenantId
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

    private UUID effectiveNotificationTenantId(ActorScope scope) {
        if (scope == null || scope.tenantId() == null) {
            return null;
        }
        try {
            entitlementAccessService.requireFeature(TenantFeature.NOTIFICATIONS);
            return scope.tenantId();
        } catch (AccessDeniedException ex) {
            return null;
        }
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
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("No authenticated user found");
        }

        UserPrincipal principal = auth.getPrincipal() instanceof UserPrincipal userPrincipal
                ? userPrincipal
                : null;
        UserEntity user = resolveAuthenticatedUser(auth, principal);

        UUID tenantId = resolveTenantId(principal, user);
        if (tenantId != null && !hasPlatformScope(auth)) {
            UUID userTenantId = user.getTenantId();
            boolean explicitlyAddressedInTenant = userTenantId == null
                    && notificationRepository.existsActiveTenantInboxNotificationForRecipient(tenantId, user.getUuid());
            if ((userTenantId == null && !explicitlyAddressedInTenant)
                    || (userTenantId != null && !tenantId.equals(userTenantId))) {
                throw new AccessDeniedException("Authenticated user is not in tenant scope");
            }
        }

        return new ActorScope(tenantId, user.getUuid());
    }

    private UserEntity resolveAuthenticatedUser(Authentication auth, UserPrincipal principal) {
        if (principal != null && principal.getUserUuid() != null) {
            return userRepository.findById(principal.getUserUuid())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
        }

        if (StringUtils.hasText(auth.getName())) {
            return userRepository.findByEmailIgnoreCase(auth.getName())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
        }

        throw new AccessDeniedException("No authenticated user found");
    }

    private UUID resolveTenantId(UserPrincipal principal, UserEntity user) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        if (principal != null && principal.getTenantId() != null) {
            return principal.getTenantId();
        }
        return user.getTenantId();
    }

    private boolean hasPlatformScope(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_PLATFORM_ADMIN".equals(authority)
                        || "MANAGE_TENANTS".equals(authority)
                        || "VIEW_ALL_DATA".equals(authority));
    }

    private record ActorScope(UUID tenantId, UUID userId) {
    }
}
