package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantUserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantUserRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAdminNotificationService {

    private static final Set<TenantRole> MEMBER_REGISTRATION_NOTIFY_ROLES = EnumSet.of(
            TenantRole.PRIMARY_ADMIN,
            TenantRole.ADMIN
    );
    private static final Set<TenantRole> MEMBER_REGISTRATION_FALLBACK_ROLES = EnumSet.of(
            TenantRole.PRIMARY_OWNER,
            TenantRole.OWNER
    );

    private final TenantUserRepository tenantUserRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    public void notifyMemberRegistrationSubmitted(Adult_MemberEntity member, UUID submittedByUserId) {
        if (member == null || member.getTenantId() == null) {
            return;
        }

        UUID tenantId = member.getTenantId();

        Set<UUID> recipientIds = resolveRecipientIds(tenantId, submittedByUserId);

        if (recipientIds.isEmpty()) {
            log.debug("No tenant admin recipients found for member registration notification tenantId={}", tenantId);
            return;
        }

        Set<UserEntity> recipients = new HashSet<>(userRepository.findAllByUuidIn(recipientIds));

        for (UserEntity recipient : recipients) {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put("memberId", member.getId());
                props.put("memberName", buildMemberName(member));
                props.put("memberEmail", member.getEmail());
                props.put("membershipNumber", member.getMembershipNumber());
                props.put("churchNumber", member.getChurchNumber());
                props.put("status", member.getStatus());
                props.put("submittedByUserId", submittedByUserId);
                props.put("tenantId", tenantId);
                props.put("username", recipient.getFullName());

                publisher.publishEvent(new NotificationEvent(
                        this,
                        NotificationType.MEMBER_REGISTRATION_SUBMITTED,
                        recipient,
                        props,
                        EnumSet.of(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL)
                ));
            } catch (Exception ex) {
                log.error(
                        "Failed to publish member registration notification to recipient={} tenant={} member={}",
                        recipient.getUuid(),
                        tenantId,
                        member.getId(),
                        ex
                );
            }
        }
    }

    public void notifyChildRegistrationSubmitted(Child_MemberEntity child, UUID submittedByUserId) {
        if (child == null || child.getTenantId() == null) {
            return;
        }

        UUID tenantId = child.getTenantId();

        Set<UUID> recipientIds = resolveRecipientIds(tenantId, submittedByUserId);

        if (recipientIds.isEmpty()) {
            log.debug("No tenant admin recipients found for child registration notification tenantId={}", tenantId);
            return;
        }

        Set<UserEntity> recipients = new HashSet<>(userRepository.findAllByUuidIn(recipientIds));

        for (UserEntity recipient : recipients) {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put("childId", child.getId());
                props.put("childName", buildChildName(child));
                props.put("childEmail", child.getEmail());
                props.put("membershipNumber", child.getMembershipNumber());
                props.put("churchNumber", child.getChurchNumber());
                props.put("status", child.getStatus());
                props.put("submittedByUserId", submittedByUserId);
                props.put("tenantId", tenantId);
                props.put("username", recipient.getFullName());

                publisher.publishEvent(new NotificationEvent(
                        this,
                        NotificationType.CHILD_REGISTRATION_SUBMITTED,
                        recipient,
                        props,
                        EnumSet.of(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL)
                ));
            } catch (Exception ex) {
                log.error(
                        "Failed to publish child registration notification to recipient={} tenant={} child={}",
                        recipient.getUuid(),
                        tenantId,
                        child.getId(),
                        ex
                );
            }
        }
    }

    private String buildMemberName(Adult_MemberEntity member) {
        String first = member.getFirstName() == null ? "" : member.getFirstName().trim();
        String father = member.getFatherName() == null ? "" : member.getFatherName().trim();
        String fullName = (first + " " + father).trim();
        return fullName.isEmpty() ? "Unknown member" : fullName;
    }

    private String buildChildName(Child_MemberEntity child) {
        String first = child.getFirstName() == null ? "" : child.getFirstName().trim();
        String father = child.getFatherName() == null ? "" : child.getFatherName().trim();
        String fullName = (first + " " + father).trim();
        return fullName.isEmpty() ? "Unknown child" : fullName;
    }

    private Set<UUID> resolveRecipientIds(UUID tenantId, UUID submittedByUserId) {
        Set<UUID> primaryRecipients = tenantUserRepository
                .findActiveUsersByTenantIdAndRoles(tenantId, MEMBER_REGISTRATION_NOTIFY_ROLES)
                .stream()
                .map(TenantUserEntity::getUserId)
                .filter(userId -> submittedByUserId == null || !submittedByUserId.equals(userId))
                .collect(Collectors.toSet());

        if (!primaryRecipients.isEmpty()) {
            return primaryRecipients;
        }

        Set<UUID> fallbackRecipients = tenantUserRepository
                .findActiveUsersByTenantIdAndRoles(tenantId, MEMBER_REGISTRATION_FALLBACK_ROLES)
                .stream()
                .map(TenantUserEntity::getUserId)
                .filter(userId -> submittedByUserId == null || !submittedByUserId.equals(userId))
                .collect(Collectors.toSet());

        if (!fallbackRecipients.isEmpty()) {
            log.warn(
                    "No PRIMARY_ADMIN/ADMIN recipients found for tenant={}, falling back to owner roles for registration notifications",
                    tenantId
            );
        }
        return fallbackRecipients;
    }
}
