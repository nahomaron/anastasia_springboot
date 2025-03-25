package com.anastasia.Anastasia_BackEnd.controller.auth;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.interfaces.UserServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserServices userServices;

    // Enabling any user to sign up and create account in anastasia app
    // here user mapper is used to map between UserEntity and UserDTO
    // Finally return the userDTO with http status of OK
    @PostMapping("/sign-up")
    public ResponseEntity<UserDTO> signUp(@RequestBody UserDTO userDTO){
        UserEntity userEntity = userServices.convertToEntity(userDTO);
        UserEntity savedUserEntity = userServices.createUser(userEntity);
        return ResponseEntity.ok(userServices.convertToDTO(savedUserEntity));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request){
        return ResponseEntity.ok(userServices.authenticate(request));
    }
}
