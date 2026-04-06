package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPaymentStatusUpdateRequest {
    @NotBlank
    private String status;
}
