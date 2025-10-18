package com.anastasia.Anastasia_BackEnd.api.factories;

import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;

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
