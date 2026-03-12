package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.AddUsersToGroupRequest;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.AddUsersToGroupResponse;
import com.anastasia.Anastasia_BackEnd.modules.groups.service.GroupService;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk.BulkMemberActionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk.BulkMemberAddToGroupRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk.BulkMemberCommunicationRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk.BulkMemberTargetType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberBulkActionService {

    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final GroupService groupService;
    private final ApplicationEventPublisher eventPublisher;
    private final LocalizedMessageService messageService;

    @Transactional(readOnly = true)
    public BulkMemberActionResponse sendCommunication(BulkMemberCommunicationRequest request) {
        Set<Long> requestedIds = normalizeIds(request.getMemberIds());
        List<MemberTarget> targets = resolveTargets(request.getMemberType(), requestedIds);

        int missingUserCount = 0;
        int missingContactCount = 0;
        int processedCount = 0;

        for (MemberTarget target : targets) {
            if (request.getChannel() == NotificationChannelType.IN_APP && target.user() == null) {
                missingUserCount++;
                continue;
            }

            if ((request.getChannel() == NotificationChannelType.EMAIL && !StringUtils.hasText(target.email()))
                    || (request.getChannel() == NotificationChannelType.SMS && !StringUtils.hasText(target.phone()))) {
                missingContactCount++;
                continue;
            }

            Map<String, Object> properties = new HashMap<>();
            properties.put("title", resolveTitle(request));
            properties.put("subject", resolveSubject(request));
            properties.put("message_content", request.getMessage().trim());
            properties.put("plainText", request.getMessage().trim());
            properties.put("username", target.fullName());
            properties.put("email", target.email());
            properties.put("phone", target.phone());
            properties.put("whatsApp", target.whatsApp());

            NotificationEvent event = new NotificationEvent(
                    this,
                    NotificationType.NOTIFICATION,
                    target.user(),
                    properties,
                    EnumSet.of(request.getChannel())
            );
            eventPublisher.publishEvent(event);
            processedCount++;
        }

        int matchedCount = targets.size();
        int notFoundCount = Math.max(0, requestedIds.size() - matchedCount);
        int skippedCount = missingUserCount + missingContactCount + notFoundCount;

        return BulkMemberActionResponse.builder()
                .requestedCount(requestedIds.size())
                .matchedCount(matchedCount)
                .processedCount(processedCount)
                .skippedCount(skippedCount)
                .notFoundCount(notFoundCount)
                .missingUserCount(missingUserCount)
                .missingContactCount(missingContactCount)
                .message(buildCommunicationMessage(processedCount, request.getChannel(), matchedCount, skippedCount))
                .build();
    }

    @Transactional(readOnly = true)
    public BulkMemberActionResponse addToGroup(BulkMemberAddToGroupRequest request) {
        Set<Long> requestedIds = normalizeIds(request.getMemberIds());
        List<MemberTarget> targets = resolveTargets(request.getMemberType(), requestedIds);

        Set<UUID> userIds = new LinkedHashSet<>();
        int missingUserCount = 0;
        for (MemberTarget target : targets) {
            if (target.userId() == null) {
                missingUserCount++;
                continue;
            }
            userIds.add(target.userId());
        }

        int processedCount = 0;
        int skippedCount = missingUserCount;
        int notFoundCount = Math.max(0, requestedIds.size() - targets.size());
        skippedCount += notFoundCount;

        if (!userIds.isEmpty()) {
            AddUsersToGroupResponse response = groupService.addUsersToGroup(
                    request.getGroupId(),
                    AddUsersToGroupRequest.builder().userIds(userIds).build()
            );
            processedCount = response.getAddedCount();
            skippedCount += response.getSkippedCount() + response.getNotFoundCount();
        }

        return BulkMemberActionResponse.builder()
                .requestedCount(requestedIds.size())
                .matchedCount(targets.size())
                .processedCount(processedCount)
                .skippedCount(skippedCount)
                .notFoundCount(notFoundCount)
                .missingUserCount(missingUserCount)
                .missingContactCount(0)
                .message(buildGroupMessage(processedCount, skippedCount))
                .build();
    }

    private List<MemberTarget> resolveTargets(BulkMemberTargetType type, Set<Long> ids) {
        UUID tenantId = requireTenantId();
        List<? extends BaseMember> members = type == BulkMemberTargetType.CHILD
                ? childRepository.findAllByIdInAndTenantId(ids, tenantId)
                : memberRepository.findAllByIdInAndTenantId(ids, tenantId);

        List<MemberTarget> targets = new ArrayList<>();
        for (BaseMember member : members) {
            UserEntity user = member.getUser();
            Long memberId = member instanceof Adult_MemberEntity adult
                    ? adult.getId()
                    : member instanceof Child_MemberEntity child ? child.getId() : null;
            if (memberId == null) {
                continue;
            }
            targets.add(new MemberTarget(
                    memberId,
                    user != null ? user.getUuid() : member.getUserId(),
                    user,
                    member.getEmail(),
                    member.getPhone(),
                    member.getWhatsApp(),
                    buildFullName(member)
            ));
        }
        return targets;
    }

    private String buildFullName(BaseMember member) {
        return String.join(" ",
                        safe(member.getFirstName()),
                        safe(member.getFatherName()),
                        safe(member.getGrandFatherName()))
                .trim();
    }

    private String resolveTitle(BulkMemberCommunicationRequest request) {
        String fallback = switch (request.getChannel()) {
            case SMS -> "SMS";
            case EMAIL -> "Email";
            case IN_APP -> "Notification";
            case WHATSAPP -> "Message";
        };
        return StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : fallback;
    }

    private String resolveSubject(BulkMemberCommunicationRequest request) {
        if (StringUtils.hasText(request.getSubject())) {
            return request.getSubject().trim();
        }
        return resolveTitle(request);
    }

    private String buildCommunicationMessage(int processedCount,
                                             NotificationChannelType channel,
                                             int matchedCount,
                                             int skippedCount) {
        String action = switch (channel) {
            case IN_APP -> "in-app notification";
            case SMS -> "SMS";
            case EMAIL -> "email";
            case WHATSAPP -> "WhatsApp message";
        };
        if (processedCount == 0) {
            return "No " + action + "s were queued.";
        }
        return "Queued " + processedCount + " " + action + (processedCount == 1 ? "" : "s")
                + " from " + matchedCount + " matched member(s)"
                + (skippedCount > 0 ? "; " + skippedCount + " skipped." : ".");
    }

    private String buildGroupMessage(int processedCount, int skippedCount) {
        if (processedCount == 0) {
            return "No members were added to the group.";
        }
        return "Added " + processedCount + " member(s) to the group"
                + (skippedCount > 0 ? "; " + skippedCount + " skipped." : ".");
    }

    private Set<Long> normalizeIds(Set<Long> ids) {
        return ids == null ? Set.of() : new LinkedHashSet<>(ids);
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "registration.bulkActions.tenantContext.missing",
                    "Tenant context is required for bulk member actions."
            ));
        }
        return tenantId;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record MemberTarget(
            Long memberId,
            UUID userId,
            UserEntity user,
            String email,
            String phone,
            String whatsApp,
            String fullName
    ) {
    }
}
