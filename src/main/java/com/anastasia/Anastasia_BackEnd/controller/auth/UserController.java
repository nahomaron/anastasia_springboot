package com.anastasia.Anastasia_BackEnd.controller.auth;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.auth.UserServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {

    private final UserServices userServices;

    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }

    @GetMapping("/dashboard")
    public String getDashboard(){
        return "bravo! You are logged in";
    }

//
//    @GetMapping("/users")
//    public ResponseEntity<List<UserDTO>> listOfUsers(){
//        List<UserDTO> users = userServices.findAllUsers().stream().map(userServices::convertToDTO).toList();
//        return ResponseEntity.ok(users);
//    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> listOfUsers(Pageable pageable){
        Page<UserEntity> users = userServices.findAllUsers(pageable);
        return new ResponseEntity<>(users.map(userServices::convertToDTO), HttpStatus.OK);
    }

    @GetMapping("/users/{userid}")
    public ResponseEntity<UserDTO> getUser(@PathVariable UUID userId){
        Optional<UserEntity> foundUser = userServices.findOne(userId);
        return foundUser.map(userEntity -> {
            UserDTO userDTO = userServices.convertToDTO(userEntity);
            return ResponseEntity.ok(userDTO);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }
}
