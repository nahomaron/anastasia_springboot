package com.anastasia.Anastasia_BackEnd.service.auth;

import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public interface AuthService {


    void createUser(UserEntity userEntity) throws MessagingException;

    AuthenticationResponse authenticate(AuthenticationRequest request) throws MessagingException;

    void refreshToken(HttpServletRequest request, HttpServletResponse response);

    boolean exists(UUID userId);

    void activateAccount(String token) throws MessagingException;

    Optional<UserEntity> findUserByEmail(@NotBlank(message = "Email is required") @Email(message = "Invalid Email format") String email);

    void resendActivationEmail(String email) throws MessagingException;

    void initiatePasswordReset(String email) throws MessagingException;

    void resetPassword(String token, String newPassword);

    boolean isEmailRegistered(String email);

}
