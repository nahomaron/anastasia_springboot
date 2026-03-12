package com.anastasia.Anastasia_BackEnd.modules.groups;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.common.PagedResponse;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.*;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.groups.service.GroupService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
@RequiresTenantFeature(TenantFeature.GROUPS)
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final LocalizedMessageService messageService;

    // Creating the group
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST', 'MEMBER') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'CREATE_GROUPS')")
    public ResponseEntity<GroupResponse> createGroup(@RequestBody GroupDTO groupDTO){
        GroupResponse groupResponse = groupService.createGroup(groupDTO);
        return new ResponseEntity<>(groupResponse, HttpStatus.CREATED);
    }

    // Get list of Groups
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST', 'MEMBER') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
    public ResponseEntity<PagedModel<EntityModel<GroupResponse>>> listOfGroups(
            Pageable pageable,
            PagedResourcesAssembler<GroupResponse> assembler,
            Authentication authentication,
            @RequestParam(value = "createdBy", required = false) String createdBy
    ){
        Page<GroupResponse> groupResponses;
        boolean privileged = hasAnyRole(authentication, "OWNER", "PRIMARY_ADMIN", "ADMIN");
        if (!privileged && authentication != null) {
            UUID currentUserId = resolveCurrentUserId();
            if (currentUserId == null) {
                groupResponses = Page.empty(pageable);
            } else {
                groupResponses = groupService.findVisibleForUser(currentUserId, pageable);
            }
        } else {
            if (createdBy != null && !createdBy.isBlank()) {
                UUID creatorId = "me".equalsIgnoreCase(createdBy) ? resolveCurrentUserId() : UUID.fromString(createdBy);
                if (creatorId == null) {
                    groupResponses = Page.empty(pageable);
                } else {
                    groupResponses = groupService.findAllByCreatedBy(creatorId, pageable);
                }
            } else {
                groupResponses = groupService.findAll(pageable);
            }
        }
        PagedModel<EntityModel<GroupResponse>> model = assembler.toModel(groupResponses);
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    // Get specific group by ID
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST', 'MEMBER') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long groupId, Authentication authentication){
        boolean privileged = hasAnyRole(authentication, "OWNER", "PRIMARY_ADMIN", "ADMIN");
        Optional<GroupEntity> foundGroup;
        if (!privileged && authentication != null) {
            foundGroup = groupService.findOneVisibleForUser(groupId, resolveCurrentUserId());
        } else {
            foundGroup = groupService.findOne(groupId);
        }
        return foundGroup.map(groupEntity -> {
            GroupResponse groupResponse = groupService.convertToResponse(groupEntity);
            return new ResponseEntity<>(groupResponse, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Update a specific group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'EDIT_GROUPS')")
    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(@PathVariable Long groupId, @RequestBody GroupDTO groupDTO){
        boolean groupExists = groupService.exists(groupId);

        if(!groupExists){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        GroupResponse response = groupService.updateGroup(groupId, groupDTO);

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    // Get list of church members as candidates for group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
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

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{groupId}/users/candidates/search")
    public ResponseEntity<List<GroupUserCandidateDTO>> searchCandidatesForGroup(
            @PathVariable Long groupId,
            @RequestParam("q") String query,
            Authentication authentication
    ) {
        boolean privileged = hasAnyRole(authentication, "OWNER", "PRIMARY_ADMIN", "ADMIN", "PRIEST")
                || hasAnyAuthority(authentication, "MANAGE_GROUPS", "CREATE_GROUPS", "EDIT_GROUPS", "ADD_MEMBERS_TO_GROUPS");

        UUID currentUserId = resolveCurrentUserId();
        if (!privileged && !groupService.canManageGroup(groupId, currentUserId)) {
            throw new AccessDeniedException(messageService.get(
                    "groups.candidates.search.accessDenied",
                    "You do not have permission to search group candidates."
            ));
        }

        return ResponseEntity.ok(groupService.searchGroupUserCandidates(groupId, query));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST', 'MEMBER')")
    @PostMapping("/{groupId}/join-requests")
    public ResponseEntity<GroupJoinRequestResponse> submitJoinRequest(@PathVariable Long groupId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.submitJoinRequest(groupId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST', 'MEMBER')")
    @GetMapping("/join-requests/mine")
    public ResponseEntity<List<MyGroupJoinRequestResponse>> listMyJoinRequests() {
        return ResponseEntity.ok(groupService.listMyPendingJoinRequests());
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST', 'MEMBER')")
    @GetMapping("/{groupId}/join-requests/mine")
    public ResponseEntity<MyGroupJoinRequestResponse> getMyJoinRequestStatus(@PathVariable Long groupId) {
        return groupService.getMyJoinRequestStatus(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST', 'MEMBER')")
    @DeleteMapping("/{groupId}/join-requests/mine")
    public ResponseEntity<MyGroupJoinRequestResponse> cancelMyJoinRequest(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.cancelMyJoinRequest(groupId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{groupId}/join-requests")
    public ResponseEntity<List<GroupJoinRequestResponse>> listJoinRequests(
            @PathVariable Long groupId,
            Authentication authentication
    ) {
        ensureCanReviewJoinRequests(groupId, authentication);
        return ResponseEntity.ok(groupService.listJoinRequests(groupId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{groupId}/join-requests/{requestId}/approve")
    public ResponseEntity<GroupJoinRequestResponse> approveJoinRequest(
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @RequestBody(required = false) GroupJoinRequestDecisionRequest request,
            Authentication authentication
    ) {
        ensureCanReviewJoinRequests(groupId, authentication);
        return ResponseEntity.ok(groupService.approveJoinRequest(groupId, requestId, request));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{groupId}/join-requests/{requestId}/reject")
    public ResponseEntity<GroupJoinRequestResponse> rejectJoinRequest(
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @RequestBody(required = false) GroupJoinRequestDecisionRequest request,
            Authentication authentication
    ) {
        ensureCanReviewJoinRequests(groupId, authentication);
        return ResponseEntity.ok(groupService.rejectJoinRequest(groupId, requestId, request));
    }

    // Add users to group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'CREATE_GROUPS', 'EDIT_GROUPS', 'ADD_MEMBERS_TO_GROUPS')")
    @PostMapping("/{groupId}/users")
    public ResponseEntity<AddUsersToGroupResponse> addUsersToGroup(@PathVariable Long groupId,
                                                                   @Valid @RequestBody AddUsersToGroupRequest request){
        AddUsersToGroupResponse response = groupService.addUsersToGroup(groupId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Get all list of Group members
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
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
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
    @GetMapping("/group/members/{userId}")
    public ResponseEntity<SimpleUserDTO> getGroupMember(@PathVariable UUID userId) {
        // Fetch user logic here
        Optional<UserEntity> userEntity = userService.findEntity(userId);
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
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'REMOVE_MEMBERS_FROM_GROUPS')")
    @DeleteMapping("/{groupId}/members")
    public ResponseEntity<RemoveUsersFromGroupResponse> removeMembersFromGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody RemoveUsersFromGroupRequest request) {
        RemoveUsersFromGroupResponse response = groupService.removeMembersFromGroup(groupId, request);
        return ResponseEntity.ok(response);
    }

    // Delete group
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'DELETE_GROUPS')")
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId){
        groupService.delete(groupId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private UUID resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal userPrincipal) {
            return userPrincipal.getUserUuid();
        }

        return null;
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (String role : roles) {
            String expected = "ROLE_" + role;
            boolean found = authentication.getAuthorities().stream()
                    .anyMatch(a -> expected.equals(a.getAuthority()));
            if (found) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (String authority : authorities) {
            boolean found = authentication.getAuthorities().stream()
                    .anyMatch(a -> authority.equals(a.getAuthority()));
            if (found) {
                return true;
            }
        }
        return false;
    }

    private void ensureCanReviewJoinRequests(Long groupId, Authentication authentication) {
        boolean privileged = hasAnyRole(authentication, "OWNER", "PRIMARY_ADMIN", "ADMIN", "PRIEST")
                || hasAnyAuthority(authentication, "MANAGE_GROUPS", "CREATE_GROUPS", "EDIT_GROUPS", "ADD_MEMBERS_TO_GROUPS");

        UUID currentUserId = resolveCurrentUserId();
        if (!privileged && !groupService.canManageGroup(groupId, currentUserId)) {
            throw new AccessDeniedException(messageService.get(
                    "groups.joinRequests.manage.accessDenied",
                    "You do not have permission to manage group join requests."
            ));
        }
    }


    // Get group managers
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'VIEW_GROUPS')")
     @GetMapping("/{groupId}/managers")
     public ResponseEntity<List<SimpleUserDTO>> getGroupManagers(@PathVariable Long groupId) {
         List<SimpleUserDTO> managers = groupService.getGroupManagers(groupId);
         return new ResponseEntity<>(managers, HttpStatus.OK);
     }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'CREATE_GROUPS', 'EDIT_GROUPS')")
    @PostMapping("/{groupId}/managers")
    public ResponseEntity<AddManagersResponse> addManagersToGroup(@PathVariable Long groupId,
                                                                  @Valid @RequestBody GroupManagerRequest request) {
        AddManagersResponse response = groupService.addManagersToGroup(groupId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_GROUPS', 'REMOVE_MEMBERS_FROM_GROUPS')")
    @DeleteMapping("/{groupId}/managers")
    public ResponseEntity<RemoveManagersResponse> removeManagersFromGroup(@PathVariable Long groupId,
                                                                          @Valid @RequestBody GroupManagerRequest request) {
        RemoveManagersResponse response = groupService.removeManagersFromGroup(groupId, request);
        return ResponseEntity.ok(response);
    }

    // Add batch invites with email or UUID instead of user ID
    @PostMapping("/{groupId}/batch-invite")
    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'PRIEST') " +
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
