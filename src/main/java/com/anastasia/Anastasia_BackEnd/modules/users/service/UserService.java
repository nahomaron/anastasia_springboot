package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantInviteResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantMembershipAction;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MemberTransferResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserRowResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUsersPageResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TotpSetupResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateRecoveryEmailRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateUserPreferencesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateTwoFactorRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateUserProfileRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.VerifyRecoveryEmailCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.VerifyTotpSetupRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserResponseIDs;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.BackupCodesResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserPreferencesResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserMembershipsResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public interface UserService {

    UserEntity convertToEntity(UserDTO userDTO);

    UserDTO convertToDTO(UserEntity savedUserEntity);

    Page<UserResponseIDs> findAllUsers(Pageable pageable);

    Optional<SimpleUserDTO> findOne(UUID userId);

    Optional<UserEntity> findEntity(UUID userId);

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateCurrentUserProfile(@Valid UpdateUserProfileRequest request);

    UserProfileResponse updateCurrentUserRecoveryEmail(@Valid UpdateRecoveryEmailRequest request);

    void sendRecoveryEmailVerificationCode();

    boolean verifyRecoveryEmailCode(@Valid VerifyRecoveryEmailCodeRequest request);

    UserProfileResponse updateCurrentUserTwoFactor(@Valid UpdateTwoFactorRequest request);

    UserPreferencesResponse getCurrentUserPreferences();

    UserPreferencesResponse updateCurrentUserPreferences(@Valid UpdateUserPreferencesRequest request);

    TotpSetupResponse initiateTotpSetup();

    BackupCodesResponse verifyTotpSetup(@Valid VerifyTotpSetupRequest request);

    BackupCodesResponse regenerateBackupCodes();

    List<UserSessionResponse> listCurrentUserSessions(String currentBearerToken);

    void revokeCurrentUserSession(Integer sessionId);

    void revokeOtherCurrentUserSessions(String currentBearerToken);

    SimpleUserDTO updateUserDetails(UserEntity user, Principal connectedUser);

    void changePassword(ChangePasswordRequest request, Principal connectedUser);

    void assignRolesToUser(UUID userId, AssignRolesRequest request);

    List<UserResponseIDs> findAll();

    void deleteUser(UUID userId);

    void updateProfileAvatar(@Valid AvatarDTO avatarDTO);

    UserMembershipsResponse getCurrentUserMemberships();

    List<SimpleUserDTO> searchUsers(String query, Set<String> roles);

    TenantUsersPageResponse listTenantUsers(String query, String status, String role, int page, int size);

    TenantInviteResponse inviteUserToTenant(String email);

    TenantUserRowResponse applyMembershipAction(UUID userId, TenantMembershipAction action);

    MemberTransferResponse createMemberTransferRequest(UUID userId, UUID targetTenantId, String reason);

    MemberTransferResponse approveMemberTransferRequest(UUID transferRequestId, String decisionNote);

    MemberTransferResponse rejectMemberTransferRequest(UUID transferRequestId, String decisionNote);
}
