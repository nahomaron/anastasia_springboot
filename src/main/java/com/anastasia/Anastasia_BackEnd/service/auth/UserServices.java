package com.anastasia.Anastasia_BackEnd.service.auth;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface UserServices {
    UserEntity convertToEntity(UserDTO userDTO);

    UserDTO convertToDTO(UserEntity savedUserEntity);

    void createUser(UserEntity userEntity) throws MessagingException;

    AuthenticationResponse authenticate(AuthenticationRequest request) throws MessagingException;

    void refreshToken(HttpServletRequest request, HttpServletResponse response);

    List<UserEntity> findAllUsers();

    Optional<UserEntity> findOne(UUID userId);

    boolean exists(UUID userId);

    UserEntity updateUser(UserEntity user);

    void activateAccount(String token) throws MessagingException;

//    UserEntity subscribeUserAsTenant(UUID userId, TenantEntity tenantEntity);

}
