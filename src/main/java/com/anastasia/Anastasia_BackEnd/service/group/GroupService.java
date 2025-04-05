package com.anastasia.Anastasia_BackEnd.service.group;

import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface GroupService {
    GroupEntity convertToEntity(GroupDTO groupDTO);
    GroupDTO convertToDTO(GroupEntity groupEntity);

    void createGroup(GroupEntity groupEntity);

    Page<GroupEntity> findAll(Pageable pageable);

    Optional<GroupEntity> findOne(Long groupId);

    boolean exists(Long groupId);

    GroupEntity updateGroup(Long groupId, GroupDTO groupDTO);

    void delete(Long groupId);

    AddUsersToGroupResponse addUsersToGroup(Long groupId, AddUsersToGroupRequest request);

    String removeUsersFromGroup(Long groupId, RemoveUsersFromGroupRequest request);

    Page<UserEntity> listGroupMembers(Long groupId, Pageable pageable);

    List<UserEntity> getGroupManagers(Long groupId);
}
