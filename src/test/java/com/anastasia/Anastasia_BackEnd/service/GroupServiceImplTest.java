package com.anastasia.Anastasia_BackEnd.service;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
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
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Unit test for {@link GroupServiceImpl} */

@ExtendWith(MockitoExtension.class)
public class GroupServiceImplTest {

    @Mock private GroupMapper groupMapper;
    @Mock private GroupRepository groupRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChurchRepository churchRepository;

    @InjectMocks GroupServiceImpl groupService;

    private GroupEntity testGroup;

    @BeforeEach
    void setUp() {
        testGroup = GroupEntity.builder()
                .groupId(1L)
                .groupName("Test Group")
                .users(new HashSet<>())
                .managers(new HashSet<>())
                .build();
    }

    @Test
    void testCreateGroup_success() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        GroupDTO groupDTO = GroupDTO.builder()
                .groupName("New Group")
                .users(new HashSet<>())
                .managers(new HashSet<>())
                .build();

        when(groupRepository.existsByGroupName(anyString())).thenReturn(false);
        when(groupMapper.groupDTOToEntity(any())).thenReturn(testGroup);
        when(churchRepository.findByTenantId(any(UUID.class))).thenReturn(Optional.of(new ChurchEntity()));
        when(userRepository.findAllByUuidIn(anySet())).thenReturn(Collections.emptyList());
        when(groupRepository.save(any(GroupEntity.class))).thenReturn(testGroup);

        groupService.createGroup(groupDTO);

        verify(groupRepository).save(any(GroupEntity.class));
    }

    @Test

    void testFindAll_success() {
        Pageable pageable = Pageable.unpaged();
        when(groupRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<GroupEntity> result = groupService.findAll(pageable);

        assertThat(result).isEmpty();
        verify(groupRepository).findAll(pageable);
    }

    @Test
    void testFindOneGroup_success() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Optional<GroupEntity> result = groupService.findOne(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getGroupName()).isEqualTo("Test Group");
        verify(groupRepository, times(1)).findById(1L);
    }

    @Test
    void testFindOneGroup_notFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<GroupEntity> result = groupService.findOne(99L);
        assertThat(result).isEmpty();
        verify(groupRepository, times(1)).findById(99L);
    }


    @Test
    void testExists_true() {
        when(groupRepository.existsById(1L)).thenReturn(true);
        boolean exists = groupService.exists(1L);
        assertThat(exists).isTrue();
        verify(groupRepository, times(1)).existsById(1L);
    }

    @Test
    void testUpdateGroup_success() {
        GroupDTO updateRequest = GroupDTO.builder()
                .groupName("Updated Group")
                .build();

        when(groupRepository.existsById(1L)).thenReturn(true);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(GroupEntity.class))).thenReturn(testGroup);

        groupService.updateGroup(1L, updateRequest);

        verify(groupRepository).save(any(GroupEntity.class));
    }

    @Test
    void testAddUsersToGroup_success() {
        UserEntity user = TestDataUtil.createTestUserEntityA();
        user.setUuid(UUID.randomUUID());
        Set<UUID> userIds = Set.of(user.getUuid());

        AddUsersToGroupRequest request = AddUsersToGroupRequest.builder()
                .userIds(userIds)
                .build();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findAllByUuidIn(userIds)).thenReturn(List.of(user));

        groupService.addUsersToGroup(1L, request);

        assertThat(testGroup.getUsers()).contains(user);
//        verify(groupRepository).save(any(GroupEntity.class));
        verify(groupRepository, times(1)).saveAndFlush(any(GroupEntity.class));

    }

    @Test
    void testListGroupMembers_success() {
        Pageable pageable = Pageable.unpaged();

        when(groupRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findUsersByGroupId(eq(1L), any(Pageable.class))).thenReturn(Page.empty());

        Page<SimpleUserDTO> result = groupService.listGroupMembers(1L, pageable);

        assertThat(result).isEmpty();
        verify(userRepository).findUsersByGroupId(eq(1L), any(Pageable.class));
    }

    @Test
    void testGetGroupManagers_success() {
        UserEntity manager = TestDataUtil.createTestUserEntityA();
        testGroup.getManagers().add(manager);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        List<SimpleUserDTO> result = groupService.getGroupManagers(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo(manager.getFullName());
    }

    @Test
    void testRemoveMembersFromGroup_success() {
        UUID userId = UUID.randomUUID();
        UserEntity user = TestDataUtil.createTestUserEntityA();
        user.setUuid(userId);

        RemoveUsersFromGroupRequest request = RemoveUsersFromGroupRequest.builder()
                .userIds(List.of(userId))
                .build();

        testGroup.getUsers().add(user);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user));

        String response = groupService.removeMembersFromGroup(1L, request);

        assertThat(response).contains("user(s) removed");
        verify(groupRepository).save(testGroup);
        verify(userRepository).saveAll(anyList());
    }

    @Test
    void testGetGroupUserStatus_success() {
        UUID userId = UUID.randomUUID();

        testGroup.setChurch(ChurchEntity.builder().churchId(1L).build());
        testGroup.getUsers().add(UserEntity.builder().uuid(userId).build());

        SimpleUserDTO userDto = new SimpleUserDTO(userId, "User Name", "user@example.com");

        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findSimpleUsersByChurchId(1L)).thenReturn(List.of(userDto));

        List<GroupUserCandidateDTO> result = groupService.getGroupUserStatus(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().isAlreadyInGroup()).isTrue();
    }

    @Test
    void testDeleteGroup_success() {
        when(groupRepository.existsById(1L)).thenReturn(true);
        groupService.delete(1L);
        verify(groupRepository, times(1)).deleteById(1L);
    }


    // ########################################################################################
    // ########################################################################################
    // Negative Tests

    @Test
    void testCreateGroup_duplicateGroupName_throwsException() {
        GroupDTO groupDTO = GroupDTO.builder()
                .groupName("Duplicate Group")
                .build();

        when(groupRepository.existsByGroupName(anyString())).thenReturn(true);

        assertThatThrownBy(() -> groupService.createGroup(groupDTO))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessageContaining("Group name already exists");

        verify(groupRepository, never()).save(any(GroupEntity.class));
    }

    @Test
    void testUpdateGroup_groupNotFound_throwsException() {
        when(groupRepository.existsById(1L)).thenReturn(false);

        GroupDTO request = GroupDTO.builder()
                .groupName("New Name")
                .build();

        assertThatThrownBy(() -> groupService.updateGroup(1L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Group not found");

        verify(groupRepository, never()).save(any(GroupEntity.class));
    }

    @Test
    void testRemoveMembersFromGroup_groupNotFound_throwsException() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        RemoveUsersFromGroupRequest request = RemoveUsersFromGroupRequest.builder()
                .userIds(List.of(UUID.randomUUID()))
                .build();

        assertThatThrownBy(() -> groupService.removeMembersFromGroup(1L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Group not found");
    }

    @Test
    void testListGroupMembers_groupNotFound_throwsException() {
        Pageable pageable = Pageable.unpaged();

        when(groupRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> groupService.listGroupMembers(1L, pageable))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Group not found");
    }

    @Test
    void testGetGroupManagers_groupNotFound_throwsException() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupManagers(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Group not found");
    }

    @Test
    void testGetGroupUserStatus_groupNotFound_throwsException() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupUserStatus(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Group not found");
    }
}
