package com.anastasia.Anastasia_BackEnd.core.auth.role;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RoleResponse {
    private Long id;
    private String roleName;
    private String description;
    private boolean system;
    private UUID tenantId;
    private long userCount;
    private List<String> permissions;
}
