package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.modules.groups.GroupController;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
import com.anastasia.Anastasia_BackEnd.modules.groups.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class GroupControllerUnitTest {

    @Mock
    private GroupService groupService;
    @Mock
    private UserService userService;

    @InjectMocks
    private GroupController groupController;

    private GroupDTO groupDTO;
    private GroupEntity groupEntity;
    private GroupResponse groupResponse;

    @BeforeEach
    void setUp() {
        groupDTO = GroupDTO.builder()
                .groupName("Choir")
                .visibility("PUBLIC")
                .build();

        groupEntity = GroupEntity.builder()
                .groupId(5L)
                .groupName("Choir")
                .visibility("PUBLIC")
                .build();

        groupResponse = GroupResponse.builder()
                .groupId(5L)
                .groupName("Choir")
                .visibility("PUBLIC")
                .build();
    }

    @Test
    void createGroup_shouldReturnCreated() {
        GroupResponse expected = GroupResponse.builder().groupId(10L).groupName("Choir").build();
        when(groupService.createGroup(groupDTO)).thenReturn(expected);

        ResponseEntity<GroupResponse> response = groupController.createGroup(groupDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(groupService).createGroup(groupDTO);
    }

    @Test
    void listOfGroups_shouldReturnPagedModel() {
        Page<GroupResponse> groupPage = new PageImpl<>(List.of(groupResponse));
        when(groupService.findAll(Pageable.unpaged())).thenReturn(groupPage);

        @SuppressWarnings("unchecked")
        PagedResourcesAssembler<GroupResponse> assembler = mock(PagedResourcesAssembler.class);
        PagedModel<EntityModel<GroupResponse>> pagedModel = PagedModel.of(
                singletonList(EntityModel.of(groupResponse)),
                new PagedModel.PageMetadata(groupPage.getSize(), groupPage.getNumber(), groupPage.getTotalElements()));
        when(assembler.toModel(any(Page.class))).thenReturn(pagedModel);

        ResponseEntity<PagedModel<EntityModel<GroupResponse>>> response = groupController.listOfGroups(Pageable.unpaged(), assembler, null, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pagedModel);
    }

    @Test
    void getGroup_whenFound_shouldReturnDto() {
        when(groupService.findOne(5L)).thenReturn(Optional.of(groupEntity));
        when(groupService.convertToResponse(groupEntity)).thenReturn(groupResponse);

        ResponseEntity<GroupResponse> response = groupController.getGroup(5L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(groupResponse);
    }

    @Test
    void getGroup_whenMissing_shouldReturnNotFound() {
        when(groupService.findOne(9L)).thenReturn(Optional.empty());

        ResponseEntity<GroupResponse> response = groupController.getGroup(9L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateGroup_whenExists_shouldReturnAccepted() {
        when(groupService.exists(5L)).thenReturn(true);

        when(groupService.updateGroup(5L, groupDTO)).thenReturn(groupResponse);

        ResponseEntity<GroupResponse> response = groupController.updateGroup(5L, groupDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(groupService).updateGroup(5L, groupDTO);
    }

    @Test
    void updateGroup_whenMissing_shouldReturnNotFound() {
        when(groupService.exists(7L)).thenReturn(false);

        ResponseEntity<GroupResponse> response = groupController.updateGroup(7L, groupDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listCandidatesForGroup_shouldReturnCandidates() {
        when(groupService.getGroupUserStatus(5L)).thenReturn(List.of(
                GroupUserCandidateDTO.builder().uuid(UUID.randomUUID()).alreadyInGroup(true).build()));

        ResponseEntity<List<GroupUserCandidateDTO>> response = groupController.listCandidatesForGroup(5L, Pageable.unpaged(), mock(PagedResourcesAssembler.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void addUsersToGroup_shouldDelegateToService() {
        AddUsersToGroupRequest request = AddUsersToGroupRequest.builder().userIds(Set.of(UUID.randomUUID())).build();
        AddUsersToGroupResponse expected = AddUsersToGroupResponse.builder()
                .groupName("Choir")
                .addedCount(1)
                .skippedCount(0)
                .notFoundCount(0)
                .addedUserIds(List.of(UUID.randomUUID()))
                .skippedUserIds(List.of())
                .notFoundUserIds(List.of())
                .build();
        when(groupService.addUsersToGroup(5L, request)).thenReturn(expected);

        ResponseEntity<AddUsersToGroupResponse> response = groupController.addUsersToGroup(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void searchCandidatesForGroup_adminShouldReturnCandidates() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        List<GroupUserCandidateDTO> expected = List.of(
                GroupUserCandidateDTO.builder().uuid(UUID.randomUUID()).fullName("John Doe").alreadyInGroup(false).build()
        );
        when(groupService.searchGroupUserCandidates(5L, "john")).thenReturn(expected);

        ResponseEntity<List<GroupUserCandidateDTO>> response =
                groupController.searchCandidatesForGroup(5L, "john", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void submitJoinRequest_shouldReturnCreated() {
        GroupJoinRequestResponse expected = GroupJoinRequestResponse.builder()
                .id(1L)
                .groupId(5L)
                .status("PENDING")
                .build();
        when(groupService.submitJoinRequest(5L)).thenReturn(expected);

        ResponseEntity<GroupJoinRequestResponse> response = groupController.submitJoinRequest(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void cancelMyJoinRequest_shouldReturnOk() {
        MyGroupJoinRequestResponse expected = MyGroupJoinRequestResponse.builder()
                .groupId(5L)
                .requestId(1L)
                .status("CANCELLED")
                .build();
        when(groupService.cancelMyJoinRequest(5L)).thenReturn(expected);

        ResponseEntity<MyGroupJoinRequestResponse> response = groupController.cancelMyJoinRequest(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void listGroupMembers_shouldReturnPagedModel() {
        Page<SimpleUserDTO> page = new PageImpl<>(List.of(SimpleUserDTO.builder()
                .uuid(UUID.randomUUID())
                .fullName("John")
                .email("john@example.com")
                .build()));
        when(groupService.listGroupMembers(eq(5L), any(Pageable.class))).thenReturn(page);

        @SuppressWarnings("unchecked")
        PagedResourcesAssembler<SimpleUserDTO> assembler = mock(PagedResourcesAssembler.class);
        PagedModel<EntityModel<SimpleUserDTO>> pagedModel = PagedModel.of(
                singletonList(EntityModel.of(page.getContent().get(0))),
                new PagedModel.PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements()));
        when(assembler.toModel(eq(page), any(RepresentationModelAssembler.class))).thenReturn(pagedModel);

        ResponseEntity<PagedModel<EntityModel<SimpleUserDTO>>> response = groupController.listGroupMembers(5L, Pageable.unpaged(), assembler);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pagedModel);
    }

    @Test
    void getGroupMember_whenUserFound_shouldReturnUser() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().uuid(userId).fullName("Jane").email("jane@example.com").build();
        when(userService.findEntity(userId)).thenReturn(Optional.of(user));

        ResponseEntity<SimpleUserDTO> response = groupController.getGroupMember(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().uuid()).isEqualTo(userId);
    }

    @Test
    void getGroupMember_whenMissing_shouldReturnNotFound() {
        UUID userId = UUID.randomUUID();
        when(userService.findEntity(userId)).thenReturn(Optional.empty());

        ResponseEntity<SimpleUserDTO> response = groupController.getGroupMember(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removeMembersFromGroup_shouldReturnResponse() {
        RemoveUsersFromGroupRequest request = RemoveUsersFromGroupRequest.builder().userIds(List.of(UUID.randomUUID())).build();
        RemoveUsersFromGroupResponse expected = RemoveUsersFromGroupResponse.builder()
                .groupName("Choir")
                .removedCount(1)
                .notInGroupCount(0)
                .notFoundCount(0)
                .removedUserIds(List.of(UUID.randomUUID()))
                .notInGroupUserIds(List.of())
                .notFoundUserIds(List.of())
                .build();
        when(groupService.removeMembersFromGroup(5L, request)).thenReturn(expected);

        ResponseEntity<RemoveUsersFromGroupResponse> response = groupController.removeMembersFromGroup(5L, request);

        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void deleteGroup_shouldReturnNoContent() {
        ResponseEntity<?> response = groupController.deleteGroup(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(groupService).delete(5L);
    }

    @Test
    void getGroupManagers_shouldReturnManagers() {
        List<SimpleUserDTO> managers = List.of(SimpleUserDTO.builder().uuid(UUID.randomUUID()).fullName("Manager").email("manager@example.com").build());
        when(groupService.getGroupManagers(5L)).thenReturn(managers);

        ResponseEntity<List<SimpleUserDTO>> response = groupController.getGroupManagers(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(managers);
    }

    @Test
    void batchInviteUsersToGroup_shouldReturnCreatedResponse() {
        BatchInviteRequest request = BatchInviteRequest.builder().groupEmails(Set.of("a@example.com")).build();
        BatchInviteResponse expected = BatchInviteResponse.builder()
                .groupName("Choir")
                .invitedCount(1)
                .skippedCount(0)
                .notFoundCount(0)
                .invitedUserIds(List.of(UUID.randomUUID()))
                .skippedEmails(List.of())
                .notFoundEmails(List.of())
                .build();
        when(groupService.batchInviteUsersToGroup(5L, request)).thenReturn(expected);

        ResponseEntity<BatchInviteResponse> response = groupController.batchInviteUsersToGroup(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void addManagersToGroup_shouldReturnResponse() {
        GroupManagerRequest request = GroupManagerRequest.builder().managerIds(Set.of(UUID.randomUUID())).build();
        AddManagersResponse expected = AddManagersResponse.builder()
                .groupName("Choir")
                .addedCount(1)
                .skippedCount(0)
                .notFoundCount(0)
                .addedManagerIds(List.of(UUID.randomUUID()))
                .skippedManagerIds(List.of())
                .notFoundManagerIds(List.of())
                .build();
        when(groupService.addManagersToGroup(5L, request)).thenReturn(expected);

        ResponseEntity<AddManagersResponse> response = groupController.addManagersToGroup(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void removeManagersFromGroup_shouldReturnResponse() {
        GroupManagerRequest request = GroupManagerRequest.builder().managerIds(Set.of(UUID.randomUUID())).build();
        RemoveManagersResponse expected = RemoveManagersResponse.builder()
                .groupName("Choir")
                .removedCount(1)
                .notManagersCount(0)
                .notFoundCount(0)
                .removedManagerIds(List.of(UUID.randomUUID()))
                .notManagerIds(List.of())
                .notFoundManagerIds(List.of())
                .build();
        when(groupService.removeManagersFromGroup(5L, request)).thenReturn(expected);

        ResponseEntity<RemoveManagersResponse> response = groupController.removeManagersFromGroup(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
