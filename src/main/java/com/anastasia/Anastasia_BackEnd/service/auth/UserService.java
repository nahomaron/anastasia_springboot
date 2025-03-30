package com.anastasia.Anastasia_BackEnd.service.auth;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Service
public interface UserService {

    UserEntity convertToEntity(UserDTO userDTO);

    UserDTO convertToDTO(UserEntity savedUserEntity);

    Page<UserEntity> findAllUsers(Pageable pageable);

    Optional<UserEntity> findOne(UUID userId);

    UserEntity updateUserDetails(UserEntity user, Principal connectedUser);

    void changePassword(ChangePasswordRequest request, Principal connectedUser);
}
