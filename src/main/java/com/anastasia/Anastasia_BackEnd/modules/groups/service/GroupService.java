package com.anastasia.Anastasia_BackEnd.modules.groups.service;

import com.anastasia.Anastasia_BackEnd.modules.groups.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.*;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface GroupService {
    GroupEntity convertToEntity(GroupDTO groupDTO);
    GroupResponse convertToResponse(GroupEntity groupEntity);

    GroupResponse createGroup(GroupDTO groupDTO);

    Page<GroupResponse> findAll(Pageable pageable);

    Page<GroupResponse> findAllByCreatedBy(UUID createdBy, Pageable pageable);

    Page<GroupResponse> findVisibleForUser(UUID userId, Pageable pageable);

    Optional<GroupEntity> findOne(Long groupId);

    Optional<GroupEntity> findOneVisibleForUser(Long groupId, UUID userId);

    boolean exists(Long groupId);

    GroupResponse updateGroup(Long groupId, GroupDTO groupDTO);

    void delete(Long groupId);

    AddUsersToGroupResponse addUsersToGroup(Long groupId, AddUsersToGroupRequest request);

    RemoveUsersFromGroupResponse removeMembersFromGroup(Long groupId, RemoveUsersFromGroupRequest request);

    AddManagersResponse addManagersToGroup(Long groupId, GroupManagerRequest request);

    RemoveManagersResponse removeManagersFromGroup(Long groupId, GroupManagerRequest request);

    Page<SimpleUserDTO> listGroupMembers(Long groupId, Pageable pageable);

    List<SimpleUserDTO> getGroupManagers(Long groupId);

    List<GroupUserCandidateDTO> getGroupUserStatus(Long groupId);

    List<GroupUserCandidateDTO> searchGroupUserCandidates(Long groupId, String query);

    boolean canManageGroup(Long groupId, UUID userId);

    GroupJoinRequestResponse submitJoinRequest(Long groupId);

    List<GroupJoinRequestResponse> listJoinRequests(Long groupId);

    Optional<MyGroupJoinRequestResponse> getMyJoinRequestStatus(Long groupId);

    List<MyGroupJoinRequestResponse> listMyPendingJoinRequests();

    MyGroupJoinRequestResponse cancelMyJoinRequest(Long groupId);

    GroupJoinRequestResponse approveJoinRequest(Long groupId, Long requestId, GroupJoinRequestDecisionRequest request);

    GroupJoinRequestResponse rejectJoinRequest(Long groupId, Long requestId, GroupJoinRequestDecisionRequest request);

    BatchInviteResponse batchInviteUsersToGroup(Long groupId, @Valid BatchInviteRequest request);
}
