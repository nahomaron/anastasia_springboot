package com.anastasia.Anastasia_BackEnd.service.auth;

import com.anastasia.Anastasia_BackEnd.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.token.Token;
import com.anastasia.Anastasia_BackEnd.model.token.TokenType;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.TokenRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.email.EmailService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.IllegalWriteException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    @Override
    public void createUser(UserEntity userEntity) throws MessagingException {
        // todo -> make role fetching and assigning method

        if (userRepository.existsByEmail(userEntity.getEmail())) {
            throw new IllegalWriteException("The provided email is already in use. Please use a different email.");
        }

        try {

            userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
            userEntity.setUserType(UserType.GUEST);

            UserEntity savedUser = userRepository.save(userEntity);

            // Only send email if save was successful and no exceptions occurred
            sendValidationEmail(savedUser);

        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error creating user: " + e.getMessage());
            throw new RuntimeException("User creation failed: " + e.getMessage());
        }

    }

    @Override
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token)
                // todo - exception
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if(LocalDateTime.now().isAfter(savedToken.getExpiresAt())){
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. Please find the new token sent to you!");
        }

        var user = userRepository.findById(savedToken.getUser().getUuid())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setVerified(true);
        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) throws MessagingException {

        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }


        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        if(!user.isVerified()){
            if (user.getCreatedDate().isBefore(LocalDateTime.now().minusHours(24))) {
                // The user was created more than 24 hours ago
                sendValidationEmail(user);
                throw new RuntimeException("Account is not verified. Please find a new token sent to you for verification!");
            }
            throw new RuntimeException("Account is not verified. Please find the token sent to you for verification!");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);

        var jwtToken = jwtUtil.generateAccessToken(userPrincipal);
        var refreshToken = jwtUtil.generateRefreshToken(userPrincipal);

        // first make sure the existing tokens are revoked
        revokeAllValidUserTokens(user);

        saveUserToken(jwtToken, user, TokenType.BEARER);
        saveUserToken(refreshToken, user, TokenType.REFRESH);

        return AuthenticationResponse.builder()
                .userId(user.getUuid())
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        final String authHeader = request.getHeader("Authorization");
        String refreshToken = null;
        String username = null;

        if(authHeader != null && authHeader.startsWith("Bearer ")){
            refreshToken = authHeader.substring(7);
            username = jwtUtil.extractUsername(refreshToken);
        }

        if(username != null){
            UserEntity user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid, User doesn't exist"));

            UserPrincipal userPrincipal = new UserPrincipal(user);

            if(jwtUtil.isTokenValid(refreshToken, userPrincipal)){
                var accessToken = jwtUtil.generateAccessToken(userPrincipal);
                revokeAllValidUserTokens(user);
                saveUserToken(accessToken, user, TokenType.BEARER);

                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

                try {
                    new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public Optional<UserEntity> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void resendActivationEmail(String email) throws MessagingException {
        // Find the user by email
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist"));

        // Check if the user is already activated
        if (user.isVerified()) {
            throw new IllegalStateException("User is already verified");
        }

        // Generate and send a new activation email
        sendValidationEmail(user);
    }

    public void initiatePasswordReset(String email) throws MessagingException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String resetTokenValue = generateActivationCode(8); // Longer token for security
        // Invalidate any existing password reset tokens for this user first (optional but good practice)
        tokenRepository.findAllValidTokensByUser(user.getUuid(), TokenType.PASSWORD_RESET) // Assuming you add TokenType.PASSWORD_RESET
                .forEach(token -> {
                    token.setExpired(true);
                    token.setRevoked(true);
                });

        Token resetToken = Token.builder()
                .token(resetTokenValue)
                .tokenType(TokenType.PASSWORD_RESET) // Assign a specific type
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1)) // Reset token valid for 1 hour
                .user(user)
                .build();
        tokenRepository.save(resetToken);

        Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("username", user.getFullName());
        templateProperties.put("reset_url", "http://localhost:3000/reset-password?token=" + resetTokenValue);
        templateProperties.put("reset_token", resetTokenValue); // Also provide the raw token if needed

        emailService.sendEmail(
                user.getEmail(),
                "Password Reset for Anastasia Account",
                EmailTemplateName.RESET_PASSWORD, // Make sure you have a reset_password.html template
                templateProperties
        );
        System.out.println("Password reset email triggered for: " + user.getEmail());
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        Token savedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token."));

        // Validate token type (ensure it's a password reset token)
        if (savedToken.getTokenType() != TokenType.PASSWORD_RESET) {
            throw new RuntimeException("Invalid token type for password reset.");
        }

        // Check if the token has expired
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            // Optionally, you could resend a new reset email here or prompt the user to request a new one
            throw new RuntimeException("Password reset token has expired. Please request a new one.");
        }

        // Check if the token has already been validated, expired or revoked
        if (savedToken.getValidatedAt() != null || savedToken.isExpired() || savedToken.isRevoked()) {
            throw new RuntimeException("Password reset token has already been used or is invalid.");
        }

        UserEntity user = userRepository.findById(savedToken.getUser().getUuid())
                .orElseThrow(() -> new UsernameNotFoundException("User not found for this token."));

        // Hash the new password and save it
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate the reset token after use
        savedToken.setValidatedAt(LocalDateTime.now());
        savedToken.setExpired(true);
        savedToken.setRevoked(true);
        tokenRepository.save(savedToken);

        System.out.println("Password reset successfully for user: " + user.getEmail());
    }

    @Override
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean exists(UUID userId) {
        return userRepository.existsById(userId);
    }

    // method to build and save refresh token into the database
    public void saveUserToken(String theToken, UserEntity user, TokenType tokenType){
        var token = Token.builder()
                .token(theToken)
                .user(user)
                .expired(false)
                .revoked(false)
                .build();

        tokenRepository.save(token);
    }

    // revoking the currently existing refreshTokens
    public void revokeAllValidUserTokens(UserEntity user){
        var validUserTokens = tokenRepository.findAllValidUserTokens(user.getUuid());

        if (validUserTokens.isEmpty()) return;

        validUserTokens.forEach(token -> {
            if(token.getTokenType() == TokenType.BEARER){
                token.setRevoked(true);
                token.setExpired(true);
            }
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void sendValidationEmail(UserEntity user) throws MessagingException {
        // Generate and save the activation token
        var newToken = generateAndSaveActivationToken(user);

        // Define the activation URL for the frontend application
        String activationUrl = "http://localhost:3000/activate-account";

        // Prepare the properties map for the email template
        Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("username", user.getFullName());
        templateProperties.put("confirmation_url", activationUrl);
        templateProperties.put("activation_code", newToken);

        // Call the enhanced EmailService with the dynamic properties map
        emailService.sendEmail(
                user.getEmail(),
                "Account Activation for Anastasia",
                EmailTemplateName.ACTIVATE_ACCOUNT,
                templateProperties
        );

        System.out.println("Validation email triggered for: " + user.getEmail());
    }

    private String generateAndSaveActivationToken(UserEntity user) {
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .tokenType(TokenType.ACTIVATION)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();

        tokenRepository.save(token);

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "01234456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }


}
