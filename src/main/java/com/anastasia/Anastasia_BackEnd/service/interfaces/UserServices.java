package com.anastasia.Anastasia_BackEnd.service.interfaces;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import org.springframework.stereotype.Service;

@Service
public interface UserServices {
    UserEntity convertToEntity(UserDTO userDTO);

    UserDTO convertToDTO(UserEntity savedUserEntity);

    UserEntity createUser(UserEntity userEntity);

    AuthenticationResponse authenticate(AuthenticationRequest request);
}
