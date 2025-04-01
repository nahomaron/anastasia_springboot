package com.anastasia.Anastasia_BackEnd.model.principal;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.Permission;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UserEntity user;

    @Getter
    private final UUID tenantId;

    public UserPrincipal(UserEntity user) {
        this.user = user;
        this.tenantId = user.getTenant().getTenantId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // Add roles
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));

            // Add permissions from role
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName().getName()));
            }
        }
        return authorities;
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
