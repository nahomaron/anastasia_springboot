package com.anastasia.Anastasia_BackEnd;

import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;

public class TestDataUtil {

    private TestDataUtil(){}

    public static UserEntity createTestUserEntityA(){
        return UserEntity.builder()
                .fullName("Gebray weldu")
                .email("gebray@gmail.com")
                .password("Gebray@123")
                .build();
    }

    public static UserDTO createTestUserDTO(){
        return UserDTO.builder()
                .fullName("Gebray weldu")
                .email("gebray@gmail.com")
                .password("Gebray@123")
                .confirmPassword("Gebray@123")
                .build();
    }

    public static AuthenticationRequest createTestAuthenticationRequest() {
        return AuthenticationRequest.builder()
                .email("gebray@gmail.com")
                .password("Gebray@123")
                .build();
    }
}
