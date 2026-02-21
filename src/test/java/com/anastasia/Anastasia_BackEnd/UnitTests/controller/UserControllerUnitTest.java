package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.core.auth.controller.UserController;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserResponseIDs;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserEntity userEntity;
    private UserDTO userDTO;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userEntity = UserEntity.builder()
                .uuid(userId)
                .fullName("Controller User")
                .email("controller@example.com")
                .build();

        userDTO = UserDTO.builder()
                .fullName("Controller User")
                .email("controller@example.com")
                .password("Password1!")
                .confirmPassword("Password1!")
                .build();
    }

    @Test
    void getUserInfo_shouldReturnPrincipalAttributes() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttributes()).thenReturn(Map.of("name", "User"));

        Map<String, Object> attributes = userController.getUserInfo(principal);

        assertThat(attributes).containsEntry("name", "User");
    }

    @Test
    void getDashboard_shouldReturnStaticMessage() {
        assertThat(userController.getDashboard()).contains("logged in");
    }

    @Test
    void listOfUsers_shouldReturnUserIds() {
        Page<UserResponseIDs> userPage = new PageImpl<>(List.of(UserResponseIDs.builder().uuid(userId).build()));
        when(userService.findAllUsers(any(Pageable.class))).thenReturn(userPage);

        ResponseEntity<List<UUID>> response = userController.listOfUsers(Pageable.unpaged());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(userId);
    }

    @Test
    void getUser_whenUserExists_shouldReturnDto() {
        SimpleUserDTO expected = SimpleUserDTO.builder()
                .uuid(userId)
                .fullName(userEntity.getFullName())
                .email(userEntity.getEmail())
                .build();
        when(userService.findOne(userId)).thenReturn(java.util.Optional.of(expected));

        ResponseEntity<SimpleUserDTO> response = userController.getUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void getUser_whenUserMissing_shouldReturnNotFound() {
        when(userService.findOne(userId)).thenReturn(java.util.Optional.empty());

        ResponseEntity<SimpleUserDTO> response = userController.getUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUserDetails_shouldConvertUpdateAndReturnUpdatedDto() {
        Principal principal = () -> "principal";
        when(userService.convertToEntity(userDTO)).thenReturn(userEntity);

        SimpleUserDTO updated = SimpleUserDTO.builder()
                .uuid(userId)
                .fullName("Updated Name")
                .email("updated@example.com")
                .build();

        when(userService.updateUserDetails(userEntity, principal)).thenReturn(updated);

        ResponseEntity<SimpleUserDTO> response = userController.updateUserDetails(userDTO, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(userService).updateUserDetails(userEntity, principal);
    }

    @Test
    void updateProfileAvatar_shouldDelegateToService() {
        AvatarDTO avatarDTO = AvatarDTO.builder().imageUrl("avatar.png").build();

        ResponseEntity<String> response = userController.updateProfileAvatar(avatarDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).updateProfileAvatar(avatarDTO);
    }

    @Test
    void assignRolesToUser_shouldCallService() {
        AssignRolesRequest request = new AssignRolesRequest(Set.of(1L));

        ResponseEntity<?> response = userController.assignRolesToUser(userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).assignRolesToUser(userId, request);
    }

    @Test
    void changePassword_shouldCallService() {
        Principal principal = () -> "user";
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("Password1!")
                .confirmNewPassword("Password1!")
                .build();

        ResponseEntity<?> response = userController.changePassword(request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(userService).changePassword(request, principal);
    }

    @Test
    void deleteUser_shouldInvokeDeletion() {
        ResponseEntity<?> response = userController.deleteUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteUser(userId);
    }
}
