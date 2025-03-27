package com.anastasia.Anastasia_BackEnd.service.interfaces;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
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

    AuthenticationResponse createUser(UserEntity userEntity);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    void refreshToken(HttpServletRequest request, HttpServletResponse response);

    List<UserEntity> findAllUsers();

    Optional<UserEntity> findOne(UUID userId);

    boolean exists(UUID userId);

    UserEntity updateUser(UserEntity user);

//    UserEntity subscribeUserAsTenant(UUID userId, TenantEntity tenantEntity);

}
