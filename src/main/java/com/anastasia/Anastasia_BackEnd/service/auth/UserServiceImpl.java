package com.anastasia.Anastasia_BackEnd.service.auth;

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
import com.anastasia.Anastasia_BackEnd.service.interfaces.UserServices;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserServices {

    private final UsersMapper usersMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;

    @Override
    public UserEntity convertToEntity(UserDTO userDTO) {
        return usersMapper.userDTOToUserEntity(userDTO);
    }

    @Override
    public UserDTO convertToDTO(UserEntity userEntity) {
        return usersMapper.userEntityToUserDTO(userEntity);
    }

    @Override
    public AuthenticationResponse createUser(UserEntity userEntity) {
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        var user = userRepository.save(userEntity);
        UserPrincipal userPrincipal = new UserPrincipal(user);


        var jwtToken = jwtService.generateAccessToken(userPrincipal);
        var refreshToken = jwtService.generateRefreshToken(userPrincipal);

        saveUserToken(jwtToken, user, TokenType.BEARER);
        saveUserToken(refreshToken, user, TokenType.REFRESH);


        // After a successful sign-up we send access and refresh token to the client
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserPrincipal userPrincipal = new UserPrincipal(user);

        var jwtToken = jwtService.generateAccessToken(userPrincipal);
        var refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // first make sure the existing tokens are revoked
        revokeAllValidUserTokens(user);

        saveUserToken(jwtToken, user, TokenType.BEARER);
        saveUserToken(refreshToken, user, TokenType.REFRESH);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
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
