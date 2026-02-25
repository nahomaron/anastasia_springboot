package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TenantUserRowResponse {
    private UUID id;
    private UUID tenantId;
    private String username;
    private String email;
    private List<String> roles;
    private List<String> groups;
    private String membershipId;
    private TenantUserStatus status;
    private LocalDateTime createdAt;
    private boolean protectedAccount;
    private String protectedReason;
}
