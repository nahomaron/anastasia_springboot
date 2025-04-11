package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.group.GroupService;
import com.anastasia.Anastasia_BackEnd.service.auth.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody GroupDTO groupDTO){
        GroupEntity groupEntity = groupService.convertToEntity(groupDTO);
        groupService.createGroup(groupEntity);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<GroupDTO>> listOfGroups(Pageable pageable){
        Page<GroupEntity> groups = groupService.findAll(pageable);
        return new ResponseEntity<>(groups.map(groupService::convertToDTO), HttpStatus.OK);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDTO> getGroup(@PathVariable Long groupId){
        Optional<GroupEntity> foundGroup = groupService.findOne(groupId);
        return foundGroup.map(groupEntity -> {
            GroupDTO groupDTO = groupService.convertToDTO(groupEntity);
            return new ResponseEntity<>(groupDTO, HttpStatus.FOUND);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupEntity> updateGroup(@PathVariable Long groupId, @RequestBody GroupDTO groupDTO){
        boolean groupExists = groupService.exists(groupId);

        if(!groupExists){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        GroupEntity group = groupService.updateGroup(groupId, groupDTO);
        return new ResponseEntity<>(group, HttpStatus.ACCEPTED);
    }

    @PostMapping("/{groupId}/users")
    public ResponseEntity<AddUsersToGroupResponse> addUsersToGroup(@PathVariable Long groupId, AddUsersToGroupRequest request){
        AddUsersToGroupResponse response = groupService.addUsersToGroup(groupId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{groupId}/users")
    public ResponseEntity<String> removeUsersFromGroup(
            @PathVariable Long groupId,
            @RequestBody RemoveUsersFromGroupRequest request) {

        String response = groupService.removeUsersFromGroup(groupId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId){
        groupService.delete(groupId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/group/{groupId}/users")
    public ResponseEntity<Page<UserDTO>> listGroupMembers(@PathVariable Long groupId, Pageable pageable){
        Page<UserEntity> members = groupService.listGroupMembers(groupId, pageable);
        return new ResponseEntity<>(members.map(userService::convertToDTO), HttpStatus.OK);
    }
     //  Add role-based filters (e.g., list only managers)
     @GetMapping("/{groupId}/managers")
     public ResponseEntity<List<UserDTO>> getGroupManagers(@PathVariable Long groupId) {
         List<UserEntity> managers = groupService.getGroupManagers(groupId);

         return new ResponseEntity<>(managers.stream()
                 .map(userService::convertToDTO).toList(), HttpStatus.OK);
     }

    // todo - Add batch invites with email or UUID instead of user ID


}
