package com.anastasia.Anastasia_BackEnd.UnitTests.service;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.mappers.GroupMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.GroupRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.group.GroupServiceImpl;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceUnitTest {

    @Mock
    private GroupMapper groupMapper;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChurchRepository churchRepository;

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
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createGroup_assignsTenantUsersAndManagers() {
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

        SimpleGroupEntity result = groupService.createGroup(groupDTO);

        assertThat(result.getGroupName()).isEqualTo("New Group");
        verify(groupRepository).save(argThat(saved ->
                saved.getTenantId().equals(tenantId)
                        && saved.getUsers().contains(user)
                        && saved.getManagers().contains(manager)));
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

    private UserEntity userEntity(UUID userId, UUID tenantId) {
        return UserEntity.builder()
                .uuid(userId)
                .tenantId(tenantId)
                .email(userId + "@example.com")
                .groups(new HashSet<>())
                .build();
    }
}
