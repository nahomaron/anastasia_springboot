package com.anastasia.Anastasia_BackEnd.modules.groups.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupJoinRequestRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.*;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.GroupMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private static final String GROUP_NOT_FOUND = "Group not found";
    private static final String USERS_REQUIRED_MESSAGE = "No users provided";

    private final GroupMapper groupMapper;
    private final GroupRepository groupRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final UserRepository userRepository;
    private final ChurchRepository churchRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LocalizedMessageService messageService;

    @Override
    public GroupEntity convertToEntity(GroupDTO groupDTO) {
        return groupMapper.groupDTOToEntity(groupDTO);
    }

    @Override
    public GroupResponse convertToResponse(GroupEntity groupEntity) {
        return groupMapper.groupEntityToResponse(groupEntity);
    }

    @Override
    public GroupResponse createGroup(GroupDTO groupDTO) {
        UUID tenantId = requireTenantId();

        if (groupRepository.existsByGroupNameAndTenantId(groupDTO.getGroupName(), tenantId)) {
            throw new EntityExistsException(messageService.get(
                    "groups.name.duplicate",
                    "Group name already exists for this tenant"
            ));
        }

        GroupEntity groupEntity = groupMapper.groupDTOToEntity(groupDTO);

        ChurchEntity church = churchRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "groups.church.notFoundForTenant",
                        "Church not found for tenant"
                )));

        groupEntity.setTenantId(tenantId);
        groupEntity.setChurch(church);
        groupEntity.setUsers(new HashSet<>());
        groupEntity.setManagers(new HashSet<>());

        loadUsersForTenant(toNonNullSet(groupDTO.getUsers()), tenantId)
                .forEach(groupEntity::addUser);

        loadUsersForTenant(toNonNullSet(groupDTO.getManagers()), tenantId)
                .forEach(manager -> addManagerAsMember(groupEntity, manager));

        resolveCurrentUserId()
                .flatMap(userRepository::findById)
                .filter(user -> belongsToTenant(user, tenantId))
                .ifPresent(groupEntity::addUser);

        GroupEntity savedGroup = groupRepository.save(groupEntity);

        return groupMapper.groupEntityToResponse(savedGroup);
    }

    @Override
    public Page<GroupResponse> findAll(Pageable pageable) {
        UUID tenantId = requireTenantId();
        return groupRepository.findAllByTenantId(tenantId, pageable)
                .map(groupMapper::groupEntityToResponse);
    }

    @Override
    public Page<GroupResponse> findAllByCreatedBy(UUID createdBy, Pageable pageable) {
        UUID tenantId = requireTenantId();
        return groupRepository.findAllByCreatedByAndTenantId(createdBy, tenantId, pageable)
                .map(groupMapper::groupEntityToResponse);
    }

    @Override
    public Page<GroupResponse> findVisibleForUser(UUID userId, Pageable pageable) {
        UUID tenantId = requireTenantId();
        if (userId == null) {
            return Page.empty(pageable);
        }
        return groupRepository.findVisibleForUser(tenantId, userId, pageable)
                .map(groupMapper::groupEntityToResponse);
    }

    @Override
    public Optional<GroupEntity> findOne(Long groupId) {
        try {
            return Optional.of(loadGroupForTenant(groupId));
        } catch (EntityNotFoundException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<GroupEntity> findOneVisibleForUser(Long groupId, UUID userId) {
        UUID tenantId = requireTenantId();
        if (userId == null) {
            return Optional.empty();
        }
        return groupRepository.findVisibleByIdForUser(tenantId, groupId, userId);
    }

    @Override
    public boolean exists(Long groupId) {
        try {
            loadGroupForTenant(groupId);
            return true;
        } catch (EntityNotFoundException ex) {
            return false;
        }
    }

    @Override
    public GroupResponse updateGroup(Long groupId, GroupDTO request) {
        GroupEntity group = loadGroupForTenant(groupId);
        UUID tenantId = group.getTenantId();

        if (StringUtils.hasText(request.getGroupName()) && !request.getGroupName().equals(group.getGroupName())) {
            if (groupRepository.existsByGroupNameAndTenantId(request.getGroupName(), tenantId)) {
                throw new EntityExistsException(messageService.get(
                        "groups.name.duplicate",
                        "Group name already exists for this tenant"
                ));
            }
            group.setGroupName(request.getGroupName());
        }

        Optional.ofNullable(request.getDescription()).ifPresent(group::setDescription);
        Optional.ofNullable(request.getAvatar()).ifPresent(group::setAvatar);
        Optional.ofNullable(request.getVisibility()).ifPresent(group::setVisibility);

        if (request.getManagers() != null) {
            List<UserEntity> managers = loadUsersForTenant(request.getManagers(), tenantId);
            group.getManagers().clear();
            managers.forEach(manager -> addManagerAsMember(group, manager));
        }

        if (request.getUsers() != null) {
            List<UserEntity> users = loadUsersForTenant(request.getUsers(), tenantId);
            group.getUsers().clear();
            users.forEach(group::addUser);
        }

        group.getManagers().forEach(group::addUser);

        GroupEntity savedGroup = groupRepository.save(group);
        return groupMapper.groupEntityToResponse(savedGroup);
    }

    @Override
    public void delete(Long groupId) {
        GroupEntity group = loadGroupForTenant(groupId);
        groupRepository.delete(group);
    }

    @Transactional
    @Override
    public AddUsersToGroupResponse addUsersToGroup(Long groupId, AddUsersToGroupRequest request) {
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.users.required",
                    USERS_REQUIRED_MESSAGE
            ));
        }

        GroupEntity group = loadGroupForTenant(groupId);
        UUID tenantId = group.getTenantId();

        Set<UUID> requestedUserIds = new LinkedHashSet<>(request.getUserIds());
        Map<UUID, UserEntity> fetchedUsers = userRepository.findAllByUuidIn(requestedUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getUuid, Function.identity()));

        List<UUID> notFoundUserIds = requestedUserIds.stream()
                .filter(id -> !fetchedUsers.containsKey(id))
                .toList();

        List<UUID> tenantMismatchUserIds = fetchedUsers.values().stream()
                .filter(user -> !belongsToTenant(user, tenantId))
                .map(UserEntity::getUuid)
                .toList();

        tenantMismatchUserIds.forEach(fetchedUsers::remove);

        Set<UUID> existingUserIds = group.getUsers().stream()
                .map(UserEntity::getUuid)
                .collect(Collectors.toSet());

        List<UUID> addedIds = new ArrayList<>();
        List<UUID> skippedIds = new ArrayList<>();

        for (UUID userId : requestedUserIds) {
            if (notFoundUserIds.contains(userId) || tenantMismatchUserIds.contains(userId)) {
                continue;
            }

            if (existingUserIds.contains(userId)) {
                skippedIds.add(userId);
                continue;
            }

            UserEntity user = fetchedUsers.get(userId);
            group.addUser(user);
            addedIds.add(userId);
        }

        groupRepository.saveAndFlush(group);

        return AddUsersToGroupResponse.builder()
                .groupName(group.getGroupName())
                .addedCount(addedIds.size())
                .skippedCount(skippedIds.size())
                .notFoundCount(notFoundUserIds.size() + tenantMismatchUserIds.size())
                .addedUserIds(addedIds)
                .skippedUserIds(skippedIds)
                .notFoundUserIds(mergeLists(notFoundUserIds, tenantMismatchUserIds))
                .build();
    }

    @Override
    public RemoveUsersFromGroupResponse removeMembersFromGroup(Long groupId, RemoveUsersFromGroupRequest request) {
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.users.required",
                    USERS_REQUIRED_MESSAGE
            ));
        }

        GroupEntity group = loadGroupForTenant(groupId);
        UUID tenantId = group.getTenantId();

        Set<UUID> requestedUserIds = new LinkedHashSet<>(request.getUserIds());
        Map<UUID, UserEntity> fetchedUsers = userRepository.findAllByUuidIn(new HashSet<>(requestedUserIds)).stream()
                .collect(Collectors.toMap(UserEntity::getUuid, Function.identity()));

        List<UUID> notFoundUserIds = requestedUserIds.stream()
                .filter(id -> !fetchedUsers.containsKey(id))
                .collect(Collectors.toCollection(ArrayList::new));

        List<UUID> tenantMismatchUserIds = fetchedUsers.values().stream()
                .filter(user -> !belongsToTenant(user, tenantId))
                .map(UserEntity::getUuid)
                .collect(Collectors.toCollection(ArrayList::new));

        tenantMismatchUserIds.forEach(fetchedUsers::remove);

        List<UUID> removedUserIds = new ArrayList<>();
        List<UUID> notInGroupUserIds = new ArrayList<>();

        for (UUID userId : requestedUserIds) {
            if (notFoundUserIds.contains(userId) || tenantMismatchUserIds.contains(userId)) {
                continue;
            }

            UserEntity user = fetchedUsers.get(userId);
            if (!group.getUsers().remove(user)) {
                notInGroupUserIds.add(userId);
                continue;
            }
            user.getGroups().remove(group);
            removedUserIds.add(userId);
        }

        groupRepository.save(group);
        userRepository.saveAll(fetchedUsers.values());

        return RemoveUsersFromGroupResponse.builder()
                .groupName(group.getGroupName())
                .removedCount(removedUserIds.size())
                .notInGroupCount(notInGroupUserIds.size())
                .notFoundCount(notFoundUserIds.size() + tenantMismatchUserIds.size())
                .removedUserIds(removedUserIds)
                .notInGroupUserIds(notInGroupUserIds)
                .notFoundUserIds(mergeLists(notFoundUserIds, tenantMismatchUserIds))
                .build();
    }

    @Override
    public AddManagersResponse addManagersToGroup(Long groupId, GroupManagerRequest request) {
        if (request == null || request.getManagerIds() == null || request.getManagerIds().isEmpty()) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.managers.required",
                    "Manager identifiers cannot be empty"
            ));
        }

        GroupEntity group = loadGroupForTenant(groupId);
        UUID tenantId = group.getTenantId();

        Set<UUID> requestedManagerIds = new LinkedHashSet<>(request.getManagerIds());
        Map<UUID, UserEntity> fetchedUsers = userRepository.findAllByUuidIn(requestedManagerIds).stream()
                .collect(Collectors.toMap(UserEntity::getUuid, Function.identity()));

        List<UUID> notFoundManagerIds = requestedManagerIds.stream()
                .filter(id -> !fetchedUsers.containsKey(id))
                .toList();

        List<UUID> tenantMismatchManagerIds = fetchedUsers.values().stream()
                .filter(user -> !belongsToTenant(user, tenantId))
                .map(UserEntity::getUuid)
                .toList();

        tenantMismatchManagerIds.forEach(fetchedUsers::remove);

        Set<UUID> existingManagerIds = group.getManagers().stream()
                .map(UserEntity::getUuid)
                .collect(Collectors.toSet());

        List<UUID> addedManagerIds = new ArrayList<>();
        List<UUID> skippedManagerIds = new ArrayList<>();

        for (UUID managerId : requestedManagerIds) {
            if (notFoundManagerIds.contains(managerId) || tenantMismatchManagerIds.contains(managerId)) {
                continue;
            }

            if (existingManagerIds.contains(managerId)) {
                skippedManagerIds.add(managerId);
                continue;
            }

            UserEntity manager = fetchedUsers.get(managerId);
            addManagerAsMember(group, manager);
            addedManagerIds.add(managerId);
        }

        group.getManagers().forEach(group::addUser);

        groupRepository.save(group);

        return AddManagersResponse.builder()
                .groupName(group.getGroupName())
                .addedCount(addedManagerIds.size())
                .skippedCount(skippedManagerIds.size())
                .notFoundCount(notFoundManagerIds.size() + tenantMismatchManagerIds.size())
                .addedManagerIds(addedManagerIds)
                .skippedManagerIds(skippedManagerIds)
                .notFoundManagerIds(mergeLists(notFoundManagerIds, tenantMismatchManagerIds))
                .build();
    }

    @Override
    public RemoveManagersResponse removeManagersFromGroup(Long groupId, GroupManagerRequest request) {
        if (request == null || request.getManagerIds() == null || request.getManagerIds().isEmpty()) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.managers.required",
                    "Manager identifiers cannot be empty"
            ));
        }

        GroupEntity group = loadGroupForTenant(groupId);
        UUID tenantId = group.getTenantId();

        Set<UUID> requestedManagerIds = new LinkedHashSet<>(request.getManagerIds());
        Map<UUID, UserEntity> fetchedUsers = userRepository.findAllByUuidIn(new HashSet<>(requestedManagerIds)).stream()
                .collect(Collectors.toMap(UserEntity::getUuid, Function.identity()));

        List<UUID> notFoundManagerIds = requestedManagerIds.stream()
                .filter(id -> !fetchedUsers.containsKey(id))
                .collect(Collectors.toCollection(ArrayList::new));

        List<UUID> tenantMismatchManagerIds = fetchedUsers.values().stream()
                .filter(user -> !belongsToTenant(user, tenantId))
                .map(UserEntity::getUuid)
                .collect(Collectors.toCollection(ArrayList::new));

        tenantMismatchManagerIds.forEach(fetchedUsers::remove);

        List<UUID> removedManagerIds = new ArrayList<>();
        List<UUID> notManagerIds = new ArrayList<>();

        for (UUID managerId : requestedManagerIds) {
            if (notFoundManagerIds.contains(managerId) || tenantMismatchManagerIds.contains(managerId)) {
                continue;
            }

            UserEntity manager = fetchedUsers.get(managerId);
            if (!group.getManagers().remove(manager)) {
                notManagerIds.add(managerId);
                continue;
            }
            removedManagerIds.add(managerId);
        }

        groupRepository.save(group);

        return RemoveManagersResponse.builder()
                .groupName(group.getGroupName())
                .removedCount(removedManagerIds.size())
                .notManagersCount(notManagerIds.size())
                .notFoundCount(notFoundManagerIds.size() + tenantMismatchManagerIds.size())
                .removedManagerIds(removedManagerIds)
                .notManagerIds(notManagerIds)
                .notFoundManagerIds(mergeLists(notFoundManagerIds, tenantMismatchManagerIds))
                .build();
    }

    @Override
    public Page<SimpleUserDTO> listGroupMembers(Long groupId, Pageable pageable) {
        loadGroupForTenant(groupId);
        return userRepository.findUsersByGroupId(groupId, pageable);
    }

