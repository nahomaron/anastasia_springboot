package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPriestAssignmentRequest {
    @NotNull
    private UUID tenantId;
}
