package com.anastasia.Anastasia_BackEnd.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {

    private UUID userId;
    private String accessToken;
    private String refreshToken;
    private AuthenticatedUserResponse user;
}
