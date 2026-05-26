package com.anastasia.Anastasia_BackEnd.UnitTests.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupJoinRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupJoinRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupJoinRequestStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.GroupMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.service.GroupServiceImpl;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@LenientMockitoTest
class GroupServiceUnitTest {

    @Mock
    private GroupMapper groupMapper;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupJoinRequestRepository groupJoinRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChurchRepository churchRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private LocalizedMessageService messageService;

    @InjectMocks
    private GroupServiceImpl groupService;

    private UUID tenantId;
    private GroupEntity existingGroup;
    private ChurchEntity church;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        church = ChurchEntity.builder().churchId(42L).build();

        existingGroup = GroupEntity.builder()
                .groupId(7L)
                .groupName("Existing Group")
                .visibility("PRIVATE")
                .tenantId(tenantId)
                .church(church)
                .users(new HashSet<>())
                .managers(new HashSet<>())
                .build();
        lenient().when(messageService.get(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(messageService.get(anyString(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createGroup_assignsManagersSeparatelyFromMembers() {
        UUID userId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        GroupDTO groupDTO = GroupDTO.builder()
                .groupName("New Group")
                .visibility("PRIVATE")
                .users(Set.of(userId))
                .managers(Set.of(managerId))
                .build();

        GroupEntity mappedEntity = GroupEntity.builder()
                .groupName("New Group")
                .visibility("PRIVATE")
                .users(new HashSet<>())
                .managers(new HashSet<>())
                .build();

        UserEntity user = userEntity(userId, tenantId);
        UserEntity manager = userEntity(managerId, tenantId);
        when(groupRepository.existsByGroupNameAndTenantId("New Group", tenantId)).thenReturn(false);
        when(groupMapper.groupDTOToEntity(groupDTO)).thenReturn(mappedEntity);
        when(churchRepository.findByTenantId(tenantId)).thenReturn(Optional.of(church));
        when(userRepository.findAllByUuidIn(Set.of(userId))).thenReturn(List.of(user));
        when(userRepository.findAllByUuidIn(Set.of(managerId))).thenReturn(List.of(manager));
        when(groupRepository.save(any(GroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMapper.groupEntityToResponse(any(GroupEntity.class)))
                .thenReturn(GroupResponse.builder().groupId(1L).groupName("New Group").build());

        GroupResponse result = groupService.createGroup(groupDTO);

        assertThat(result.getGroupName()).isEqualTo("New Group");
        ArgumentCaptor<GroupEntity> captor = ArgumentCaptor.forClass(GroupEntity.class);
        verify(groupRepository).save(captor.capture());
        GroupEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getUsers()).containsExactly(user);
        assertThat(saved.getManagers()).contains(manager);
    }

    @Test
    void createGroup_withoutTenant_throwsIllegalState() {
        TenantContext.clear();

        GroupDTO dto = GroupDTO.builder().groupName("No Tenant").visibility("PRIVATE").build();

        assertThatThrownBy(() -> groupService.createGroup(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant ID not found");
    }

    @Test
    void addUsersToGroup_returnsDetailedResponse() {
        UUID existingUserId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();
        UUID missingUserId = UUID.randomUUID();
        UUID foreignUserId = UUID.randomUUID();

        UserEntity existingUser = userEntity(existingUserId, tenantId);
        existingGroup.addUser(existingUser);

        UserEntity newUser = userEntity(newUserId, tenantId);
        UserEntity foreignUser = userEntity(foreignUserId, UUID.randomUUID());

        AddUsersToGroupRequest request = AddUsersToGroupRequest.builder()
                .userIds(Set.of(existingUserId, newUserId, missingUserId, foreignUserId))
                .build();

        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(userRepository.findAllByUuidIn(anySet()))
                .thenReturn(List.of(existingUser, newUser, foreignUser));

        AddUsersToGroupResponse response = groupService.addUsersToGroup(existingGroup.getGroupId(), request);

        assertThat(response.getAddedUserIds()).containsExactly(newUserId);
        assertThat(response.getSkippedUserIds()).containsExactly(existingUserId);
        assertThat(response.getNotFoundUserIds()).containsExactlyInAnyOrder(missingUserId, foreignUserId);
        verify(groupRepository).saveAndFlush(existingGroup);
    }

    @Test
    void removeMembersFromGroup_returnsDetailedResponse() {
        UUID inGroupId = UUID.randomUUID();
        UUID notMemberId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        UserEntity inGroupUser = userEntity(inGroupId, tenantId);
        existingGroup.addUser(inGroupUser);

        UserEntity notMemberUser = userEntity(notMemberId, tenantId);

        RemoveUsersFromGroupRequest request = RemoveUsersFromGroupRequest.builder()
                .userIds(List.of(inGroupId, notMemberId, missingId))
                .build();

        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(userRepository.findAllByUuidIn(anySet()))
                .thenReturn(List.of(inGroupUser, notMemberUser));

        RemoveUsersFromGroupResponse response = groupService.removeMembersFromGroup(existingGroup.getGroupId(), request);

        assertThat(response.getRemovedUserIds()).containsExactly(inGroupId);
        assertThat(response.getNotInGroupUserIds()).containsExactly(notMemberId);
        assertThat(response.getNotFoundUserIds()).containsExactly(missingId);
        verify(groupRepository).save(existingGroup);
        verify(userRepository).saveAll(anyCollection());
    }

    @Test
    void addManagersToGroup_returnsDetailedResponse() {
        UUID managerId = UUID.randomUUID();
        UUID existingManagerId = UUID.randomUUID();
        UUID missingManagerId = UUID.randomUUID();

        UserEntity existingManager = userEntity(existingManagerId, tenantId);
        existingGroup.getManagers().add(existingManager);

        UserEntity newManager = userEntity(managerId, tenantId);

        GroupManagerRequest request = GroupManagerRequest.builder()
                .managerIds(Set.of(managerId, existingManagerId, missingManagerId))
                .build();

        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(userRepository.findAllByUuidIn(anySet()))
                .thenReturn(List.of(existingManager, newManager));

        AddManagersResponse response = groupService.addManagersToGroup(existingGroup.getGroupId(), request);

        assertThat(response.getAddedManagerIds()).containsExactly(managerId);
        assertThat(response.getSkippedManagerIds()).containsExactly(existingManagerId);
        assertThat(response.getNotFoundManagerIds()).containsExactly(missingManagerId);
        assertThat(existingGroup.getManagers()).contains(existingManager, newManager);
        verify(groupRepository).save(existingGroup);
    }

    @Test
    void removeManagersFromGroup_returnsDetailedResponse() {
        UUID managerId = UUID.randomUUID();
        UUID notManagerId = UUID.randomUUID();

        UserEntity manager = userEntity(managerId, tenantId);
        existingGroup.getManagers().add(manager);

        UserEntity notManager = userEntity(notManagerId, tenantId);

        GroupManagerRequest request = GroupManagerRequest.builder()
                .managerIds(Set.of(managerId, notManagerId))
                .build();

        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(userRepository.findAllByUuidIn(anySet()))
                .thenReturn(List.of(manager, notManager));

        RemoveManagersResponse response = groupService.removeManagersFromGroup(existingGroup.getGroupId(), request);

        assertThat(response.getRemovedManagerIds()).containsExactly(managerId);
        assertThat(response.getNotManagerIds()).containsExactly(notManagerId);
        verify(groupRepository).save(existingGroup);
    }

    @Test
    void batchInviteUsersToGroup_returnsCategorisedResponse() {
        existingGroup.getManagers().add(userEntity(UUID.randomUUID(), tenantId));

        UserEntity alreadyMember = userEntity(UUID.randomUUID(), tenantId);
        alreadyMember.setEmail("member@example.com");
        existingGroup.addUser(alreadyMember);

        UserEntity invitee = userEntity(UUID.randomUUID(), tenantId);
        invitee.setEmail("invitee@example.com");

        UserEntity foreignUser = userEntity(UUID.randomUUID(), UUID.randomUUID());
        foreignUser.setEmail("foreign@example.com");

        BatchInviteRequest request = BatchInviteRequest.builder()
                .groupEmails(new LinkedHashSet<>(Arrays.asList(
                        "invitee@example.com",
                        "member@example.com",
                        "missing@example.com",
                        "foreign@example.com")))
                .build();

        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(userRepository.findAllByEmailIn(anySet()))
                .thenReturn(List.of(alreadyMember, invitee, foreignUser));

        BatchInviteResponse response = groupService.batchInviteUsersToGroup(existingGroup.getGroupId(), request);

        assertThat(response.getInvitedUserIds()).containsExactly(invitee.getUuid());
        assertThat(response.getSkippedEmails()).containsExactly("member@example.com");
        assertThat(response.getNotFoundEmails()).containsExactlyInAnyOrder("missing@example.com", "foreign@example.com");
        verify(groupRepository).saveAndFlush(existingGroup);
    }

    @Test
    void searchGroupUserCandidates_returnsReducedMatchesWithExistingState() {
        UUID existingMemberId = UUID.randomUUID();
        UserEntity existingMember = userEntity(existingMemberId, tenantId);
        existingGroup.addUser(existingMember);

        SimpleUserDTO existingCandidate = SimpleUserDTO.builder()
                .uuid(existingMemberId)
                .fullName("Existing Member")
                .email("existing@example.com")
                .build();
        SimpleUserDTO newCandidate = SimpleUserDTO.builder()
                .uuid(UUID.randomUUID())
                .fullName("New Member")
                .email("new@example.com")
                .build();

        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(userRepository.searchSimpleUsersByChurchId(church.getChurchId(), "member"))
                .thenReturn(List.of(existingCandidate, newCandidate));

        List<GroupUserCandidateDTO> response =
                groupService.searchGroupUserCandidates(existingGroup.getGroupId(), "member");

        assertThat(response).hasSize(2);
        assertThat(response).anySatisfy(candidate -> {
            assertThat(candidate.getUuid()).isEqualTo(existingMemberId);
            assertThat(candidate.isAlreadyInGroup()).isTrue();
        });
        assertThat(response).anySatisfy(candidate -> {
            assertThat(candidate.getUuid()).isEqualTo(newCandidate.uuid());
            assertThat(candidate.isAlreadyInGroup()).isFalse();
        });
    }

    @Test
    void submitJoinRequest_createsPendingRequestForPublicGroup() {
        UUID requesterId = UUID.randomUUID();
        UserEntity requester = userEntity(requesterId, tenantId);
        existingGroup.setVisibility("PUBLIC_REQUEST_TO_JOIN");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(requester), null, List.of())
        );

        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(groupJoinRequestRepository.findFirstByGroup_GroupIdAndRequester_UuidAndStatusInOrderByCreatedAtDesc(
                eq(existingGroup.getGroupId()),
                eq(requesterId),
                anyCollection()
        )).thenReturn(Optional.empty());
        when(groupJoinRequestRepository.save(any(GroupJoinRequestEntity.class))).thenAnswer(invocation -> {
            GroupJoinRequestEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            entity.setCreatedAt(java.time.Instant.now());
            return entity;
        });

        GroupJoinRequestResponse response = groupService.submitJoinRequest(existingGroup.getGroupId());

        assertThat(response.getGroupId()).isEqualTo(existingGroup.getGroupId());
        assertThat(response.getRequesterId()).isEqualTo(requesterId);
        assertThat(response.getStatus()).isEqualTo(GroupJoinRequestStatus.PENDING.name());
    }

    @Test
    void approveJoinRequest_addsRequesterToGroup() {
        UUID managerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UserEntity manager = userEntity(managerId, tenantId);
        UserEntity requester = userEntity(requesterId, tenantId);
        existingGroup.getManagers().add(manager);

        GroupJoinRequestEntity joinRequest = GroupJoinRequestEntity.builder()
                .id(55L)
                .tenantId(tenantId)
                .group(existingGroup)
                .requester(requester)
                .status(GroupJoinRequestStatus.PENDING)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(manager), null, List.of())
        );

        when(groupJoinRequestRepository.findById(55L)).thenReturn(Optional.of(joinRequest));
        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(groupRepository.save(existingGroup)).thenReturn(existingGroup);
        when(groupJoinRequestRepository.save(joinRequest)).thenReturn(joinRequest);

        GroupJoinRequestResponse response = groupService.approveJoinRequest(
                existingGroup.getGroupId(),
                55L,
                GroupJoinRequestDecisionRequest.builder().note("Approved").build()
        );

        assertThat(response.getStatus()).isEqualTo(GroupJoinRequestStatus.APPROVED.name());
        assertThat(existingGroup.getUsers()).contains(requester);
        assertThat(joinRequest.getDecisionNote()).isEqualTo("Approved");
    }

    @Test
    void cancelMyJoinRequest_marksPendingRequestAsCancelled() {
        UUID requesterId = UUID.randomUUID();
        UserEntity requester = userEntity(requesterId, tenantId);
        GroupJoinRequestEntity joinRequest = GroupJoinRequestEntity.builder()
                .id(77L)
                .tenantId(tenantId)
                .group(existingGroup)
                .requester(requester)
                .status(GroupJoinRequestStatus.PENDING)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(requester), null, List.of())
        );

        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        when(groupJoinRequestRepository.findFirstByGroup_GroupIdAndRequester_UuidAndStatusInOrderByCreatedAtDesc(
                eq(existingGroup.getGroupId()),
                eq(requesterId),
                anyCollection()
        )).thenReturn(Optional.of(joinRequest));
        when(groupJoinRequestRepository.save(joinRequest)).thenReturn(joinRequest);

        MyGroupJoinRequestResponse response = groupService.cancelMyJoinRequest(existingGroup.getGroupId());

        assertThat(response.getStatus()).isEqualTo(GroupJoinRequestStatus.CANCELLED.name());
        assertThat(joinRequest.getStatus()).isEqualTo(GroupJoinRequestStatus.CANCELLED);
    }

    @Test
    void listGroupMembers_requiresExistingGroup() {
        when(groupRepository.findById(existingGroup.getGroupId())).thenReturn(Optional.of(existingGroup));
        Page<SimpleUserDTO> expectedPage = new PageImpl<>(List.of());
        when(userRepository.findUsersByGroupId(eq(existingGroup.getGroupId()), any(Pageable.class))).thenReturn(expectedPage);

        Page<SimpleUserDTO> result = groupService.listGroupMembers(existingGroup.getGroupId(), Pageable.unpaged());

        assertThat(result).isSameAs(expectedPage);
    }

    @Test
    void createGroup_duplicateNameThrows() {
        GroupDTO dto = GroupDTO.builder().groupName("Dup").visibility("PRIVATE").build();
        when(groupRepository.existsByGroupNameAndTenantId("Dup", tenantId)).thenReturn(true);

        assertThatThrownBy(() -> groupService.createGroup(dto))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("Group name already exists");
    }

    @Test
    void loadGroupForDifferentTenant_throwsNotFound() {
        GroupEntity otherTenantGroup = GroupEntity.builder()
                .groupId(88L)
                .tenantId(UUID.randomUUID())
                .build();

        when(groupRepository.findById(88L)).thenReturn(Optional.of(otherTenantGroup));

        assertThatThrownBy(() -> groupService.getGroupManagers(88L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findVisibleForUser_delegatesToTenantScopedRepositoryQuery() {
        UUID userId = UUID.randomUUID();
        when(groupRepository.findVisibleForUser(eq(tenantId), eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existingGroup)));
        when(groupMapper.groupEntityToResponse(existingGroup))
                .thenReturn(GroupResponse.builder().groupId(existingGroup.getGroupId()).build());

        Page<GroupResponse> result = groupService.findVisibleForUser(userId, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(groupRepository).findVisibleForUser(eq(tenantId), eq(userId), any(Pageable.class));
    }

    @Test
    void findOneVisibleForUser_returnsEmptyWhenRepositoryFindsNone() {
        UUID userId = UUID.randomUUID();
        when(groupRepository.findVisibleByIdForUser(tenantId, existingGroup.getGroupId(), userId))
                .thenReturn(Optional.empty());

        Optional<GroupEntity> result = groupService.findOneVisibleForUser(existingGroup.getGroupId(), userId);

        assertThat(result).isEmpty();
    }

    @Test
    void readPageMethods_areTransactionalReadOnly() throws NoSuchMethodException {
        assertTransactionalReadOnly("findAll", Pageable.class);
        assertTransactionalReadOnly("findAllByCreatedBy", UUID.class, Pageable.class);
        assertTransactionalReadOnly("findVisibleForUser", UUID.class, Pageable.class);
    }

    private UserEntity userEntity(UUID userId, UUID tenantId) {
        return UserEntity.builder()
                .uuid(userId)
                .affiliatedTenantId(tenantId)
                .email(userId + "@example.com")
                .groups(new HashSet<>())
                .build();
    }

    private void assertTransactionalReadOnly(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = GroupServiceImpl.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional)
                .as("%s should be transactional", methodName)
                .isNotNull();
        assertThat(transactional.readOnly())
                .as("%s should keep the Hibernate session open only for reads", methodName)
                .isTrue();
    }
}
