package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantInviteResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantMembershipAction;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MemberTransferResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserRowResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUsersPageResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserResponseIDs;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserMembershipsResponse;
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
