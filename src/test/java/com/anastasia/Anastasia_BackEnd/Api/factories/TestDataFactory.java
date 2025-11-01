package com.anastasia.Anastasia_BackEnd.Api.factories;

import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;

import java.util.UUID;

public class TestDataFactory {

    private static final String DEFAULT_PASSWORD = "Password@123";

    public static UserDTO newUser() {
        String uniqueEmail = "auto_" + UUID.randomUUID() + "@mail.com";
        return UserDTO.builder()
                .fullName("Auto User " + System.currentTimeMillis())
                .email(uniqueEmail)
                .password(DEFAULT_PASSWORD)
                .confirmPassword(DEFAULT_PASSWORD)
                .build();
    }

    public static AuthenticationRequest loginRequest(String email, String password) {
        return AuthenticationRequest.builder()
                .email(email)
                .password(password)
                .build();
    }
}
