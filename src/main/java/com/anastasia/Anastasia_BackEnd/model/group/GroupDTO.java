package com.anastasia.Anastasia_BackEnd.model.group;

import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDTO {

    @NotBlank(message = "Church Id is required")
    private String churchId;

    @NotBlank(message = "Group name is required")
    private String groupName;

    private String description;

    private String avatar;

    @NotBlank(message = "Visibility Required")
    private String visibility;

    private Set<UUID> managers;

    private Set<UUID> users;
}
