package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.LogoutService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RoleService;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.TestSupport.ServiceIntegrationTestBase;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@Epic("Integration Tests")
@Feature("Service Layer - Auth & User Domain")
class AuthAndUserServicesIT extends ServiceIntegrationTestBase {
    private static final Pattern RESET_TOKEN_PATTERN = Pattern.compile("/reset-password\\?token=([^\"'\\s<]+)");

    @Autowired private AuthService authService;
    @Autowired private RoleService roleService;
    @Autowired private UserService userService;
    @Autowired private LogoutService logoutService;
    @Autowired private TokenRepository tokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ImageAssetRepository avatarRepository;
    @MockitoBean private EmailNotificationService emailNotificationService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void authAndUserServices_endToEndFlow() throws Exception {
        // Create user and capture activation mail
        UserEntity pendingUser = UserEntity.builder()
                .fullName("Auth Integration User")
                .email("auth+" + UUID.randomUUID() + "@example.com")
                .password(TestDataUtil.TEST_PASSWORD)
                .build();

        authService.createUser(pendingUser);

        verify(emailNotificationService).sendEmail(
                eq(pendingUser.getEmail()),
                anyString(),
                anyString(),
                anyString(),
                any()
        );
        String activationCode = tokenRepository
                .findTopByUserEmailIgnoreCaseAndTokenTypeAndDeletedAtIsNullOrderByIdDesc(
                        pendingUser.getEmail(),
                        TokenType.ACTIVATION
                )
                .orElseThrow(() -> new AssertionError("Activation token not persisted"))
                .getToken();
        authService.activateAccount(activationCode, pendingUser.getEmail());

        UserEntity activatedUser = userRepository.findByEmail(pendingUser.getEmail())
                .orElseThrow(() -> new AssertionError("User not persisted"));
        assertThat(activatedUser.isVerified()).isTrue();

        AuthenticationResponse loginResponse = authService.authenticate(
                AuthenticationRequest.builder()
                        .email(pendingUser.getEmail())
                        .password(TestDataUtil.TEST_PASSWORD)
                        .build()
        );

        assertThat(loginResponse.getAccessToken()).isNotBlank();
        assertThat(loginResponse.getRefreshToken()).isNotBlank();

        // Refresh token lifecycle
        MockHttpServletRequest refreshRequest = new MockHttpServletRequest();
        refreshRequest.setCookies(new jakarta.servlet.http.Cookie(
                RefreshTokenCookieService.REFRESH_TOKEN_COOKIE_NAME,
                loginResponse.getRefreshToken()
        ));

        AuthenticationResponse refreshed = authService.refreshToken(refreshRequest);
        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNull();

        Token storedAccessToken = tokenRepository.findTopByTokenOrderByIdDesc(loginResponse.getAccessToken())
                .orElseThrow(() -> new AssertionError("Access token not found"));

        MockHttpServletRequest logoutRequest = new MockHttpServletRequest();
        logoutRequest.addHeader("Authorization", "Bearer " + loginResponse.getAccessToken());

        logoutRequest.setCookies(new jakarta.servlet.http.Cookie(
                RefreshTokenCookieService.REFRESH_TOKEN_COOKIE_NAME,
                loginResponse.getRefreshToken()
        ));

        logoutService.logout(logoutRequest, new org.springframework.mock.web.MockHttpServletResponse(), null);

        Token revokedAccessToken = tokenRepository.findById(storedAccessToken.getId()).orElseThrow();
        assertThat(revokedAccessToken.isExpired()).isTrue();
        assertThat(revokedAccessToken.isRevoked()).isTrue();

        // Password reset flow
        reset(emailNotificationService);
        authService.initiatePasswordReset(pendingUser.getEmail());

        verify(emailNotificationService).sendEmail(
                eq(pendingUser.getEmail()),
                anyString(),
                anyString(),
                anyString(),
                any()
        );
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendEmail(
                eq(pendingUser.getEmail()),
                anyString(),
                htmlCaptor.capture(),
                anyString(),
                any()
        );
        String resetToken = extractResetToken(htmlCaptor.getValue());
        Token storedResetToken = tokenRepository
                .findTopByUserEmailIgnoreCaseAndTokenTypeAndDeletedAtIsNullOrderByIdDesc(
                        pendingUser.getEmail(),
                        TokenType.PASSWORD_RESET
                )
                .orElseThrow(() -> new AssertionError("Password reset token not persisted"));
        assertThat(storedResetToken.getToken()).isNotEqualTo(resetToken);
        String newPassword = "NewPassw0rd!";
        authService.resetPassword(resetToken, newPassword);

        AuthenticationResponse postResetResponse = authService.authenticate(
                AuthenticationRequest.builder()
                        .email(pendingUser.getEmail())
                        .password(newPassword)
                        .build()
        );
        assertThat(postResetResponse.getAccessToken()).isNotBlank();

        // Role creation and assignment
        String dynamicRoleName = "IT_ROLE_" + UUID.randomUUID();
        RoleRequest roleRequest = RoleRequest.builder()
                .roleName(dynamicRoleName)
                .description("Integration Test Role")
                .permissions(Set.of(PermissionType.VIEW_MEMBERS, PermissionType.ADD_MEMBERS))
                .build();

        roleService.createRole(roleRequest);

        Role createdRole = roleRepository.findByRoleName(dynamicRoleName)
                .orElseThrow(() -> new AssertionError("Role not persisted"));
        assertThat(createdRole.getTenantId()).isEqualTo(tenant.getId());

        userService.assignRolesToUser(activatedUser.getUuid(), new AssignRolesRequest(Set.of(createdRole.getId())));

        UserEntity userWithRoles = userRepository.findById(activatedUser.getUuid()).orElseThrow();
        assertThat(userWithRoles.getRoles())
                .extracting(Role::getRoleName)
                .contains(dynamicRoleName);

        // Update user profile details
        UserPrincipal principalAfterRoles = new UserPrincipal(userWithRoles);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principalAfterRoles,
                userWithRoles.getPassword(),
                principalAfterRoles.getAuthorities()
        );

