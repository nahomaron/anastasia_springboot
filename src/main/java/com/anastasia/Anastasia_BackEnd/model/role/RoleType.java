package com.anastasia.Anastasia_BackEnd.model.role;

import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.anastasia.Anastasia_BackEnd.model.permission.PermissionType.*;

@RequiredArgsConstructor
public enum RoleType {

    OWNER(
            Set.of(
                    MANAGE_USERS,  MANAGE_ROLES
            ), "Owns the subscription"
    ),

    USER(
            Collections.emptySet(),
            "User does not have roles"
    ),

    ADMIN(
            Set.of(
                    MANAGE_USERS,  MANAGE_ROLES
            ), "Admin manages everything under the tenant"
    ),
    PRIEST(
            Set.of(
                    VIEW_MEMBERS,ADD_MEMBERS, EDIT_MEMBERS, DELETE_MEMBERS, SMS_MEMBERS
            ), "Priest has role of pastoring members"
    )

    ;
    @Getter
    private final Set<PermissionType> permissions;

    @Getter
    private final String description;

    public List<SimpleGrantedAuthority> getAuthorities(){
        var authorities = getPermissions().stream()
                .map(permissionType -> new SimpleGrantedAuthority(permissionType.name())).toList();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));

        return authorities;
    }

}
