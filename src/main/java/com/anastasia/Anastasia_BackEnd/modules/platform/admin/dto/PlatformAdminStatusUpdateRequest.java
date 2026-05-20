package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlatformAdminStatusUpdateRequest {

    @NotBlank
    private String status;
}
