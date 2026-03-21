package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TenantAssignedRoleResponse {
    private Long id;
    private String roleName;
    private String description;
    private boolean system;
    private List<String> permissions;
}
