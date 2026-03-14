package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class UserSessionResponse {
    Integer sessionId;
    String tokenType;
    Instant createdAt;
    Instant expiresAt;
    boolean revoked;
    boolean expired;
    boolean current;
}
