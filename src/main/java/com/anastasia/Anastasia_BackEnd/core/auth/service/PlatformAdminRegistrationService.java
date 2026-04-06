package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.core.auth.dto.PlatformAdminRegistrationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformAdminRegistrationService {

    private static final String DEVELOPER_SUPER_ROLE = "DEVELOPER_SUPER_USER";
    private static final List<String> REQUIRED_ROLE_NAMES = List.of(
            "OWNER",
            "PLATFORM_ADMIN",
            "PRIMARY_ADMIN",
            "MEMBER",
            "PRIEST",
            "USER"
    );

    @Value("${app.platform-admin.secret:${platform.admin.secret:}}")
    private String configuredSecret;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity registerPlatformAdmin(PlatformAdminRegistrationRequest request, String providedSecret) {
        ensureSecretMatches(providedSecret);

        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("A user with that email already exists");
        }

        Set<Role> roles = resolveRequiredRoles();
        roles.add(ensureSuperRoleWithAllPermissions());

        UserEntity newUser = UserEntity.builder()
                .fullName(determineFullName(request.getFullName(), normalizedEmail))
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.STAFF)
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();

        newUser.setVerified(true);
        return userRepository.save(newUser);
    }

    private void ensureSecretMatches(String providedSecret) {
        if (!StringUtils.hasText(configuredSecret)) {
            throw new IllegalStateException("app.platform-admin.secret must be configured before using the developer registration endpoint");
        }
        if (!StringUtils.hasText(providedSecret) || !configuredSecret.equals(providedSecret.trim())) {
            throw new AccessDeniedException("Invalid developer secret");
        }
    }

    private Set<Role> resolveRequiredRoles() {
        Set<Role> roles = new LinkedHashSet<>();
        for (String roleName : REQUIRED_ROLE_NAMES) {
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Required role " + roleName + " is not seeded"));
            roles.add(role);
        }
        return roles;
    }

    private Role ensureSuperRoleWithAllPermissions() {
        return roleRepository.findByRoleName(DEVELOPER_SUPER_ROLE)
                .orElseGet(() -> createDeveloperSuperRole());
    }

    private Role createDeveloperSuperRole() {
        Set<Permission> allPermissions = permissionRepository.findAll().stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Role role = Role.builder()
                .roleName(DEVELOPER_SUPER_ROLE)
                .description("Developer access role that carries every permission in the system")
                .permissions(allPermissions)
                .tenant(null)
                .build();
        return roleRepository.save(role);
    }

    private String determineFullName(String fullName, String email) {
        if (StringUtils.hasText(fullName)) {
            return fullName.trim();
        }
        return "Platform Admin (" + email + ")";
    }
}
