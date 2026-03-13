package com.anastasia.Anastasia_BackEnd.modules.services.marriage.support;

import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class MarriageSecuritySupport {

    private final UserRepository userRepository;

    public MarriageSecuritySupport(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("User not authenticated");
        }

        return userRepository.findById(principal.getUserUuid())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    public UUID requireCurrentUserId() {
        return requireCurrentUser().getUuid();
    }

    public boolean hasAnyRole(String... roles) {
        Set<String> required = Set.of(roles);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> {
                    String normalized = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
                    return required.stream().anyMatch(role -> role.equalsIgnoreCase(normalized));
                });
    }

    public boolean isAdminLike() {
        return hasAnyRole("OWNER", "PRIMARY_ADMIN", "ADMIN");
    }

    public boolean isSecretaryLike() {
        return isAdminLike() || hasAnyRole("STAFF");
    }
}
