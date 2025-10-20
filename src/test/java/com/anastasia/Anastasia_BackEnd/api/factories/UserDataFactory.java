package com.anastasia.Anastasia_BackEnd.api.factories;

import com.anastasia.Anastasia_BackEnd.api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.model.auth.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.model.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;

import java.util.Set;

/**
 * Utility factory for user-centric API payloads.
 */
public final class UserDataFactory {

    private static final String DEFAULT_PASSWORD = "Password@123";

    private UserDataFactory() {
    }

    public static UserDTO newUser() {
        return UserDTO.builder()
                .fullName("Api User " + System.currentTimeMillis())
                .email("api-user+" + System.currentTimeMillis() + "@mail.com")
                .password(DEFAULT_PASSWORD)
                .confirmPassword(DEFAULT_PASSWORD)
                .build();
    }

    public static UserDTO updatePayload() {
        UserDTO dto = newUser();
        dto.setFullName("Updated " + dto.getFullName());
        return dto;
    }

    public static ChangePasswordRequest changePasswordRequest(String currentPassword, String nextPassword) {
        return ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(nextPassword)
                .confirmNewPassword(nextPassword)
                .build();
    }

    public static AssignRolesRequest assignRolesRequest(Set<Long> roleIds) {
        return new AssignRolesRequest(roleIds);
    }

    public static String randomEmail() {
        return DataGenerator.randomEmail();
    }
}
