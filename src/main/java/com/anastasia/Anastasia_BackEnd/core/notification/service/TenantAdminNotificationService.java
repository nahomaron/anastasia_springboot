package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestEntity;
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

    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
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

    public void notifyBaptismRequestSubmitted(BaptismRequestEntity request, UUID submittedByUserId) {
        if (request == null || request.getTenantId() == null) {
            return;
        }

        UUID tenantId = request.getTenantId();
        Set<UUID> recipientIds = resolveRecipientIds(tenantId, submittedByUserId);

        if (recipientIds.isEmpty()) {
            log.debug("No tenant admin recipients found for baptism request notification tenantId={}", tenantId);
            return;
        }

        Set<UserEntity> recipients = new HashSet<>(userRepository.findAllByUuidIn(recipientIds));

        for (UserEntity recipient : recipients) {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put("requestId", request.getId());
                props.put("requestNumber", request.getRequestNumber());
                props.put("status", request.getStatus());
                props.put("memberName", request.getEnglish() != null ? request.getEnglish().getFullName() : null);
                props.put("churchNumber", request.getChurchNumber());
                props.put("tenantId", tenantId);
                props.put("submittedByUserId", submittedByUserId);
                props.put("username", recipient.getFullName());

                publisher.publishEvent(new NotificationEvent(
                        this,
                        NotificationType.BAPTISM_REQUEST_SUBMITTED,
                        recipient,
                        props,
                        EnumSet.of(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL)
                ));
            } catch (Exception ex) {
                log.error(
                        "Failed to publish baptism request notification to recipient={} tenant={} request={}",
                        recipient.getUuid(),
                        tenantId,
                        request.getId(),
                        ex
                );
            }
        }
    }

    public void notifyMarriageCaseSubmitted(MarriageCaseEntity marriageCase, UUID submittedByUserId) {
        publishMarriageNotification(marriageCase, submittedByUserId, NotificationType.MARRIAGE_CASE_SUBMITTED, "Marriage case submitted");
    }

    public void notifyMarriageCaseBothSubmitted(MarriageCaseEntity marriageCase, UUID submittedByUserId) {
        publishMarriageNotification(marriageCase, submittedByUserId, NotificationType.MARRIAGE_CASE_BOTH_SUBMITTED, "Both parties submitted");
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
        Set<UUID> primaryRecipients = tenantAdminAssignmentRepository
                .findActiveUsersByTenantIdAndRoles(tenantId, MEMBER_REGISTRATION_NOTIFY_ROLES)
                .stream()
                .map(TenantAdminAssignmentEntity::getUserId)
                .filter(userId -> submittedByUserId == null || !submittedByUserId.equals(userId))
                .collect(Collectors.toSet());

        if (!primaryRecipients.isEmpty()) {
            return primaryRecipients;
        }

        Set<UUID> fallbackRecipients = tenantAdminAssignmentRepository
                .findActiveUsersByTenantIdAndRoles(tenantId, MEMBER_REGISTRATION_FALLBACK_ROLES)
                .stream()
                .map(TenantAdminAssignmentEntity::getUserId)
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

    private void publishMarriageNotification(
            MarriageCaseEntity marriageCase,
            UUID submittedByUserId,
            NotificationType notificationType,
            String stageLabel
    ) {
        if (marriageCase == null || marriageCase.getTenantId() == null) {
            return;
        }

        UUID tenantId = marriageCase.getTenantId();
        Set<UUID> recipientIds = resolveRecipientIds(tenantId, submittedByUserId);
        if (recipientIds.isEmpty()) {
            log.debug("No tenant admin recipients found for marriage notification tenantId={}", tenantId);
            return;
        }

        Set<UserEntity> recipients = new HashSet<>(userRepository.findAllByUuidIn(recipientIds));
        for (UserEntity recipient : recipients) {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put("caseId", marriageCase.getId());
                props.put("caseReference", marriageCase.getCaseReference());
                props.put("status", marriageCase.getStatus());
                props.put("churchId", marriageCase.getChurchId());
                props.put("churchName", marriageCase.getChurch() == null ? null : marriageCase.getChurch().getChurchNameLocal());
                props.put("tenantId", tenantId);
                props.put("submittedByUserId", submittedByUserId);
                props.put("stageLabel", stageLabel);
                props.put("username", recipient.getFullName());

                publisher.publishEvent(new NotificationEvent(
                        this,
                        notificationType,
                        recipient,
                        props,
                        EnumSet.of(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL)
                ));
            } catch (Exception ex) {
                log.error(
                        "Failed to publish marriage notification to recipient={} tenant={} case={}",
                        recipient.getUuid(),
                        tenantId,
                        marriageCase.getId(),
                        ex
                );
            }
        }
    }
}
