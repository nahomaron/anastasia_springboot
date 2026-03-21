package com.anastasia.Anastasia_BackEnd.core.auth.principal;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public class UserPrincipal implements UserDetails {
    private static final long serialVersionUID = 4584858096297851104L;

    private final UserEntity user;


    @Getter
    private final UUID tenantId;

    @Getter
    private Set<Role> roles;

    private final Set<Permission> directPermissions;

    public UserPrincipal(UserEntity user) {
        this(user, user.getRoles(), Set.of());
    }

    public UserPrincipal(UserEntity user, Set<Role> roles) {
        this(user, roles, Set.of());
    }

    public UserPrincipal(UserEntity user, Set<Role> roles, Set<Permission> directPermissions) {
        this.user = user;
        this.tenantId = (user.getTenant() != null) ? user.getTenant().getId() : null; //  Safe handling
        this.roles = roles == null ? Set.of() : new LinkedHashSet<>(roles);
        this.directPermissions = directPermissions == null ? Set.of() : new LinkedHashSet<>(directPermissions);
    }



    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> authorities = new LinkedHashSet<>();

        for (Role role : roles) {
            String roleName = role.getRoleName();
            String authority = roleName != null && roleName.startsWith("ROLE_")
                    ? roleName
                    : "ROLE_" + roleName;
            authorities.add(authority);

            for (Permission permission : role.getPermissions()) {
                authorities.add(permission.getName().name());
            }
        }

        for (Permission permission : directPermissions) {
            authorities.add(permission.getName().name());
        }

        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public boolean hasPermission(String permissionName) {
        return getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equalsIgnoreCase(permissionName));
    }



    public UUID getUserUuid(){
        return user.getUuid();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isAccountLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getDeletedAt() == null
                && user.getStatus() != null
                && switch (user.getStatus()) {
                    case ACTIVE -> true;
                    case PENDING_VERIFICATION, LOCKED, SUSPENDED, DISABLED, DELETED -> false;
                };
    }

}
