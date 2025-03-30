package com.anastasia.Anastasia_BackEnd.service.auth;

import com.anastasia.Anastasia_BackEnd.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.mappers.UsersMapper;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.Token;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.TokenType;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.repository.auth.TokenRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.email.EmailService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final TenantMapper tenantMapper;
    private final EmailService emailService;



    @Override
    public void createUser(UserEntity userEntity) throws MessagingException {
        // todo -> make role fetching and assigning method
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        userRepository.save(userEntity);
        sendValidationEmail(userEntity);
    }

    private void sendValidationEmail(UserEntity user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        // send email

        String activationUrl = "http://localhost:3000/activate-account";

        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account Activation"
        );

    }

    private String generateAndSaveActivationToken(UserEntity user) {
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
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


//    @Transactional
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
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if(!user.isVerified()){
            throw new RuntimeException("Account is not verified. Please find the token sent to you for verification!");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);

        var jwtToken = jwtService.generateAccessToken(userPrincipal);
        var refreshToken = jwtService.generateRefreshToken(userPrincipal);

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
            username = jwtService.extractUsername(refreshToken);
        }

        if(username != null){
            UserEntity user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid, User doesn't exist"));

            UserPrincipal userPrincipal = new UserPrincipal(user);

            if(jwtService.isTokenValid(refreshToken, userPrincipal)){
                var accessToken = jwtService.generateAccessToken(userPrincipal);
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


}
