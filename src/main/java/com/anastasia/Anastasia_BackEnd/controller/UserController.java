package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.auth.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.model.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import com.anastasia.Anastasia_BackEnd.service.auth.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;
    private final UserService userService;


    /**
     * Retrieves the user information of the currently authenticated user.
     *
     * @param principal The OAuth2User representing the currently authenticated user.
     * @return A map containing user attributes.
     */
    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }

    /**
     * Retrieves the user information of the currently authenticated user.
     *
     * @return A map containing user attributes.
     */
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MEMBER')")
    @GetMapping("/dashboard")
    public String getDashboard(){
        return "bravo! You are logged in";
    }


    /**
     * Retrieves the user information of the currently authenticated user.
     *
     * @return A ResponseEntity containing the UserDTO of the connected user.
     */
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/")
    public ResponseEntity<List<UUID>> listOfUsers(Pageable pageable){
        Page<UserEntity> users = userService.findAllUsers(pageable);
        List<UUID> userIdsList = users.stream()
                .map(UserEntity::getUuid)
                .toList();

        return new ResponseEntity<>(userIdsList, HttpStatus.OK);
    }

    /**
     * Retrieves the user information of a specific user by their UUID.
     *
     * @param userId The UUID of the user to retrieve.
     * @return A ResponseEntity containing the UserDTO of the specified user, or NOT_FOUND if the user does not exist.
     */
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
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


    /**
     * Updates the details of the currently authenticated user.
     * This endpoint allows the user to update their personal information such as name, email, etc.
     *
     * @param userDTO The UserDTO containing the updated user details.
     * @param connectedUser The Principal representing the currently authenticated user.
     * @return ResponseEntity containing the updated UserDTO of the connected user.
     */
    @PatchMapping("/update-user-details")
    public ResponseEntity<UserDTO> updateUserDetails(@RequestBody UserDTO userDTO, Principal connectedUser){
        UserEntity user = userService.convertToEntity(userDTO);
        UserEntity updatedUser = userService.updateUserDetails(user, connectedUser);
        return new ResponseEntity<>(userService.convertToDTO(updatedUser), HttpStatus.ACCEPTED);
    }


    /**
     * Assigns roles to a user identified by their UUID.
     * @param userId The UUID of the user to whom roles will be assigned.
     * @param request The AssignRolesRequest containing the roles to be assigned.
     * @return  ResponseEntity indicating the success of the operation.
     */
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PutMapping("/{userId}/assign-roles")
    public ResponseEntity<?> assignRolesToUser(@PathVariable UUID userId, @RequestBody AssignRolesRequest request){
        userService.assignRolesToUser(userId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Changes the password of the currently authenticated user.
     * This endpoint allows the user to change their password.
     *
     * @param request The ChangePasswordRequest containing the new password details.
     * @param connectedUser The Principal representing the currently authenticated user.
     * @return ResponseEntity indicating the success of the password change operation.
     */
    @PreAuthorize("@hashRole('USER')")
    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal connectedUser){
        userService.changePassword(request, connectedUser);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * Deletes a user identified by their UUID.
     * @param userId The UUID of the user to be deleted.
     * @return ResponseEntity indicating the success of the deletion operation.
     */
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID userId){
        userService.deleteUser(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


}
