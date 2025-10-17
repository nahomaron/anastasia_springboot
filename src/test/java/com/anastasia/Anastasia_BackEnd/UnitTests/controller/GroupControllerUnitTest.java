package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.controller.GroupController;
import com.anastasia.Anastasia_BackEnd.model.group.AddUsersToGroupRequest;
import com.anastasia.Anastasia_BackEnd.model.group.AddUsersToGroupResponse;
import com.anastasia.Anastasia_BackEnd.model.group.BatchInviteRequest;
import com.anastasia.Anastasia_BackEnd.model.group.BatchInviteResponse;
import com.anastasia.Anastasia_BackEnd.model.group.GroupDTO;
import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.group.GroupUserCandidateDTO;
import com.anastasia.Anastasia_BackEnd.model.group.RemoveUsersFromGroupRequest;
import com.anastasia.Anastasia_BackEnd.model.group.SimpleGroupEntity;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.auth.user.UserService;
import com.anastasia.Anastasia_BackEnd.service.group.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.hateoas.server.RepresentationModelAssembler;

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

@ExtendWith(MockitoExtension.class)
class GroupControllerUnitTest {

    @Mock
    private GroupService groupService;
    @Mock
    private UserService userService;

    @InjectMocks
    private GroupController groupController;

    private GroupDTO groupDTO;
    private GroupEntity groupEntity;

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
    }

    @Test
    void createGroup_shouldReturnCreated() {
        when(groupService.createGroup(groupDTO)).thenReturn(SimpleGroupEntity.builder().build());

        ResponseEntity<SimpleGroupEntity> response = groupController.createGroup(groupDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(groupService).createGroup(groupDTO);
    }

    @Test
    void listOfGroups_shouldReturnPagedModel() {
        Page<GroupEntity> groupPage = new PageImpl<>(List.of(groupEntity));
        when(groupService.findAll(Pageable.unpaged())).thenReturn(groupPage);
        when(groupService.convertToDTO(groupEntity)).thenReturn(groupDTO);

        @SuppressWarnings("unchecked")
        PagedResourcesAssembler<GroupDTO> assembler = mock(PagedResourcesAssembler.class);
        PagedModel<EntityModel<GroupDTO>> pagedModel = PagedModel.of(
                singletonList(EntityModel.of(groupDTO)),
                new PagedModel.PageMetadata(groupPage.getSize(), groupPage.getNumber(), groupPage.getTotalElements()));
        when(assembler.toModel(any(Page.class))).thenReturn(pagedModel);

        ResponseEntity<PagedModel<EntityModel<GroupDTO>>> response = groupController.listOfGroups(Pageable.unpaged(), assembler);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pagedModel);
    }

    @Test
    void getGroup_whenFound_shouldReturnDto() {
        when(groupService.findOne(5L)).thenReturn(Optional.of(groupEntity));
        when(groupService.convertToDTO(groupEntity)).thenReturn(groupDTO);

        ResponseEntity<GroupDTO> response = groupController.getGroup(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getBody()).isEqualTo(groupDTO);
    }

    @Test
    void getGroup_whenMissing_shouldReturnNotFound() {
        when(groupService.findOne(9L)).thenReturn(Optional.empty());

        ResponseEntity<GroupDTO> response = groupController.getGroup(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateGroup_whenExists_shouldReturnAccepted() {
        when(groupService.exists(5L)).thenReturn(true);

        ResponseEntity<GroupEntity> response = groupController.updateGroup(5L, groupDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(groupService).updateGroup(5L, groupDTO);
    }

    @Test
    void updateGroup_whenMissing_shouldReturnNotFound() {
        when(groupService.exists(7L)).thenReturn(false);

        ResponseEntity<GroupEntity> response = groupController.updateGroup(7L, groupDTO);

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
        AddUsersToGroupResponse expected = new AddUsersToGroupResponse("Choir", 1);
        when(groupService.addUsersToGroup(5L, request)).thenReturn(expected);

        ResponseEntity<AddUsersToGroupResponse> response = groupController.addUsersToGroup(5L, request);

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
        when(userService.findOne(userId)).thenReturn(Optional.of(user));

        ResponseEntity<SimpleUserDTO> response = groupController.getGroupMember(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().uuid()).isEqualTo(userId);
    }

    @Test
    void getGroupMember_whenMissing_shouldReturnNotFound() {
        UUID userId = UUID.randomUUID();
        when(userService.findOne(userId)).thenReturn(Optional.empty());

        ResponseEntity<SimpleUserDTO> response = groupController.getGroupMember(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removeMembersFromGroup_shouldReturnServiceMessage() {
        RemoveUsersFromGroupRequest request = RemoveUsersFromGroupRequest.builder().userIds(List.of(UUID.randomUUID())).build();
        when(groupService.removeMembersFromGroup(5L, request)).thenReturn("Removed");

        ResponseEntity<String> response = groupController.removeMembersFromGroup(5L, request);

        assertThat(response.getBody()).isEqualTo("Removed");
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
        BatchInviteResponse expected = new BatchInviteResponse("Choir", 1);
        when(groupService.batchInviteUsersToGroup(5L, request)).thenReturn(expected);

        ResponseEntity<BatchInviteResponse> response = groupController.batchInviteUsersToGroup(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
