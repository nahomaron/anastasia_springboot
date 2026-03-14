package com.anastasia.Anastasia_BackEnd.modules.groups.support;

import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component("groupSecuritySupport")
@RequiredArgsConstructor
public class GroupSecuritySupport {

    private static final Set<String> PRIVILEGED_ROLES = Set.of(
            "ROLE_OWNER",
            "ROLE_PRIMARY_ADMIN",
            "ROLE_ADMIN",
            "ROLE_PRIEST"
    );

    private static final Set<String> MANAGEMENT_AUTHORITIES = Set.of(
            "MANAGE_GROUPS",
            "CREATE_GROUPS",
            "EDIT_GROUPS",
            "ADD_MEMBERS_TO_GROUPS",
            "MANAGE_REQUESTS"
    );

    private final GroupService groupService;

    public boolean canManageGroup(Authentication authentication, Long groupId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        if (authorities.stream().anyMatch(PRIVILEGED_ROLES::contains)
                || authorities.stream().anyMatch(MANAGEMENT_AUTHORITIES::contains)) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            return false;
        }

        UUID currentUserId = userPrincipal.getUserUuid();
        return currentUserId != null && groupService.canManageGroup(groupId, currentUserId);
    }
}
