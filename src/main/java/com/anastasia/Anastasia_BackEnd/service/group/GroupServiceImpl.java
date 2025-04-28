package com.anastasia.Anastasia_BackEnd.service.group;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.mappers.GroupMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.group.*;
import com.anastasia.Anastasia_BackEnd.model.group.GroupUserCandidateDTO;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.GroupRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService{

    private final GroupMapper groupMapper;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ChurchRepository churchRepository;

    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);  // Use SLF4J logger

    @Override
    public GroupEntity convertToEntity(GroupDTO groupDTO) {
        return groupMapper.groupDTOToEntity(groupDTO);
    }

    @Override
    public GroupDTO convertToDTO(GroupEntity groupEntity) {
        return groupMapper.groupEntityToDTO(groupEntity);
    }

    @Override
    public void createGroup(GroupDTO groupDTO) {
        if(groupRepository.existsByGroupName(groupDTO.getGroupName())){
            throw new DuplicateRequestException("Group name already exists");
        }
        GroupEntity groupEntity = groupMapper.groupDTOToEntity(groupDTO);


        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context");
        }

        ChurchEntity church = churchRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Church not found for tenant "));

        Set<UserEntity> users = new HashSet<>(userRepository.findAllByUuidIn(groupDTO.getUsers()));
        Set<UserEntity> managers = new HashSet<>(userRepository.findAllByUuidIn(groupDTO.getManagers()));

        groupEntity.setTenantId(tenantId);
        groupEntity.setChurch(church);
        groupEntity.setUsers(users);
        groupEntity.setManagers(managers);

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
    public void updateGroup(Long groupId, GroupDTO request) {

        if(!exists(groupId)){
            throw new EntityNotFoundException("Group not found");
        }
        groupRepository.findById(groupId).map(groupEntity -> {
            Optional.ofNullable(request.getGroupName()).ifPresent(groupEntity::setGroupName);
            Optional.ofNullable(request.getDescription()).ifPresent(groupEntity::setDescription);
            Optional.ofNullable(request.getAvatar()).ifPresent(groupEntity::setAvatar);
            Optional.ofNullable(request.getVisibility()).ifPresent(groupEntity::setVisibility);

             Optional.ofNullable(request.getManagers()).ifPresent(managerUUIDs -> {
                 Set<UserEntity> managers = new HashSet<>(userRepository.findAllByUuidIn(managerUUIDs));
                 groupEntity.setManagers(managers);
             });

             Optional.ofNullable(request.getUsers()).ifPresent(userUUIDs -> {
                 Set<UserEntity> users = new HashSet<>(userRepository.findAllByUuidIn(userUUIDs));
                 groupEntity.setUsers(users);
             });

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

        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new EntityNotFoundException("No users provided");
        }

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        List<UserEntity> users = userRepository.findAllByUuidIn(request.getUserIds());

        if (users.size() != request.getUserIds().size()) {
            throw new EntityNotFoundException("One or more users not found");
        }

        Set<UserEntity> newUsersToAdd = users.stream()
                .filter(user -> !group.getUsers().contains(user))
                .collect(Collectors.toSet());

        newUsersToAdd.forEach(group::addUser);

        groupRepository.saveAndFlush(group);  // Immediately flush batch insert/update

        return AddUsersToGroupResponse.builder()
                .groupName(group.getGroupName())
                .addedCount(newUsersToAdd.size())
                .build();
    }


    @Override
    public String removeMembersFromGroup(Long groupId, RemoveUsersFromGroupRequest request) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        List<UserEntity> users = userRepository.findAllById(request.getUserIds());

        for (UserEntity user : users){
            group.getUsers().remove(user);
            user.getGroups().remove(group);
        }

        groupRepository.save(group);
        userRepository.saveAll(users);

        return users.size() + " user(s) removed from "+group.getGroupName();
    }

    @Override
    public Page<SimpleUserDTO> listGroupMembers(Long groupId, Pageable pageable) {
           if(!groupRepository.existsById(groupId)){
               throw new EntityNotFoundException("Group not found");
            }

        return userRepository.findUsersByGroupId(groupId, pageable);
    }

    @Override
    public List<SimpleUserDTO> getGroupManagers(Long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        return group.getManagers().stream()
                .map(manager -> SimpleUserDTO.builder()
                        .uuid(manager.getUuid())
                        .fullName(manager.getFullName())
                        .email(manager.getEmail())
                        .build())
                .toList();
    }

    @Override
    public List<GroupUserCandidateDTO> getGroupUserStatus(Long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        Long churchId = group.getChurch().getChurchId();


        List<SimpleUserDTO> simpleUsers = userRepository.findSimpleUsersByChurchId(churchId);

        Set<UUID> usersAlreadyInGroup = group.getUsers().stream()
                .map(UserEntity::getUuid)
                .collect(Collectors.toSet());

        // Build the candidate DTOs
        return simpleUsers.stream()
                .map(user -> GroupUserCandidateDTO.builder()
                        .uuid(user.uuid())
                        .fullName(user.fullName())
                        .avatarUrl(null) // todo -> later we can load avatar here if needed
                        .alreadyInGroup(usersAlreadyInGroup.contains(user.uuid()))
                        .build())
                .toList();

    }

}
