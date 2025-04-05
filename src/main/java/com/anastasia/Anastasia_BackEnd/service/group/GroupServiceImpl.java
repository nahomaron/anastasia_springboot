package com.anastasia.Anastasia_BackEnd.service.group;

import com.anastasia.Anastasia_BackEnd.mappers.GroupMapper;
import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.GroupRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService{

    private final GroupMapper groupMapper;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Override
    public GroupEntity convertToEntity(GroupDTO groupDTO) {
        return groupMapper.groupDTOToEntity(groupDTO);
    }

    @Override
    public GroupDTO convertToDTO(GroupEntity groupEntity) {
        return groupMapper.groupEntityToDTO(groupEntity);
    }

    @Override
    public void createGroup(GroupEntity groupEntity) {
        if(groupRepository.existsByGroupName(groupEntity.getGroupName())){
            throw new DuplicateRequestException("Group name already exists");
        }
        groupRepository.save(groupEntity);
    }

    @Override
    public Page<GroupEntity> findAll(Pageable pageable) {
        return groupRepository.findAll(pageable);
    }

    @Override
    public Optional<GroupEntity> findOne(Long groupId) {
        return groupRepository.findById(groupId);
    }

    @Override
    public boolean exists(Long groupId) {
        return groupRepository.existsById(groupId);
    }

    @Override
    public GroupEntity updateGroup(Long groupId, GroupDTO request) {

        if(!exists(groupId)){
            throw new EntityNotFoundException("Group not found");
        }
         return groupRepository.findById(groupId).map(groupEntity -> {
            Optional.ofNullable(request.getGroupName()).ifPresent(groupEntity::setGroupName);
            Optional.ofNullable(request.getDescription()).ifPresent(groupEntity::setDescription);
            Optional.ofNullable(request.getManagers()).ifPresent(groupEntity::setManagers);
            Optional.ofNullable(request.getAvatar()).ifPresent(groupEntity::setAvatar);
            Optional.ofNullable(request.getVisibility()).ifPresent(groupEntity::setVisibility);

            return groupRepository.save(groupEntity);
        }).orElseThrow(() -> new RuntimeException("Group could not be updated"));
    }

    @Override
    public void delete(Long groupId) {
        if(!exists(groupId)){
            throw new IllegalStateException("Group does not exist in system");
        }
        groupRepository.deleteById(groupId);
    }

    @Transactional
    @Override
    public AddUsersToGroupResponse addUsersToGroup(Long groupId, AddUsersToGroupRequest request) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        List<UserEntity> users = userRepository.findAllById(request.getUserIds());

        if(users.size() != request.getUserIds().size()){
            throw new EntityNotFoundException("One or more users not found");
        }

        for (UserEntity user : users){
            group.getUsers().add(user);
            user.getGroups().add(group);
        }

        groupRepository.save(group);
        userRepository.saveAll(users);

        return AddUsersToGroupResponse.builder()
                .groupName(group.getGroupName())
                .addedCount(users.size())
                .addedUserIds(request.getUserIds())
                .build();
    }

    @Override
    public String removeUsersFromGroup(Long groupId, RemoveUsersFromGroupRequest request) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        List<UserEntity> users = userRepository.findAllById(request.getUserIds());

        for (UserEntity user : users){
            group.getUsers().remove(user);
            user.getGroups().remove(group);
        }

        groupRepository.save(group);
        userRepository.saveAll(users);

        return users.size() + "user(s) removed from "+group.getGroupName();
    }

    @Override
    public Page<UserEntity> listGroupMembers(Long groupId, Pageable pageable) {
           if(!groupRepository.existsById(groupId)){
               throw new EntityNotFoundException("Group not found");
            }

        return userRepository.findUsersByGroupId(groupId, pageable);

    }

    @Override
    public List<UserEntity> getGroupManagers(Long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        Set<UUID> managerIds = group.getManagers();

        if (managerIds == null || managerIds.isEmpty()) {
            return List.of(); // return empty list if no managers
        }

        return userRepository.findByUuidIn(managerIds);
    }
}
