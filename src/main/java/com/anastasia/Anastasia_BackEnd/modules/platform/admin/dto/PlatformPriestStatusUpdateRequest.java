package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPriestStatusUpdateRequest {
    @NotNull
    private PriestStatus status;
}
