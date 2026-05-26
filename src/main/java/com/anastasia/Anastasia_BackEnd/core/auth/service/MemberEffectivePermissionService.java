package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class MemberEffectivePermissionService {

    private static final Set<PermissionType> ACTIVE_MEMBER_PERMISSION_TYPES = Set.of(
            PermissionType.VIEW_EVENTS,
            PermissionType.VIEW_GROUPS,
            PermissionType.BOOK_APPOINTMENT,
            PermissionType.CANCEL_APPOINTMENT
    );

    private final PermissionRepository permissionRepository;

    public Set<PermissionType> resolvePermissionTypes(UserEntity user) {
        if (!hasActiveMemberAccess(user)) {
            return Set.of();
        }
        return ACTIVE_MEMBER_PERMISSION_TYPES;
    }

    public Set<Permission> resolvePermissions(UserEntity user) {
        Set<PermissionType> permissionTypes = resolvePermissionTypes(user);
        if (permissionTypes.isEmpty()) {
            return Set.of();
        }
        return permissionRepository.findAllByPermissionTypes(permissionTypes);
    }

    public boolean hasActiveMemberAccess(UserEntity user) {
        if (user == null || user.getMembership() == null) {
            return false;
        }

        String membershipStatus = user.getMembership().getStatus();
        if (membershipStatus == null || membershipStatus.isBlank()) {
            return false;
        }

        return MemberStatus.APPROVED.name().equalsIgnoreCase(membershipStatus)
                || MemberStatus.ACTIVE.name().equalsIgnoreCase(membershipStatus);
    }
}
