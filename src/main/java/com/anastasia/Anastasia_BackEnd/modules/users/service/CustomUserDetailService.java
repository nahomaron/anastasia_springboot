package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final LocalizedMessageService messageService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        var user = userRepository.findByEmail(username);

        if(user.isEmpty()){
            System.out.println("User not found");
            throw new UsernameNotFoundException(messageService.get("auth.login.userNotFound", "User not found"));
        }

        var resolvedUser = user.orElseThrow(() -> new UsernameNotFoundException(
                messageService.get("auth.login.userNotFound", "User not found")
        ));

        return new UserPrincipal(resolvedUser, resolveEffectiveRoles(resolvedUser));
    }

    private Set<Role> resolveEffectiveRoles(UserEntity user) {
        Set<Role> roles = new LinkedHashSet<>(user.getRoles() == null ? Set.of() : user.getRoles());
        Optional<TenantAdminAssignmentEntity> activeAssignment = resolveActiveTenantAdminAssignment(user);
        if (activeAssignment.isEmpty()) {
            return roles;
        }

        TenantRole tenantRole = activeAssignment.get().getRole();
        if (tenantRole == TenantRole.PRIMARY_ADMIN) {
            roles.removeIf(role -> "OWNER".equals(role.getRoleName()) || "ADMIN".equals(role.getRoleName()));
        }

        resolveRoleEntity(tenantRole).ifPresent(roles::add);
        return roles;
    }

    private Optional<TenantAdminAssignmentEntity> resolveActiveTenantAdminAssignment(
            UserEntity user
    ) {
        if (user.getTenantId() == null || user.getUuid() == null) {
            return Optional.empty();
        }

        return tenantAdminAssignmentRepository.findByTenant_IdAndUserId(user.getTenantId(), user.getUuid())
                .filter(assignment -> assignment.getStatus() == MembershipStatus.ACTIVE);
    }

    private Optional<Role> resolveRoleEntity(TenantRole tenantRole) {
        if (tenantRole == null) {
            return Optional.empty();
        }

        String roleName = switch (tenantRole) {
            case PRIMARY_ADMIN -> "PRIMARY_ADMIN";
            case ADMIN -> "ADMIN";
            case PRIMARY_OWNER, OWNER -> "OWNER";
            case FINANCE, COMMITTEE -> null;
        };

        return roleName == null ? Optional.empty() : roleRepository.findByRoleName(roleName);
    }
}
