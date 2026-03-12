package com.anastasia.Anastasia_BackEnd.core.auth.role;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType.*;

@Getter
@RequiredArgsConstructor
public enum RoleType {

    PLATFORM_ADMIN(
            Set.of(
                    MANAGE_USERS, MANAGE_ROLES, MANAGE_TENANTS, VIEW_ALL_DATA
            ), "Developer with full platform access"
    ),

    OWNER(
            Set.of(
                    OWN_SUBSCRIPTION
            ), "Owns the subscription"
    ),

    PRIMARY_ADMIN(
            Set.of(
                    MANAGE_USERS, MANAGE_ROLES, MANAGE_MEMBERS, MANAGE_GROUPS,
                    MANAGE_EVENTS, MANAGE_FOLLOWUPS, MANAGE_APPOINTMENT, MANAGE_FINANCE
            ), "Primary tenant admin manages everything under the tenant"
    ),

    USER(
            Collections.emptySet(),
            "User does not have roles"
    ),
    MEMBER(
            Collections.emptySet(),
            "Member access"
    ),
    STAFF(
            Collections.emptySet(),
            "Operational staff access"
    ),

    ADMIN(
            Set.of(
                    MANAGE_USERS, MANAGE_ROLES, MANAGE_MEMBERS, MANAGE_GROUPS,
                    MANAGE_EVENTS, MANAGE_FOLLOWUPS, MANAGE_APPOINTMENT, MANAGE_FINANCE
            ), "Admin manages everything under the tenant"
    ),
    PRIEST(
            Set.of(
                    VIEW_MEMBERS,ADD_MEMBERS, EDIT_MEMBERS, DELETE_MEMBERS, SMS_MEMBERS
            ), "Priest has role of pastoring members"
    )

    ;
    private final Set<PermissionType> permissions;

    private final String description;

    public List<SimpleGrantedAuthority> getAuthorities(){
        var authorities = getPermissions().stream()
                .map(permissionType -> new SimpleGrantedAuthority(permissionType.name())).toList();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));

        return authorities;
    }

}
