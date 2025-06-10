package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.common.PagedResponse;
import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.group.GroupService;
import com.anastasia.Anastasia_BackEnd.service.auth.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    // Creating the group
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'CREATE_GROUPS')")
    public ResponseEntity<SimpleGroupEntity> createGroup(@RequestBody GroupDTO groupDTO){
        SimpleGroupEntity simpleGroupEntity = groupService.createGroup(groupDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // Get list of Groups
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
    public ResponseEntity<PagedModel<EntityModel<GroupDTO>>> listOfGroups(Pageable pageable, PagedResourcesAssembler<GroupDTO> assembler){
        Page<GroupEntity> groups = groupService.findAll(pageable);
        Page<GroupDTO> groupDTOS = groups.map(groupService::convertToDTO);

        PagedModel<EntityModel<GroupDTO>> model = assembler.toModel(groupDTOS);
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    // Get specific group by ID
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDTO> getGroup(@PathVariable Long groupId){
        Optional<GroupEntity> foundGroup = groupService.findOne(groupId);
        return foundGroup.map(groupEntity -> {
            GroupDTO groupDTO = groupService.convertToDTO(groupEntity);
            return new ResponseEntity<>(groupDTO, HttpStatus.FOUND);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Update a specific group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'EDIT_GROUPS')")
    @PutMapping("/{groupId}")
    public ResponseEntity<GroupEntity> updateGroup(@PathVariable Long groupId, @RequestBody GroupDTO groupDTO){
        boolean groupExists = groupService.exists(groupId);

        if(!groupExists){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        groupService.updateGroup(groupId, groupDTO);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    // Get list of church members as candidates for group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'CREATE_GROUPS', 'EDIT_GROUPS')")
    @GetMapping("/{groupId}/users/candidates")
    public ResponseEntity<List<GroupUserCandidateDTO>> listCandidatesForGroup(
            @PathVariable Long groupId,
            Pageable pageable,
            PagedResourcesAssembler<GroupUserCandidateDTO> assembler) {

        List<GroupUserCandidateDTO> candidates = groupService.getGroupUserStatus(groupId);

//        EntityModel<GroupUserCandidateDTO> model = assembler.toModel(candidates);
        return ResponseEntity.ok(candidates);
    }

    // Add users to group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'CREATE_GROUPS', 'EDIT_GROUPS', 'ADD_MEMBERS_TO_GROUPS')")
    @PostMapping("/{groupId}/users")
    public ResponseEntity<AddUsersToGroupResponse> addUsersToGroup(@PathVariable Long groupId,
                                                                   @Valid @RequestBody AddUsersToGroupRequest request){
        AddUsersToGroupResponse response = groupService.addUsersToGroup(groupId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Get all list of Group members
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
    @GetMapping("/group/{groupId}/members")
    public ResponseEntity<PagedModel<EntityModel<SimpleUserDTO>>> listGroupMembers(
            @PathVariable Long groupId,
            Pageable pageable,
            PagedResourcesAssembler<SimpleUserDTO> assembler){

        Page<SimpleUserDTO> members = groupService.listGroupMembers(groupId, pageable);
        PagedModel<EntityModel<SimpleUserDTO>> pagedModelMembers = assembler.toModel(members, user -> addLinks(user, groupId));

        PagedResponse<SimpleUserDTO> response = PagedResponse.<SimpleUserDTO>builder()
                .data(pagedModelMembers)
                .currentPage(members.getNumber())
                .totalPages(members.getTotalPages())
                .totalElements(members.getTotalElements())
                .size(members.getSize())
                .isFirst(members.isFirst())
                .isLast(members.isLast())
                .build();

        return new ResponseEntity<>(pagedModelMembers, HttpStatus.OK);
    }

    // Get a single group member
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
    @GetMapping("/group/members/{userId}")
    public ResponseEntity<SimpleUserDTO> getGroupMember(@PathVariable UUID userId) {
        // Fetch user logic here
        Optional<UserEntity> userEntity = userService.findOne(userId);
        return userEntity.map(foundUser -> {
            SimpleUserDTO user = SimpleUserDTO.builder()
                    .uuid(foundUser.getUuid())
                    .fullName(foundUser.getFullName())
                    .email(foundUser.getEmail())
                    .build();

            return new ResponseEntity<>(user, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Remove members from group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'REMOVE_MEMBERS_FROM_GROUPS')")
    @DeleteMapping("/{groupId}/members")
    public ResponseEntity<String> removeMembersFromGroup(
            @PathVariable Long groupId,
            @RequestBody RemoveUsersFromGroupRequest request) {
        String response = groupService.removeMembersFromGroup(groupId, request);
        return ResponseEntity.ok(response);
    }

    // Delete group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'DELETE_GROUPS')")
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId){
        groupService.delete(groupId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    // Get group managers
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
     @GetMapping("/{groupId}/managers")
     public ResponseEntity<List<SimpleUserDTO>> getGroupManagers(@PathVariable Long groupId) {
         List<SimpleUserDTO> managers = groupService.getGroupManagers(groupId);
         return new ResponseEntity<>(managers, HttpStatus.OK);
     }

    // Add batch invites with email or UUID instead of user ID
    @PostMapping("/{groupId}/batch-invite")
    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'CREATE_GROUPS', 'EDIT_GROUPS')")
    public ResponseEntity<BatchInviteResponse> batchInviteUsersToGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody BatchInviteRequest request) {
        BatchInviteResponse response = groupService.batchInviteUsersToGroup(groupId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // helper method to pass additional links to the hyperlink
    private EntityModel<SimpleUserDTO> addLinks(SimpleUserDTO user, Long groupId) {
        List<UUID> usersId = new ArrayList<>();
        usersId.add(user.uuid());

        RemoveUsersFromGroupRequest removeRequest = RemoveUsersFromGroupRequest.builder()
                .userIds(usersId)
                .build();

        return EntityModel.of(user,
                linkTo(methodOn(GroupController.class)
                        .getGroupMember(user.uuid()))
                        .withSelfRel(),
                linkTo(methodOn(GroupController.class)
                        .removeMembersFromGroup(groupId, removeRequest)) // null because we don't pass body for link building
                        .withRel("remove-from-group")
        );
    }
}
