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
public class AuthSessionResponse {
    private UUID userId;
    private String email;
    private String username;
    private UUID tenantId;
    private Long churchId;
    private Set<String> roles;
    private Set<String> permissions;
    private Long membershipId;
    private String membershipStatus;
}
