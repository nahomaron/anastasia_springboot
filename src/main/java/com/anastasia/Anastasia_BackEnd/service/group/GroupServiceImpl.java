package com.anastasia.Anastasia_BackEnd.service.group;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.mappers.GroupMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.GroupRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final UserRepository userRepository;
    private final ChurchRepository churchRepository;

    @Override
    public GroupEntity convertToEntity(GroupDTO groupDTO) {
        return groupMapper.groupDTOToEntity(groupDTO);
    }

    @Override
    public GroupDTO convertToDTO(GroupEntity groupEntity) {
        return groupMapper.groupEntityToDTO(groupEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "groups_all", allEntries = true),
            @CacheEvict(value = "groups", allEntries = true),
            @CacheEvict(value = "group_managers", allEntries = true),
            @CacheEvict(value = "group_user_status", allEntries = true)
    })
    @Override
    public SimpleGroupEntity createGroup(GroupDTO groupDTO) {
        UUID tenantId = requireTenantId();

        if (groupRepository.existsByGroupNameAndTenantId(groupDTO.getGroupName(), tenantId)) {
            throw new EntityExistsException("Group name already exists for this tenant");
        }

        GroupEntity groupEntity = groupMapper.groupDTOToEntity(groupDTO);

        ChurchEntity church = churchRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Church not found for tenant"));

        groupEntity.setTenantId(tenantId);
        groupEntity.setChurch(church);
        groupEntity.setUsers(new HashSet<>());
        groupEntity.setManagers(new HashSet<>());

        loadUsersForTenant(toNonNullSet(groupDTO.getUsers()), tenantId)
                .forEach(groupEntity::addUser);

        groupEntity.getManagers().addAll(loadUsersForTenant(toNonNullSet(groupDTO.getManagers()), tenantId));

        GroupEntity savedGroup = groupRepository.save(groupEntity);

        return SimpleGroupEntity.builder()
                .groupId(savedGroup.getGroupId())
                .groupName(savedGroup.getGroupName())
                .description(savedGroup.getDescription())
                .build();
    }

    @Cacheable(value = "groups_all", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Page<GroupEntity> findAll(Pageable pageable) {
        return groupRepository.findAll(pageable);
    }

    @Cacheable(value = "groups", key = "#root.target.groupCacheKey(#groupId)")
    @Override
    public Optional<GroupEntity> findOne(Long groupId) {
        try {
            return Optional.of(loadGroupForTenant(groupId));
        } catch (EntityNotFoundException ex) {
            return Optional.empty();
        }
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

    @Caching(evict = {
            @CacheEvict(value = "groups", key = "#root.target.groupCacheKey(#groupId)"),
            @CacheEvict(value = "groups_all", allEntries = true),
            @CacheEvict(value = "group_managers", key = "#root.target.groupManagersCacheKey(#groupId)"),
            @CacheEvict(value = "group_user_status", key = "#root.target.groupUserStatusCacheKey(#groupId)")
    })
    @Override
    public void updateGroup(Long groupId, GroupDTO request) {
        GroupEntity group = loadGroupForTenant(groupId);
        UUID tenantId = group.getTenantId();

        if (StringUtils.hasText(request.getGroupName()) && !request.getGroupName().equals(group.getGroupName())) {
            if (groupRepository.existsByGroupNameAndTenantId(request.getGroupName(), tenantId)) {
                throw new EntityExistsException("Group name already exists for this tenant");
            }
            group.setGroupName(request.getGroupName());
        }

        Optional.ofNullable(request.getDescription()).ifPresent(group::setDescription);
        Optional.ofNullable(request.getAvatar()).ifPresent(group::setAvatar);
        Optional.ofNullable(request.getVisibility()).ifPresent(group::setVisibility);

        if (request.getManagers() != null) {
            List<UserEntity> managers = loadUsersForTenant(request.getManagers(), tenantId);
            group.getManagers().clear();
            group.getManagers().addAll(managers);
        }

        if (request.getUsers() != null) {
            List<UserEntity> users = loadUsersForTenant(request.getUsers(), tenantId);
            group.getUsers().clear();
            users.forEach(group::addUser);
        }

        groupRepository.save(group);
    }

    @Caching(evict = {
            @CacheEvict(value = "groups", key = "#root.target.groupCacheKey(#groupId)"),
            @CacheEvict(value = "groups_all", allEntries = true),
            @CacheEvict(value = "group_managers", key = "#root.target.groupManagersCacheKey(#groupId)"),
            @CacheEvict(value = "group_user_status", key = "#root.target.groupUserStatusCacheKey(#groupId)")
    })
    @Override
    public void delete(Long groupId) {
        GroupEntity group = loadGroupForTenant(groupId);
        groupRepository.delete(group);
    }

    @Caching(evict = {
            @CacheEvict(value = "groups", key = "#root.target.groupCacheKey(#groupId)"),
            @CacheEvict(value = "group_user_status", key = "#root.target.groupUserStatusCacheKey(#groupId)")
    })
    @Transactional
    @Override
    public AddUsersToGroupResponse addUsersToGroup(Long groupId, AddUsersToGroupRequest request) {
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException(USERS_REQUIRED_MESSAGE);
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

    @Caching(evict = {
            @CacheEvict(value = "groups", key = "#root.target.groupCacheKey(#groupId)"),
            @CacheEvict(value = "group_user_status", key = "#root.target.groupUserStatusCacheKey(#groupId)")
    })
    @Override
    public RemoveUsersFromGroupResponse removeMembersFromGroup(Long groupId, RemoveUsersFromGroupRequest request) {
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException(USERS_REQUIRED_MESSAGE);
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

    @Caching(evict = {
            @CacheEvict(value = "groups", key = "#root.target.groupCacheKey(#groupId)"),
            @CacheEvict(value = "group_managers", key = "#root.target.groupManagersCacheKey(#groupId)")
    })
    @Override
    public AddManagersResponse addManagersToGroup(Long groupId, GroupManagerRequest request) {
        if (request == null || request.getManagerIds() == null || request.getManagerIds().isEmpty()) {
            throw new IllegalArgumentException("Manager identifiers cannot be empty");
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
            group.getManagers().add(manager);
            addedManagerIds.add(managerId);
        }

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

    @Caching(evict = {
            @CacheEvict(value = "groups", key = "#root.target.groupCacheKey(#groupId)"),
            @CacheEvict(value = "group_managers", key = "#root.target.groupManagersCacheKey(#groupId)")
    })
    @Override
    public RemoveManagersResponse removeManagersFromGroup(Long groupId, GroupManagerRequest request) {
        if (request == null || request.getManagerIds() == null || request.getManagerIds().isEmpty()) {
            throw new IllegalArgumentException("Manager identifiers cannot be empty");
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

    @Cacheable(value = "group_managers", key = "#root.target.groupManagersCacheKey(#groupId)")
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

    @Cacheable(value = "group_user_status", key = "#root.target.groupUserStatusCacheKey(#groupId)")
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

    /**
     * Batch invite users to a group based on their email addresses.
     * @param groupId The ID of the group to which users will be invited.
     * @param request The batch invite request containing email addresses.
     * @return A response summarizing the invitation results.
     */
    @Caching(evict = {
            @CacheEvict(value = "groups", key = "#root.target.groupCacheKey(#groupId)"),
            @CacheEvict(value = "group_user_status", key = "#root.target.groupUserStatusCacheKey(#groupId)")
    })
    @Override
    @Transactional
    public BatchInviteResponse batchInviteUsersToGroup(Long groupId, BatchInviteRequest request) {
        if (request.getGroupEmails() == null || request.getGroupEmails().isEmpty()) {
            throw new IllegalArgumentException("Email list cannot be empty");
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
            throw new IllegalStateException("Tenant ID not found in context");
        }
        return tenantId;
    }

    private GroupEntity loadGroupForTenant(Long groupId) {
        UUID tenantId = requireTenantId();
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(GROUP_NOT_FOUND));

        if (!tenantId.equals(group.getTenantId())) {
            throw new EntityNotFoundException(GROUP_NOT_FOUND);
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
            throw new EntityNotFoundException("Users not found: " + missing);
        }

        users.forEach(user -> {
            if (!belongsToTenant(user, tenantId)) {
                throw new IllegalArgumentException("User " + user.getUuid() + " does not belong to this tenant");
            }
        });

        return users;
    }

    private Set<UUID> toNonNullSet(Set<UUID> source) {
        return source == null ? Collections.emptySet() : source;
    }

    private <T> List<T> mergeLists(List<T> first, List<T> second) {
        List<T> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged;
    }

    private boolean belongsToTenant(UserEntity user, UUID tenantId) {
        UUID userTenantId = user.getTenantId();
        if (userTenantId == null && user.getTenant() != null) {
            userTenantId = user.getTenant().getId();
        }
        return userTenantId != null && userTenantId.equals(tenantId);
    }
}
