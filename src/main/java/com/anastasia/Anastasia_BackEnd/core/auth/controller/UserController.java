package com.anastasia.Anastasia_BackEnd.core.auth.controller;

import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserResponseIDs;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantInviteRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantInviteResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MemberTransferCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MemberTransferDecisionRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MemberTransferResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.BackupCodesResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TotpSetupResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateRecoveryEmailRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateUserPreferencesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateTwoFactorRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateUserProfileRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserPreferencesResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.VerifyRecoveryEmailCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.VerifyTotpSetupRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantMembershipActionRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserAccessResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateTenantUserPermissionsRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateTenantUserRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserRowResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUsersPageResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserMembershipsResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserProfileResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.service.TenantUserAccessService;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;
    private final TenantUserAccessService tenantUserAccessService;


    /**
     * Retrieves the user information of the currently authenticated user.
     *
     * @param principal The OAuth2User representing the currently authenticated user.
     * @return A map containing user attributes.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }

    /**
     * Retrieves the user information of the currently authenticated user.
     *
     * @return A map containing user attributes.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/dashboard")
    public String getDashboard(){
        return "bravo! You are logged in";
    }


    /**
     * Retrieves the user information of the currently authenticated user.
     *
     * @return A ResponseEntity containing the UserDTO of the connected user.
     */
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'VIEW_ALL_DATA')")
    @GetMapping("/")
    public ResponseEntity<List<UUID>> listOfUsers(Pageable pageable){
        Page<UserResponseIDs> users = userService.findAllUsers(pageable);
        List<UUID> userIdsList = users.stream()
                .map(UserResponseIDs::getUuid)
                .toList();

        return new ResponseEntity<>(userIdsList, HttpStatus.OK);
    }

    /**
     * Retrieves the user information of a specific user by their UUID.
     *
     * @param userId The UUID of the user to retrieve.
     * @return A ResponseEntity containing the UserDTO of the specified user, or NOT_FOUND if the user does not exist.
     */
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'VIEW_ALL_DATA')")
    @GetMapping("/{userId}")
    public ResponseEntity<SimpleUserDTO> getUser(@PathVariable UUID userId){
        return userService.findOne(userId).map(ResponseEntity::ok).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/memberships")
    public ResponseEntity<UserMembershipsResponse> getMyMemberships() {
        return ResponseEntity.ok(userService.getCurrentUserMemberships());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/profile")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponse> getMyPreferences() {
        return ResponseEntity.ok(userService.getCurrentUserPreferences());
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponse> updateMyPreferences(
            @Valid @RequestBody UpdateUserPreferencesRequest request
    ) {
        return ResponseEntity.ok(userService.updateCurrentUserPreferences(request));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/security/recovery-email")
    public ResponseEntity<UserProfileResponse> updateRecoveryEmail(
            @Valid @RequestBody UpdateRecoveryEmailRequest request
    ) {
        return ResponseEntity.ok(userService.updateCurrentUserRecoveryEmail(request));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/security/recovery-email/send-code")
    public ResponseEntity<Map<String, String>> sendRecoveryEmailVerificationCode() {
        userService.sendRecoveryEmailVerificationCode();
        return ResponseEntity.ok(Map.of("message", "Recovery email verification code sent."));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/security/recovery-email/verify-code")
    public ResponseEntity<Map<String, Object>> verifyRecoveryEmailCode(
            @Valid @RequestBody VerifyRecoveryEmailCodeRequest request
    ) {
        boolean verified = userService.verifyRecoveryEmailCode(request);
        return ResponseEntity.ok(Map.of(
                "verified", verified,
                "message", verified ? "Recovery email verified successfully." : "Invalid verification code."
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/security/two-factor")
    public ResponseEntity<UserProfileResponse> updateTwoFactor(@Valid @RequestBody UpdateTwoFactorRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUserTwoFactor(request));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/security/two-factor/totp/setup")
    public ResponseEntity<TotpSetupResponse> initiateTotpSetup() {
        return ResponseEntity.ok(userService.initiateTotpSetup());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/security/two-factor/totp/verify-setup")
    public ResponseEntity<BackupCodesResponse> verifyTotpSetup(
            @Valid @RequestBody VerifyTotpSetupRequest request
    ) {
        return ResponseEntity.ok(userService.verifyTotpSetup(request));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/security/two-factor/backup-codes/regenerate")
    public ResponseEntity<BackupCodesResponse> regenerateBackupCodes() {
        return ResponseEntity.ok(userService.regenerateBackupCodes());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/security/sessions")
    public ResponseEntity<List<UserSessionResponse>> listMySessions(HttpServletRequest request) {
        return ResponseEntity.ok(userService.listCurrentUserSessions(extractBearerToken(request)));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me/security/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> revokeSession(@PathVariable Integer sessionId) {
        userService.revokeCurrentUserSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session revoked."));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me/security/sessions/others")
    public ResponseEntity<Map<String, String>> revokeOtherSessions(HttpServletRequest request) {
        userService.revokeOtherCurrentUserSessions(extractBearerToken(request));
        return ResponseEntity.ok(Map.of("message", "Other sessions revoked."));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_USERS', 'MANAGE_APPOINTMENT', 'VIEW_TENANT_USERS')")
    @GetMapping("/search")
    public ResponseEntity<List<SimpleUserDTO>> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(value = "roles", required = false) Set<String> roles
    ) {
        return ResponseEntity.ok(userService.searchUsers(query, roles));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'VIEW_TENANT_USERS', 'MANAGE_TENANT_USERS')")
    @GetMapping("/tenant-access")
    public ResponseEntity<TenantUsersPageResponse> listTenantUsers(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    ) {
        return ResponseEntity.ok(userService.listTenantUsers(query, status, role, page, size));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'INVITE_TENANT_USERS', 'MANAGE_TENANT_USERS')")
    @PostMapping("/tenant-access/invitations")
    public ResponseEntity<TenantInviteResponse> inviteTenantUser(@Valid @RequestBody TenantInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.inviteUserToTenant(request.getEmail()));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANT_USERS')")
    @PatchMapping("/tenant-access/{userId}/membership")
    public ResponseEntity<TenantUserRowResponse> applyMembershipAction(
            @PathVariable UUID userId,
            @Valid @RequestBody TenantMembershipActionRequest request
    ) {
        return ResponseEntity.ok(userService.applyMembershipAction(userId, request.getAction()));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'VIEW_TENANT_USERS', 'MANAGE_TENANT_USERS')")
    @GetMapping("/tenant-access/{userId}/access")
    public ResponseEntity<TenantUserAccessResponse> getTenantUserAccess(@PathVariable UUID userId) {
        return ResponseEntity.ok(tenantUserAccessService.getUserAccess(userId));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANT_USERS')")
    @PutMapping("/tenant-access/{userId}/access/roles")
    public ResponseEntity<TenantUserAccessResponse> updateTenantUserRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTenantUserRolesRequest request
    ) {
        return ResponseEntity.ok(tenantUserAccessService.updateUserRoles(userId, request.getRoleIds()));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANT_USERS')")
    @PutMapping("/tenant-access/{userId}/access/permissions")
    public ResponseEntity<TenantUserAccessResponse> updateTenantUserPermissions(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTenantUserPermissionsRequest request
    ) {
        return ResponseEntity.ok(tenantUserAccessService.updateUserPermissions(userId, request.getPermissions()));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANT_USERS')")
    @PostMapping("/tenant-access/{userId}/transfer-requests")
    public ResponseEntity<MemberTransferResponse> requestMemberTransfer(
            @PathVariable UUID userId,
            @Valid @RequestBody MemberTransferCreateRequest request
    ) {
        MemberTransferResponse response = userService.createMemberTransferRequest(
                userId,
                request.getTargetTenantId(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANT_USERS')")
    @PatchMapping("/tenant-access/transfer-requests/{transferRequestId}/approve")
    public ResponseEntity<MemberTransferResponse> approveMemberTransfer(
            @PathVariable UUID transferRequestId,
            @RequestBody(required = false) MemberTransferDecisionRequest request
    ) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(userService.approveMemberTransferRequest(transferRequestId, note));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANT_USERS')")
    @PatchMapping("/tenant-access/transfer-requests/{transferRequestId}/reject")
    public ResponseEntity<MemberTransferResponse> rejectMemberTransfer(
            @PathVariable UUID transferRequestId,
            @RequestBody(required = false) MemberTransferDecisionRequest request
    ) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(userService.rejectMemberTransferRequest(transferRequestId, note));
    }


    /**
     * Updates the details of the currently authenticated user.
     * This endpoint allows the user to update their personal information such as name, email, etc.
     *
     * @param userDTO The UserDTO containing the updated user details.
     * @param connectedUser The Principal representing the currently authenticated user.
     * @return ResponseEntity containing the updated UserDTO of the connected user.
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/update-user-details")
    public ResponseEntity<SimpleUserDTO> updateUserDetails(@Valid @RequestBody UserDTO userDTO, Principal connectedUser){
        UserEntity user = userService.convertToEntity(userDTO);
        return new ResponseEntity<>(userService.updateUserDetails(user, connectedUser), HttpStatus.ACCEPTED);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/avatar")
    public ResponseEntity<String> updateProfileAvatar(
            @Valid @RequestBody ImageAssetDTO avatarDTO
    ) {
        userService.updateProfileAvatar(avatarDTO);
        return ResponseEntity.ok("Profile avatar updated successfully.");
    }


    /**
     * Assigns roles to a user identified by their UUID.
     * @param userId The UUID of the user to whom roles will be assigned.
     * @param request The AssignRolesRequest containing the roles to be assigned.
     * @return  ResponseEntity indicating the success of the operation.
     */
    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_USERS', 'MANAGE_ROLES')")
    @PutMapping("/{userId}/assign-roles")
    public ResponseEntity<?> assignRolesToUser(@PathVariable UUID userId, @RequestBody AssignRolesRequest request){
        userService.assignRolesToUser(userId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    /**
     * Changes the password of the currently authenticated user.
     * This endpoint allows the user to change their password.
     *
     * @param request The ChangePasswordRequest containing the new password details.
     * @param connectedUser The Principal representing the currently authenticated user.
     * @return ResponseEntity indicating the success of the password change operation.
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, Principal connectedUser){
        userService.changePassword(request, connectedUser);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * Deletes a user identified by their UUID.
     * @param userId The UUID of the user to be deleted.
     * @return ResponseEntity indicating the success of the deletion operation.
     */
    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_USERS')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID userId){
        userService.deleteUser(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
