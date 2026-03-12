package com.anastasia.Anastasia_BackEnd.core.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {

    private String accessToken;
    private String refreshToken;
    private AuthSessionResponse session;
    private boolean challengeRequired;
    private String challengeToken;
    private String challengeType;
    private String message;
}
