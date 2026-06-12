package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportAccessSessionStartRequest {
    @NotNull
    private UUID tenantId;

    @NotBlank
    private String reason;

    @NotNull
    private SupportAccessScope scope;
}