//    @Cacheable(value = "group_managers", key = "#root.target.groupManagersCacheKey(#groupId)")
    @Override
    public List<SimpleUserDTO> getGroupManagers(Long groupId) {
        GroupEntity group = loadGroupForTenant(groupId);
        return group.getManagers().stream()
                .map(manager -> SimpleUserDTO.builder()
                        .uuid(manager.getUuid())
                        .fullName(manager.getFullName())
                        .email(manager.getEmail())
                        .build())
                .toList();
    }

//    @Cacheable(value = "group_user_status", key = "#root.target.groupUserStatusCacheKey(#groupId)")
    @Override
    public List<GroupUserCandidateDTO> getGroupUserStatus(Long groupId) {
        GroupEntity group = loadGroupForTenant(groupId);

        Long churchId = group.getChurch().getChurchId();

        List<SimpleUserDTO> simpleUsers = userRepository.findSimpleUsersByChurchId(churchId);

        Set<UUID> usersAlreadyInGroup = group.getUsers().stream()
                .map(UserEntity::getUuid)
                .collect(Collectors.toSet());

        return simpleUsers.stream()
                .map(user -> GroupUserCandidateDTO.builder()
                        .uuid(user.uuid())
                        .fullName(user.fullName())
                        .avatarUrl(null)
                        .alreadyInGroup(usersAlreadyInGroup.contains(user.uuid()))
                        .build())
                .toList();
    }

    @Override
    public List<GroupUserCandidateDTO> searchGroupUserCandidates(Long groupId, String query) {
        GroupEntity group = loadGroupForTenant(groupId);
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) {
            return List.of();
        }

        Long churchId = group.getChurch().getChurchId();
        Set<UUID> usersAlreadyInGroup = group.getUsers().stream()
                .map(UserEntity::getUuid)
                .collect(Collectors.toSet());

        return userRepository.searchSimpleUsersByChurchId(churchId, q).stream()
                .map(user -> GroupUserCandidateDTO.builder()
                        .uuid(user.uuid())
                        .fullName(user.fullName())
                        .avatarUrl(null)
                        .alreadyInGroup(usersAlreadyInGroup.contains(user.uuid()))
                        .build())
                .toList();
    }

    @Override
    public boolean canManageGroup(Long groupId, UUID userId) {
        if (userId == null) {
            return false;
        }

        GroupEntity group = loadGroupForTenant(groupId);
        if (userId.equals(group.getCreatedBy())) {
            return true;
        }

        return group.getManagers().stream()
                .map(UserEntity::getUuid)
                .anyMatch(userId::equals);
    }

    @Override
    public GroupJoinRequestResponse submitJoinRequest(Long groupId) {
        UUID tenantId = requireTenantId();
        UUID currentUserId = requireCurrentUserId();
        GroupEntity group = loadGroupForTenant(groupId);

        if (!"PUBLIC_REQUEST_TO_JOIN".equalsIgnoreCase(group.getVisibility())) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.joinRequests.publicOnly",
                    "Join requests are only allowed for public groups."
            ));
        }

        UserEntity requester = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "user.notFound",
                        "User not found"
                )));

        if (!belongsToTenant(requester, tenantId)) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.user.tenantMismatch",
                    "User does not belong to this tenant"
            ));
        }

        if (group.getUsers().stream().anyMatch(user -> currentUserId.equals(user.getUuid()))
                || group.getManagers().stream().anyMatch(user -> currentUserId.equals(user.getUuid()))
                || currentUserId.equals(group.getCreatedBy())) {
            throw new EntityExistsException(messageService.get(
                    "groups.joinRequests.alreadyMember",
                    "You are already part of this group."
            ));
        }

        Optional<GroupJoinRequestEntity> existingPending = groupJoinRequestRepository
                .findFirstByGroup_GroupIdAndRequester_UuidAndStatusInOrderByCreatedAtDesc(
                        groupId,
                        currentUserId,
                        List.of(GroupJoinRequestStatus.PENDING)
                );
        if (existingPending.isPresent()) {
            throw new EntityExistsException(messageService.get(
                    "groups.joinRequests.alreadyPending",
                    "A join request is already pending for this group."
            ));
        }

        GroupJoinRequestEntity joinRequest = GroupJoinRequestEntity.builder()
                .tenantId(tenantId)
                .group(group)
                .requester(requester)
                .status(GroupJoinRequestStatus.PENDING)
                .build();

        GroupJoinRequestEntity saved = groupJoinRequestRepository.save(joinRequest);
        notifyManagersOfJoinRequest(group, requester);
        return toJoinRequestResponse(saved);
    }

    @Override
    public List<GroupJoinRequestResponse> listJoinRequests(Long groupId) {
        loadGroupForTenant(groupId);
        return groupJoinRequestRepository.findByGroup_GroupIdAndStatusOrderByCreatedAtAsc(groupId, GroupJoinRequestStatus.PENDING)
                .stream()
                .map(this::toJoinRequestResponse)
                .toList();
    }

    @Override
    public Optional<MyGroupJoinRequestResponse> getMyJoinRequestStatus(Long groupId) {
        loadGroupForTenant(groupId);
        UUID currentUserId = requireCurrentUserId();
        return groupJoinRequestRepository.findFirstByGroup_GroupIdAndRequester_UuidOrderByCreatedAtDesc(groupId, currentUserId)
                .map(this::toMyJoinRequestResponse);
    }

    @Override
    public List<MyGroupJoinRequestResponse> listMyPendingJoinRequests() {
        UUID tenantId = requireTenantId();
        UUID currentUserId = requireCurrentUserId();
        return groupJoinRequestRepository.findByRequester_UuidAndTenantIdAndStatusOrderByCreatedAtDesc(
                        currentUserId,
                        tenantId,
                        GroupJoinRequestStatus.PENDING
                ).stream()
                .map(this::toMyJoinRequestResponse)
                .toList();
    }

    @Override
    public MyGroupJoinRequestResponse cancelMyJoinRequest(Long groupId) {
        loadGroupForTenant(groupId);
        UUID currentUserId = requireCurrentUserId();
        GroupJoinRequestEntity joinRequest = groupJoinRequestRepository
                .findFirstByGroup_GroupIdAndRequester_UuidAndStatusInOrderByCreatedAtDesc(
                        groupId,
                        currentUserId,
                        List.of(GroupJoinRequestStatus.PENDING)
                )
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "groups.joinRequests.pending.notFound",
                        "Pending join request not found"
                )));

        joinRequest.setStatus(GroupJoinRequestStatus.CANCELLED);
        joinRequest.setDecidedAt(Instant.now());
        joinRequest.setDecidedBy(currentUserId);

        return toMyJoinRequestResponse(groupJoinRequestRepository.save(joinRequest));
    }

    @Override
    public GroupJoinRequestResponse approveJoinRequest(Long groupId, Long requestId, GroupJoinRequestDecisionRequest request) {
        GroupJoinRequestEntity joinRequest = loadJoinRequest(groupId, requestId);
        if (joinRequest.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.joinRequests.approve.pendingOnly",
                    "Only pending join requests can be approved."
            ));
        }

        GroupEntity group = joinRequest.getGroup();
        group.addUser(joinRequest.getRequester());

        joinRequest.setStatus(GroupJoinRequestStatus.APPROVED);
        joinRequest.setDecisionNote(normalizeDecisionNote(request));
        joinRequest.setDecidedAt(Instant.now());
        joinRequest.setDecidedBy(requireCurrentUserId());

        groupRepository.save(group);
        GroupJoinRequestEntity saved = groupJoinRequestRepository.save(joinRequest);
        notifyRequesterOfDecision(saved, true);
        return toJoinRequestResponse(saved);
    }

    @Override
    public GroupJoinRequestResponse rejectJoinRequest(Long groupId, Long requestId, GroupJoinRequestDecisionRequest request) {
        GroupJoinRequestEntity joinRequest = loadJoinRequest(groupId, requestId);
        if (joinRequest.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.joinRequests.reject.pendingOnly",
                    "Only pending join requests can be rejected."
            ));
        }

        joinRequest.setStatus(GroupJoinRequestStatus.REJECTED);
        joinRequest.setDecisionNote(normalizeDecisionNote(request));
        joinRequest.setDecidedAt(Instant.now());
        joinRequest.setDecidedBy(requireCurrentUserId());

        GroupJoinRequestEntity saved = groupJoinRequestRepository.save(joinRequest);
        notifyRequesterOfDecision(saved, false);
        return toJoinRequestResponse(saved);
    }

    /**
     * Batch invite users to a group based on their email addresses.
     * @param groupId The ID of the group to which users will be invited.
     * @param request The batch invite request containing email addresses.
     * @return A response summarizing the invitation results.
     */
    @Override
    @Transactional
    public BatchInviteResponse batchInviteUsersToGroup(Long groupId, BatchInviteRequest request) {
        if (request.getGroupEmails() == null || request.getGroupEmails().isEmpty()) {
            throw new IllegalArgumentException(messageService.get(
                    "groups.batchInvite.emails.required",
                    "Email list cannot be empty"
            ));
        }

        GroupEntity group = loadGroupForTenant(groupId);
        UUID tenantId = group.getTenantId();

        Set<String> emailSet = request.getGroupEmails().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UserEntity> usersToInvite = userRepository.findAllByEmailIn(emailSet);

        Set<String> foundEmails = usersToInvite.stream()
                .map(user -> user.getEmail().toLowerCase())
                .collect(Collectors.toSet());

        List<String> normalizedEmails = emailSet.stream()
                .map(String::toLowerCase)
                .toList();

        List<String> notFoundEmails = normalizedEmails.stream()
                .filter(email -> !foundEmails.contains(email))
                .map(email -> email)
                .toList();

        List<String> tenantMismatchEmails = usersToInvite.stream()
                .filter(user -> !belongsToTenant(user, tenantId))
                .map(user -> user.getEmail().toLowerCase())
                .toList();

        Set<UUID> existingUserIds = group.getUsers().stream()
                .map(UserEntity::getUuid)
                .collect(Collectors.toSet());

        Set<UUID> managerIds = group.getManagers().stream()
                .map(UserEntity::getUuid)
                .collect(Collectors.toSet());

        List<UUID> invitedUserIds = new ArrayList<>();
        List<String> skippedEmails = new ArrayList<>();

        for (UserEntity user : usersToInvite) {
            String normalizedEmail = user.getEmail().toLowerCase();
            if (tenantMismatchEmails.contains(normalizedEmail)) {
                continue;
            }

            if (existingUserIds.contains(user.getUuid()) || managerIds.contains(user.getUuid())) {
                skippedEmails.add(normalizedEmail);
                continue;
            }

            group.addUser(user);
            invitedUserIds.add(user.getUuid());
        }

        groupRepository.saveAndFlush(group);

        return BatchInviteResponse.builder()
                .groupName(group.getGroupName())
                .invitedCount(invitedUserIds.size())
                .skippedCount(skippedEmails.size())
                .notFoundCount(notFoundEmails.size() + tenantMismatchEmails.size())
                .invitedUserIds(invitedUserIds)
                .skippedEmails(skippedEmails)
                .notFoundEmails(mergeLists(notFoundEmails, tenantMismatchEmails))
                .build();
    }

    private String groupCacheKey(Long groupId) {
        return String.valueOf(TenantContext.getTenantId()) + ":group:" + groupId;
    }

    private String groupManagersCacheKey(Long groupId) {
        return String.valueOf(TenantContext.getTenantId()) + ":group-managers:" + groupId;
    }

    private String groupUserStatusCacheKey(Long groupId) {
        return String.valueOf(TenantContext.getTenantId()) + ":group-user-status:" + groupId;
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "tenant.context.missing",
                    "Tenant ID not found in context"
            ));
        }
        return tenantId;
    }

    private UUID requireCurrentUserId() {
        return resolveCurrentUserId()
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "auth.user.notAuthenticated",
                        "Current user not found in security context"
                )));
    }

    private GroupEntity loadGroupForTenant(Long groupId) {
        UUID tenantId = requireTenantId();
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "groups.notFound",
                        GROUP_NOT_FOUND
                )));

        if (!tenantId.equals(group.getTenantId())) {
            throw new EntityNotFoundException(messageService.get(
                    "groups.notFound",
                    GROUP_NOT_FOUND
            ));
        }

        return group;
    }

    private List<UserEntity> loadUsersForTenant(Set<UUID> userIds, UUID tenantId) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserEntity> users = userRepository.findAllByUuidIn(userIds);
        Map<UUID, UserEntity> userMap = users.stream()
                .collect(Collectors.toMap(UserEntity::getUuid, Function.identity()));

        List<UUID> missing = userIds.stream()
                .filter(id -> !userMap.containsKey(id))
                .toList();

        if (!missing.isEmpty()) {
            throw new EntityNotFoundException(messageService.get(
                    "groups.users.notFound",
                    "Users not found: {0}",
                    missing
            ));
        }

        users.forEach(user -> {
            if (!belongsToTenant(user, tenantId)) {
                throw new IllegalArgumentException(messageService.get(
                        "groups.user.tenantMismatch.withId",
                        "User {0} does not belong to this tenant",
                        user.getUuid()
                ));
            }
        });

        return users;
    }

    private Set<UUID> toNonNullSet(Set<UUID> source) {
        return source == null ? Collections.emptySet() : source;
    }

    private void addManagerAsMember(GroupEntity group, UserEntity manager) {
        if (group == null || manager == null) {
            return;
        }
        group.getManagers().add(manager);
        group.addUser(manager);
    }

    private <T> List<T> mergeLists(List<T> first, List<T> second) {
        List<T> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged;
    }

    private Optional<UUID> resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return Optional.ofNullable(userPrincipal.getUserUuid());
        }

        return Optional.empty();
    }

    private GroupJoinRequestEntity loadJoinRequest(Long groupId, Long requestId) {
        GroupJoinRequestEntity joinRequest = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "groups.joinRequests.notFound",
                        "Join request not found"
                )));
        GroupEntity group = loadGroupForTenant(groupId);
        if (!joinRequest.getGroup().getGroupId().equals(group.getGroupId())) {
            throw new EntityNotFoundException(messageService.get(
                    "groups.joinRequests.notFound",
                    "Join request not found"
            ));
        }
        return joinRequest;
    }

    private String normalizeDecisionNote(GroupJoinRequestDecisionRequest request) {
        if (request == null || request.getNote() == null) {
            return null;
        }
        String note = request.getNote().trim();
        return note.isEmpty() ? null : note;
    }

    private GroupJoinRequestResponse toJoinRequestResponse(GroupJoinRequestEntity joinRequest) {
        return GroupJoinRequestResponse.builder()
                .id(joinRequest.getId())
                .groupId(joinRequest.getGroup().getGroupId())
                .requesterId(joinRequest.getRequester().getUuid())
                .requesterName(joinRequest.getRequester().getFullName())
                .requesterEmail(joinRequest.getRequester().getEmail())
                .status(joinRequest.getStatus().name())
                .decisionNote(joinRequest.getDecisionNote())
                .requestedAt(joinRequest.getCreatedDate())
                .decidedAt(joinRequest.getDecidedAt() == null ? null : LocalDateTime.ofInstant(joinRequest.getDecidedAt(), java.time.ZoneId.systemDefault()))
                .decidedBy(joinRequest.getDecidedBy())
                .build();
    }

    private MyGroupJoinRequestResponse toMyJoinRequestResponse(GroupJoinRequestEntity joinRequest) {
        return MyGroupJoinRequestResponse.builder()
                .groupId(joinRequest.getGroup().getGroupId())
                .requestId(joinRequest.getId())
                .status(joinRequest.getStatus().name())
                .build();
    }

    private void notifyManagersOfJoinRequest(GroupEntity group, UserEntity requester) {
        Set<UserEntity> recipients = new LinkedHashSet<>(group.getManagers());
        if (group.getCreatedBy() != null) {
            userRepository.findById(group.getCreatedBy()).ifPresent(recipients::add);
        }

        for (UserEntity recipient : recipients) {
            if (recipient == null || recipient.getUuid() == null || recipient.getUuid().equals(requester.getUuid())) {
                continue;
            }
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationType.NOTIFICATION,
                    recipient,
                    Map.of(
                            "title", messageService.get(
                                    "groups.notifications.joinRequest.title",
                                    "New group join request"
                            ),
                            "message_content", messageService.get(
                                    "groups.notifications.joinRequest.message",
                                    "{0} requested to join {1}.",
                                    requester.getFullName(),
                                    group.getGroupName()
                            )
                    ),
                    java.util.EnumSet.of(NotificationChannelType.IN_APP)
            ));
        }
    }

    private void notifyRequesterOfDecision(GroupJoinRequestEntity joinRequest, boolean approved) {
        UserEntity requester = joinRequest.getRequester();
        if (requester == null) {
            return;
        }

        String verb = approved ? "approved" : "rejected";
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                NotificationType.NOTIFICATION,
                requester,
                Map.of(
                        "title", messageService.get(
                                approved ? "groups.notifications.joinRequest.approved.title"
                                        : "groups.notifications.joinRequest.rejected.title",
                                approved ? "Group join request approved" : "Group join request rejected"
                        ),
                        "message_content", messageService.get(
                                approved ? "groups.notifications.joinRequest.approved.message"
                                        : "groups.notifications.joinRequest.rejected.message",
                                approved
                                        ? "Your request to join {0} was approved."
                                        : "Your request to join {0} was rejected.",
                                joinRequest.getGroup().getGroupName()
                        )
                ),
                java.util.EnumSet.of(NotificationChannelType.IN_APP)
        ));
    }

    private boolean belongsToTenant(UserEntity user, UUID tenantId) {
        UUID userTenantId = user.getTenantId();
        if (userTenantId == null && user.getTenant() != null) {
            userTenantId = user.getTenant().getId();
        }
        return userTenantId != null && userTenantId.equals(tenantId);
    }
}
