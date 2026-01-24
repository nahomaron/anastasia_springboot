package com.anastasia.Anastasia_BackEnd.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private UUID tenantId;
    private Set<String> roles;
    private Set<String> permissions;
}
