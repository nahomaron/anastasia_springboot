package com.anastasia.Anastasia_BackEnd.core.auth.principal;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class UserPrincipal implements UserDetails {
    private static final long serialVersionUID = 4584858096297851104L;

    private final UUID userUuid;
    private final String password;
    private final String username;
    private final boolean accountLocked;
    private final Instant deletedAt;
    private final UserStatus status;

    @Getter
    private final UUID tenantId;

    @Getter
    private final Set<String> roleNames;

    private final Set<String> authorityNames;

    public UserPrincipal(UserEntity user) {
        this(user, user.getRoles(), Set.of());
    }

    public UserPrincipal(UserEntity user, Set<Role> roles) {
        this(user, roles, Set.of());
    }

    public UserPrincipal(UserEntity user, Set<Role> roles, Set<Permission> directPermissions) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        this.userUuid = user.getUuid();
        this.password = user.getPassword();
        this.username = user.getEmail();
        this.accountLocked = user.isAccountLocked();
        this.deletedAt = user.getDeletedAt();
        this.status = user.getStatus();
        this.tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        this.roleNames = buildRoleNames(roles);
        this.authorityNames = buildAuthorityNames(roles, directPermissions);
    }

    private Set<String> buildRoleNames(Set<Role> roles) {
        Set<String> names = new LinkedHashSet<>();
        if (roles != null) {
            for (Role role : roles) {
                if (role != null && role.getRoleName() != null) {
                    names.add(role.getRoleName());
                }
            }
        }
        return Set.copyOf(names);
    }

    private Set<String> buildAuthorityNames(Set<Role> roles, Set<Permission> directPermissions) {
        Set<String> names = new LinkedHashSet<>();

        if (roles != null) {
            for (Role role : roles) {
                if (role == null || role.getRoleName() == null) {
                    continue;
                }

                String roleName = role.getRoleName();
                names.add(roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName);

                if (role.getPermissions() != null) {
                    for (Permission permission : role.getPermissions()) {
                        if (permission != null && permission.getName() != null) {
                            names.add(permission.getName().name());
                        }
                    }
                }
            }
        }

        if (directPermissions != null) {
            for (Permission permission : directPermissions) {
                if (permission != null && permission.getName() != null) {
                    names.add(permission.getName().name());
                }
            }
        }

        return Set.copyOf(names);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorityNames.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public boolean hasPermission(String permissionName) {
        if (permissionName == null) {
            return false;
        }
        return authorityNames.stream()
                .anyMatch(authority -> authority.equalsIgnoreCase(permissionName));
    }

    public UUID getUserUuid() {
        return userUuid;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return deletedAt == null
                && status != null
                && switch (status) {
                    case ACTIVE -> true;
                    case PENDING_VERIFICATION, LOCKED, SUSPENDED, DISABLED, DELETED -> false;
                };
    }
}
