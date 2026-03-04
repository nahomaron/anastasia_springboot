package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserSessionResponse {
    Integer sessionId;
    String tokenType;
    LocalDateTime createdAt;
    LocalDateTime expiresAt;
    boolean revoked;
    boolean expired;
    boolean current;
}
