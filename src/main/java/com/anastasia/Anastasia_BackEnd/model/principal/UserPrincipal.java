package com.anastasia.Anastasia_BackEnd.model.principal;

import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

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
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName())); // Roles

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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