        UserEntity updatePayload = UserEntity.builder()
                .fullName("Updated Auth User")
                .email("updated+" + UUID.randomUUID() + "@example.com")
                .build();

        SimpleUserDTO updatedDetails = userService.updateUserDetails(updatePayload, authentication);
        assertThat(updatedDetails.fullName()).isEqualTo("Updated Auth User");
        assertThat(updatedDetails.email()).isEqualTo(updatePayload.getEmail());

        UserEntity updatedUserEntity = userRepository.findById(updatedDetails.uuid())
                .orElseThrow(() -> new AssertionError("Updated user not found"));

        UserPrincipal principalAfterUpdate = new UserPrincipal(updatedUserEntity);
        Authentication updatedAuth = new UsernamePasswordAuthenticationToken(
                principalAfterUpdate,
                updatedUserEntity.getPassword(),
                principalAfterUpdate.getAuthorities()
        );

        ChangePasswordRequest changePasswordRequest = ChangePasswordRequest.builder()
                .currentPassword(newPassword)
                .newPassword("FinalPassw0rd!")
                .confirmNewPassword("FinalPassw0rd!")
                .build();
        userService.changePassword(changePasswordRequest, updatedAuth);

        AuthenticationResponse afterChange = authService.authenticate(
                AuthenticationRequest.builder()
                        .email(updatedDetails.email())
                        .password("FinalPassw0rd!")
                        .build()
        );
        assertThat(afterChange.getAccessToken()).isNotBlank();

        // Avatar update uses SecurityContext
        authenticate(updatedUserEntity);
        userService.updateProfileAvatar(new ImageAssetDTO("https://cdn.example.com/avatar.png", "21KB"));

        var storedAvatar = avatarRepository.findByOwnerId(updatedDetails.uuid())
                .orElseThrow(() -> new AssertionError("Avatar not persisted"));
        assertThat(storedAvatar.getImageUrl()).isEqualTo("https://cdn.example.com/avatar.png");
        assertThat(storedAvatar.getImageAssetType()).isEqualTo(ImageAssetType.USER);
    }

    @Test
    void activeMemberSessionIncludesFeaturePermissions() {
        Role memberRole = fetchRole(com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType.MEMBER);
        UserEntity memberUser = persistUser("member+" + UUID.randomUUID() + "@example.com", memberRole);

        Adult_MemberEntity membership = TestDataUtil.createTestMember(church);
        membership.setStatus(MemberStatus.ACTIVE.name());
        Adult_MemberEntity savedMembership = memberRepository.save(membership);

        memberUser.assignMembership(savedMembership);
        memberUser.setUserType(com.anastasia.Anastasia_BackEnd.modules.users.model.UserType.MEMBER);
        memberUser.setEmailVerifiedAt(Instant.now());
        userRepository.save(memberUser);

        AuthenticationResponse authResponse = authService.issueSessionForUser(memberUser.getUuid());

        assertThat(authResponse.getSession().getMembershipStatus()).isEqualTo(MemberStatus.ACTIVE.name());
        assertThat(authResponse.getSession().getPermissions()).contains(
                PermissionType.VIEW_EVENTS.getName(),
                PermissionType.VIEW_GROUPS.getName(),
                PermissionType.BOOK_APPOINTMENT.getName(),
                PermissionType.CANCEL_APPOINTMENT.getName()
        );
    }

    private String extractResetToken(String html) {
        Matcher matcher = RESET_TOKEN_PATTERN.matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
