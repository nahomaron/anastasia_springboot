package com.anastasia.Anastasia_BackEnd.service.auth;


import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.mappers.UsersMapper;
import com.anastasia.Anastasia_BackEnd.model.auth.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.model.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceUnitTest {
    @Mock private UserRepository userRepository;
    @Mock private UsersMapper usersMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleRepository roleRepository;
    @InjectMocks private UserServiceImpl userService;

    private UserEntity testUser;
    private UUID testUserId;
    private Authentication mockAuthentication;
    private UserPrincipal mockPrincipal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUserId = UUID.randomUUID();
        testUser = UserEntity.builder()
                .uuid(testUserId)
                .fullName("Test User")
                .email("test@example.com")
                .password("hashedPassword")
                .build();

        mockPrincipal = new UserPrincipal(testUser);
        mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(mockPrincipal);
    }

    @Test
    void testConvertToDTO() {
        UserDTO dto = UserDTO.builder().email("test@example.com").build();
        when(usersMapper.userEntityToUserDTO(testUser)).thenReturn(dto);

        UserDTO result = userService.convertToDTO(testUser);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testConvertToEntity() {
        UserDTO dto = UserDTO.builder().email("test@example.com").build();
        when(usersMapper.userDTOToUserEntity(dto)).thenReturn(testUser);

        UserEntity result = userService.convertToEntity(dto);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testFindAllUsers() {
        List<UserEntity> users = List.of(testUser);
        Page<UserEntity> page = new PageImpl<>(users);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<UserEntity> result = userService.findAllUsers(Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindOne_found() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        Optional<UserEntity> result = userService.findOne(testUserId);
        assertTrue(result.isPresent());
    }

    @Test
    void testUpdateUserDetails_success() {
        UserEntity updatedInfo = UserEntity.builder().fullName("Updated").email("updated@example.com").build();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserEntity result = userService.updateUserDetails(updatedInfo, mockAuthentication);
        assertEquals(testUser.getEmail(), result.getEmail());
    }

    @Test
    void testChangePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current");
        request.setNewPassword("new");
        request.setConfirmNewPassword("new");

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("current", testUser.getPassword())).thenReturn(true);

        userService.changePassword(request, mockAuthentication);

        verify(userRepository).save(any());
    }

    @Test
    void testChangePassword_incorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("new");
        request.setConfirmNewPassword("new");

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", testUser.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.changePassword(request, mockAuthentication));
    }

    @Test
    void testChangePassword_mismatchNewPasswords() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current");
        request.setNewPassword("new1");
        request.setConfirmNewPassword("new2");

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("current", testUser.getPassword())).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> userService.changePassword(request, mockAuthentication));
    }

    @Test
    void testAssignRolesToUser_success() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        Role role = Role.builder().id(1L).build();
        AssignRolesRequest request = new AssignRolesRequest(Set.of(role.getId()));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findAll()).thenReturn(List.of(role));

        userService.assignRolesToUser(testUserId, request);

        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void testAssignRolesToUser_throwsIfTenantNotInContext() {
        TenantContext.clear();  // Ensure a clean state

        Role role = Role.builder().id(1L).build();
        AssignRolesRequest request = new AssignRolesRequest(Set.of(role.getId()));

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findAll()).thenReturn(List.of(role));

        // Assert that IllegalStateException is thrown due to missing tenant
        assertThrows(IllegalStateException.class, () -> userService.assignRolesToUser(testUserId, request));

        // Verify userRepository.save is never called
        verify(userRepository, never()).save(any());
    }

    @Test
    void testFindAll() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        List<UserEntity> result = userService.findAll();
        assertEquals(1, result.size());
    }

}
