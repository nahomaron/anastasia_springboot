package com.anastasia.Anastasia_BackEnd.UnitTests.service.auth;


import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.UsersMapper;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserPreferencesEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserProfileEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserResponseIDs;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserPreferencesRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserProfileRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserTwoFactorBackupCodeRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.service.TenantUserAccessPolicy;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserServiceUnitTest {
    @Mock private UserRepository userRepository;
    @Mock private UsersMapper usersMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleRepository roleRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private LocalizedMessageService messageService;
    @Mock private TenantUserAccessPolicy accessPolicy;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserPreferencesRepository userPreferencesRepository;
    @Mock private UserTwoFactorBackupCodeRepository backupCodeRepository;
    @InjectMocks private UserServiceImpl userService;

    private UserEntity testUser;
    private UUID testUserId;
    private UUID tenantId;
    private Authentication mockAuthentication;
    private UserPrincipal mockPrincipal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUserId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        testUser = UserEntity.builder()
                .uuid(testUserId)
                .fullName("Test User")
                .email("test@example.com")
                .password("hashedPassword")
                .userType(UserType.GUEST)
                .build();
        testUser.setTenantId(tenantId);

        mockPrincipal = new UserPrincipal(testUser);
        mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(mockPrincipal);
        lenient().when(messageService.get(anyString(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(accessPolicy.isProtectedAccount(any(UserEntity.class))).thenReturn(false);
        lenient().when(accessPolicy.isAssignableThroughTenantAccess(any(Role.class), any(UUID.class))).thenReturn(true);
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

        Page<UserResponseIDs> result = userService.findAllUsers(Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindAllUsers_scopesResultsToCurrentTenant() {
        TenantContext.setTenantId(tenantId);

        UserEntity sameTenantUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("same-tenant@example.com")
                .build();
        sameTenantUser.setTenantId(tenantId);

        Page<UserEntity> page = new PageImpl<>(List.of(sameTenantUser));
        when(userRepository.findByAffiliatedTenantId(eq(tenantId), any(Pageable.class))).thenReturn(page);

        Page<UserResponseIDs> result = userService.findAllUsers(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals(sameTenantUser.getUuid(), result.getContent().getFirst().getUuid());
        verify(userRepository).findByAffiliatedTenantId(eq(tenantId), any(Pageable.class));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void testFindOne_found() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        Optional<SimpleUserDTO> result = userService.findOne(testUserId);
        assertTrue(result.isPresent());
    }

    @Test
    void testFindOne_returnsEmptyForUserFromAnotherTenant() {
        TenantContext.setTenantId(tenantId);
        UUID otherTenantUserId = UUID.randomUUID();

        when(userRepository.findByUuidAndAffiliatedTenantId(otherTenantUserId, tenantId)).thenReturn(Optional.empty());

        Optional<SimpleUserDTO> result = userService.findOne(otherTenantUserId);

        assertTrue(result.isEmpty());
        verify(userRepository).findByUuidAndAffiliatedTenantId(otherTenantUserId, tenantId);
        verify(userRepository, never()).findById(otherTenantUserId);
    }

    @Test
    void testUpdateUserDetails_success() {
        UserEntity updatedInfo = UserEntity.builder().fullName("Updated").email("updated@example.com").build();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        SimpleUserDTO result = userService.updateUserDetails(updatedInfo, mockAuthentication);
        assertEquals(testUser.getEmail(), result.email());
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
        testUser.setTenantId(tenantId);

        Role role = Role.builder().id(1L).build();
        AssignRolesRequest request = new AssignRolesRequest(Set.of(role.getId()));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findAllById(request.roleIds())).thenReturn(List.of(role));

        userService.assignRolesToUser(testUserId, request);

        verify(userRepository).save(any(UserEntity.class));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
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
    void getCurrentUserProfile_resolvesNamedAuthenticationPrincipal() {
        Authentication namedAuthentication = mock(Authentication.class);
        UserProfileEntity profile = UserProfileEntity.builder()
                .user(testUser)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .build();
        when(namedAuthentication.getName()).thenReturn(testUser.getEmail());
        when(namedAuthentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByEmailIgnoreCase(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(profile));
        when(backupCodeRepository.countUnusedByUserId(testUserId)).thenReturn(0L);

        SecurityContextHolder.getContext().setAuthentication(namedAuthentication);

        var response = userService.getCurrentUserProfile();

        assertEquals(testUserId, response.getUserId());
        assertEquals(testUser.getEmail(), response.getEmail());
    }

    @Test
    void getCurrentUserPreferences_includesLanguage() {
        UserPreferencesEntity preferences = UserPreferencesEntity.builder()
                .user(testUser)
                .themeMode("SYSTEM")
                .language("en")
                .locale("en-US")
                .dateFormat("MMM d, yyyy")
                .firstDayOfWeek("SUNDAY")
                .build();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findById(testUserId)).thenReturn(Optional.of(preferences));
        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);

        var response = userService.getCurrentUserPreferences();

        assertEquals("en", response.getLanguage());
    }

    @Test
    void testFindAll() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        List<UserResponseIDs> result = userService.findAll();
        assertEquals(1, result.size());
    }

    @Test
    void testDeleteUser_throwsWhenUserBelongsToAnotherTenant() {
        TenantContext.setTenantId(tenantId);
        UUID otherTenantUserId = UUID.randomUUID();

        when(userRepository.findByUuidAndAffiliatedTenantId(otherTenantUserId, tenantId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(otherTenantUserId));

        verify(userRepository).findByUuidAndAffiliatedTenantId(otherTenantUserId, tenantId);
        verify(tokenRepository, never()).deleteAllByUserUuid(any());
        verify(userRepository, never()).delete(any(UserEntity.class));
    }

}
