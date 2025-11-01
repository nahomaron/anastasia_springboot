package com.anastasia.Anastasia_BackEnd.modules.groups.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveUsersFromGroupRequest {
    @NotEmpty(message = "At least one user identifier is required")
    private List<UUID> userIds;
}
