package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTenantUserRolesRequest {

    @Builder.Default
    @NotNull
    private List<Long> roleIds = List.of();
}
