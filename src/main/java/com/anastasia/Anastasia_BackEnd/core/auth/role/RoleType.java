package com.anastasia.Anastasia_BackEnd.core.auth.role;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType.*;

@Getter
@RequiredArgsConstructor
public enum RoleType {

    PLATFORM_ADMIN(
            Set.of(
                    MANAGE_USERS, VIEW_TENANT_USERS, INVITE_TENANT_USERS, MANAGE_TENANT_USERS,
                    MANAGE_ROLES, MANAGE_TENANTS, MANAGE_TENANT_BILLING, VIEW_ALL_DATA,
                    VIEW_STAFF, MANAGE_STAFF, RESET_STAFF_CREDENTIALS,
                    VIEW_PRIESTS, MANAGE_PRIESTS, VIEW_PRIEST_ASSIGNMENTS,
                    APPROVE_MEMBERSHIP_AS_PRIEST, VIEW_PRIEST_DASHBOARD
            ), "Developer with full platform access"
    ),

    OWNER(
            Set.of(
                    OWN_SUBSCRIPTION, MANAGE_TENANT_BILLING
            ), "Owns the subscription"
    ),

    PRIMARY_ADMIN(
            Set.of(
                    MANAGE_USERS, VIEW_TENANT_USERS, INVITE_TENANT_USERS, MANAGE_TENANT_USERS,
                    MANAGE_ROLES, MANAGE_TENANT_BILLING, MANAGE_MEMBERS, MANAGE_GROUPS,
                    MANAGE_EVENTS, MANAGE_FOLLOWUPS, MANAGE_APPOINTMENT, MANAGE_FINANCE,
                    VIEW_ACCOUNTS, MANAGE_ACCOUNTS, VIEW_FUNDS, MANAGE_FUNDS, RECORD_TRANSACTIONS,
                    RECONCILE_ACCOUNTS, IMPORT_FINANCIAL_DATA, EXPORT_FINANCIAL_DATA,
                    VIEW_STAFF, MANAGE_STAFF, RESET_STAFF_CREDENTIALS,
                    VIEW_PRIESTS, MANAGE_PRIESTS, VIEW_PRIEST_ASSIGNMENTS,
                    APPROVE_MEMBERSHIP_AS_PRIEST, VIEW_PRIEST_DASHBOARD
            ), "Primary tenant admin has full tenant-level permissions except platform-only capabilities"
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
                    VIEW_TENANT_USERS
            ), "Admin starts with limited default access and should be granted explicit permissions as needed"
    ),
    PRIEST(
            Set.of(
                    VIEW_MEMBERS, ADD_MEMBERS, EDIT_MEMBERS, DELETE_MEMBERS, SMS_MEMBERS,
                    VIEW_GROUPS, MANAGE_REQUESTS, VIEW_STAFF,
                    VIEW_PRIESTS, VIEW_PRIEST_ASSIGNMENTS,
                    APPROVE_MEMBERSHIP_AS_PRIEST, VIEW_PRIEST_DASHBOARD
            ), "Priest has role of pastoring members"
    )

    ;
    private final Set<PermissionType> permissions;

    private final String description;

    public List<SimpleGrantedAuthority> getAuthorities(){
        var authorities = new ArrayList<>(getPermissions().stream()
                .map(permissionType -> new SimpleGrantedAuthority(permissionType.name()))
                .toList());

        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));

        return authorities;
    }

}
