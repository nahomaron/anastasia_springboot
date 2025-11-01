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
public class GroupManagerRequest {

    @NotEmpty(message = "At least one manager identifier is required")
    private Set<UUID> managerIds;
}
