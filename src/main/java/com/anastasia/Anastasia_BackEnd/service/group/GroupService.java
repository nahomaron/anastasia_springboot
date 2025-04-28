package com.anastasia.Anastasia_BackEnd.service.group;

import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public interface GroupService {
    GroupEntity convertToEntity(GroupDTO groupDTO);
    GroupDTO convertToDTO(GroupEntity groupEntity);

    void createGroup(GroupDTO groupDTO);

    Page<GroupEntity> findAll(Pageable pageable);

    Optional<GroupEntity> findOne(Long groupId);

    boolean exists(Long groupId);

    void updateGroup(Long groupId, GroupDTO groupDTO);

    void delete(Long groupId);

    AddUsersToGroupResponse addUsersToGroup(Long groupId, AddUsersToGroupRequest request);

    String removeMembersFromGroup(Long groupId, RemoveUsersFromGroupRequest request);

    Page<SimpleUserDTO> listGroupMembers(Long groupId, Pageable pageable);

    List<SimpleUserDTO> getGroupManagers(Long groupId);

    List<GroupUserCandidateDTO> getGroupUserStatus(Long groupId);
}
