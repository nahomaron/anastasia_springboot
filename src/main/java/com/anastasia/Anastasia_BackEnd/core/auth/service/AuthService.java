package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.VerifyLoginTwoFactorRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public interface AuthService {

    void createUser(UserEntity userEntity) throws MessagingException;

    AuthenticationResponse authenticate(AuthenticationRequest request) throws MessagingException;

    AuthenticationResponse authenticateGoogleUser(String googleId, String email, String fullName);

    AuthenticationResponse verifyLoginTwoFactor(VerifyLoginTwoFactorRequest request);

    AuthenticationResponse issueSessionForUser(UUID userId);

    AuthenticationResponse refreshToken(HttpServletRequest request);

    boolean exists(UUID userId);

    AuthenticationResponse activateAccount(String token);

    Optional<UserEntity> findUserByEmail(
            @NotBlank(message = "validation.auth.email.required")
            @Email(message = "validation.auth.email.invalid")
            String email
    );

    void resendActivationEmail(String email) throws MessagingException;

    void initiatePasswordReset(String email) throws MessagingException;

    void resetPassword(String token, String newPassword);

    boolean isEmailRegistered(String email);

}
