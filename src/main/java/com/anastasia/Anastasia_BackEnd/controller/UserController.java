package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.auth.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.model.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import com.anastasia.Anastasia_BackEnd.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }

    @GetMapping("/dashboard")
    public String getDashboard(){
        return "bravo! You are logged in";
    }


    @GetMapping("/")
    public ResponseEntity<Page<UserDTO>> listOfUsers(Pageable pageable){
        Page<UserEntity> users = userService.findAllUsers(pageable);
        return new ResponseEntity<>(users.map(userService::convertToDTO), HttpStatus.OK);
    }

    @GetMapping("/{userid}")
    public ResponseEntity<UserDTO> getUser(@PathVariable UUID userId){
        Optional<UserEntity> foundUser = userService.findOne(userId);
        return foundUser.map(userEntity -> {
            UserDTO userDTO = userService.convertToDTO(userEntity);
            return ResponseEntity.ok(userDTO);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal connectedUser){
        userService.changePassword(request, connectedUser);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PatchMapping("/update-user-details")
    public ResponseEntity<UserDTO> updateUserDetails(@RequestBody UserDTO userDTO, Principal connectedUser){
        UserEntity user = userService.convertToEntity(userDTO);
        UserEntity updatedUser = userService.updateUserDetails(user, connectedUser);
        return new ResponseEntity<>(userService.convertToDTO(updatedUser), HttpStatus.ACCEPTED);
    }

    @PutMapping("/{userId}/assign-roles")
    public ResponseEntity<?> assignRolesToUser(@PathVariable UUID userId, @RequestBody AssignRolesRequest request){
        userService.assignRolesToUser(userId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
