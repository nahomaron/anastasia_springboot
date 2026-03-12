package com.anastasia.Anastasia_BackEnd.modules.groups.dto;

import jakarta.validation.constraints.NotEmpty;
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
public class AddUsersToGroupRequest {

    @NotEmpty(message = "{validation.groups.users.required}")
    private Set<UUID> userIds;
}
