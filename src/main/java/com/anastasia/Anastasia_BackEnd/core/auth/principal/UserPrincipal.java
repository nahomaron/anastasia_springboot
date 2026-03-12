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

    private final UserEntity user;


    @Getter
    private final UUID tenantId;

    @Getter
    private Set<Role> roles;

    public UserPrincipal(UserEntity user) {
        this.user = user;
        this.tenantId = (user.getTenant() != null) ? user.getTenant().getId() : null; //  Safe handling
        this.roles = user.getRoles();
    }



    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        for (Role role : user.getRoles()) {
            String roleName = role.getRoleName();
            String authority = roleName != null && roleName.startsWith("ROLE_")
                    ? roleName
                    : "ROLE_" + roleName;
            authorities.add(new SimpleGrantedAuthority(authority)); // Roles

            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName().name())); // Permissions
            }
        }

        return authorities;
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
