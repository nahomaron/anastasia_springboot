package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.group.GroupDTO;
import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GroupMapper {

    private final UserService userService;

    // Convert GroupEntity to GroupDTO
    public GroupDTO groupEntityToDTO(GroupEntity groupEntity) {
        if (groupEntity == null) {
            return null;
        }

        // Convert the ChurchEntity to a string representation of its ID (or another desired property)
        String churchId = groupEntity.getChurch() != null ? groupEntity.getChurch().getChurchId().toString() : null;

        // Convert users and managers to their respective UUIDs
        Set<UUID> users = new HashSet<>();
        if (groupEntity.getUsers() != null) {
            for (UserEntity user : groupEntity.getUsers()) {
                if (user != null && user.getUuid() != null) {
                    users.add(user.getUuid());
                }
            }
        }

        Set<UUID> managers = new HashSet<>();
        if (groupEntity.getManagers() != null) {
            for (UserEntity manager : groupEntity.getManagers()) {
                if (manager != null && manager.getUuid() != null) {
                    managers.add(manager.getUuid());
                }
            }
        }

        // Construct and return the GroupDTO
        return GroupDTO.builder()
                .churchId(churchId)
                .groupName(groupEntity.getGroupName())
                .description(groupEntity.getDescription())
                .avatar(groupEntity.getAvatar())
                .visibility(groupEntity.getVisibility())
                .managers(managers)
                .users(users)
                .build();
    }

    // Convert GroupDTO to GroupEntity
    public GroupEntity groupDTOToEntity(GroupDTO groupDTO) {
        if (groupDTO == null) {
            return null;
        }

        // Convert the churchId back to a ChurchEntity (assuming you have a way to look it up)
        ChurchEntity churchEntity = new ChurchEntity(); // Replace with actual lookup logic if needed
        // Example: churchEntity = churchRepository.findById(UUID.fromString(groupDTO.getChurchId())).orElse(null);

        // Convert users and managers from UUIDs to UserEntities
        Set<UserEntity> users = new HashSet<>();
        if (groupDTO.getUsers() != null) {
            for (UUID userId : groupDTO.getUsers()) {
                UserEntity user = new UserEntity(); // Replace with actual lookup logic if needed
                user = userService.findOne(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
                users.add(user);
            }
        }

        Set<UserEntity> managers = new HashSet<>();
        if (groupDTO.getManagers() != null) {
            for (UUID managerId : groupDTO.getManagers()) {
                UserEntity manager = new UserEntity(); // Replace with actual lookup logic if needed
                manager = userService.findOne(managerId).orElseThrow(() -> new EntityNotFoundException("User not found"));
                managers.add(manager);
            }
        }

        // Construct and return the GroupEntity
        return GroupEntity.builder()
                .tenantId(UUID.randomUUID()) // Assuming you have the tenantId set somewhere else in your logic
                .church(churchEntity)
                .groupName(groupDTO.getGroupName())
                .description(groupDTO.getDescription())
                .avatar(groupDTO.getAvatar())
                .visibility(groupDTO.getVisibility())
                .managers(managers)
                .users(users)
                .build();
    }
}
