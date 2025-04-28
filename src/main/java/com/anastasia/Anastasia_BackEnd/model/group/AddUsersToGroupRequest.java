package com.anastasia.Anastasia_BackEnd.model.group;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddUsersToGroupRequest {

    @NotEmpty(message = "User IDs cannot be empty")
    private Set<UUID> userIds;
}
