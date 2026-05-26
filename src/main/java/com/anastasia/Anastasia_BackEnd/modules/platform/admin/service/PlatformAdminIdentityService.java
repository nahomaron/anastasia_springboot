package com.anastasia.Anastasia_BackEnd.modules.platform.admin.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminInviteRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminInviteResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminUserResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserProfileEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserProfileRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformAdminIdentityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String BREAK_GLASS_ROLE = "DEVELOPER_SUPER_USER";
    private static final Set<String> PLATFORM_ADMIN_ROLE_NAMES = Set.of(
            RoleType.PLATFORM_ADMIN.name(),
            BREAK_GLASS_ROLE
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<PlatformAdminUserResponse> listAdmins() {
        List<UserEntity> users = userRepository.findAllByRoleNames(PLATFORM_ADMIN_ROLE_NAMES);
        Map<UUID, UserProfileEntity> profilesByUserId = userProfileRepository.findByUserIdIn(
                users.stream().map(UserEntity::getUuid).toList()
        ).stream().collect(LinkedHashMap::new, (map, profile) -> map.put(profile.getUserId(), profile), Map::putAll);

        return users.stream()
                .map(user -> toResponse(user, profilesByUserId.get(user.getUuid())))
                .sorted(Comparator
                        .comparing(PlatformAdminUserResponse::isBreakGlass).reversed()
                        .thenComparing(PlatformAdminUserResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlatformAdminUserResponse::getEmail, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public PlatformAdminInviteResponse inviteAdmin(PlatformAdminInviteRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("A user with that email already exists.");
        }

        Role platformAdminRole = roleRepository.findByRoleName(RoleType.PLATFORM_ADMIN.name())
                .orElseThrow(() -> new IllegalStateException("PLATFORM_ADMIN role is not seeded"));

        UserEntity user = UserEntity.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(generateTemporaryPassword()))
                .roles(new LinkedHashSet<>(Set.of(platformAdminRole)))
                .userType(UserType.STAFF)
                .status(UserStatus.ACTIVE)
                .emailVerifiedAt(Instant.now())
                .mustChangePassword(true)
                .temporaryPasswordIssuedAt(Instant.now())
                .lastPasswordChangedAt(null)
                .build();
        UserEntity saved = userRepository.save(user);

        boolean onboardingEmailSent = true;
        String message = "Platform admin invited. Password setup email sent.";
        try {
            authService.initiatePasswordReset(saved.getEmail());
        } catch (MessagingException ex) {
            onboardingEmailSent = false;
            message = "Platform admin created, but the password setup email could not be sent.";
        }

        return PlatformAdminInviteResponse.builder()
                .admin(toResponse(saved, null))
                .onboardingEmailSent(onboardingEmailSent)
                .message(message)
                .build();
    }

    @Transactional
    public PlatformAdminUserResponse updateStatus(UUID userId, String requestedStatus, UUID actorUserId) {
        UserEntity user = requirePlatformAdmin(userId);
        UserStatus targetStatus = parseManagedStatus(requestedStatus);

        if (user.getStatus() == targetStatus) {
            return toResponse(user, userProfileRepository.findById(user.getUuid()).orElse(null));
        }

        if (actorUserId != null && actorUserId.equals(user.getUuid()) && targetStatus != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("You cannot suspend or disable your own platform admin account.");
        }

        if (user.getStatus() == UserStatus.ACTIVE
                && targetStatus != UserStatus.ACTIVE
                && countActivePlatformAdminsExcluding(user.getUuid()) == 0) {
            throw new IllegalStateException("At least one active platform admin account must remain.");
        }

        user.setStatus(targetStatus);
        if (targetStatus == UserStatus.ACTIVE) {
            if (!user.isVerified()) {
                user.setVerified(true);
            }
        } else {
            authService.revokeAllActiveUserTokens(user);
            user.setLockedUntil(null);
        }
        UserEntity saved = userRepository.save(user);
        return toResponse(saved, userProfileRepository.findById(saved.getUuid()).orElse(null));
    }

    @Transactional
    public PlatformAdminInviteResponse resendOnboarding(UUID userId) {
        UserEntity user = requirePlatformAdmin(userId);
        boolean onboardingEmailSent = true;
        String message = "Password setup email sent.";
        try {
            authService.initiatePasswordReset(user.getEmail());
        } catch (MessagingException ex) {
            onboardingEmailSent = false;
            message = "Password setup email could not be sent.";
        }

        return PlatformAdminInviteResponse.builder()
                .admin(toResponse(user, userProfileRepository.findById(user.getUuid()).orElse(null)))
                .onboardingEmailSent(onboardingEmailSent)
                .message(message)
                .build();
    }

    private UserEntity requirePlatformAdmin(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Platform admin not found"));
        if (!isPlatformAdmin(user)) {
            throw new EntityNotFoundException("Platform admin not found");
        }
        return user;
    }

    private boolean isPlatformAdmin(UserEntity user) {
        return user.getRoles() != null && user.getRoles().stream()
                .map(Role::getRoleName)
                .anyMatch(PLATFORM_ADMIN_ROLE_NAMES::contains);
    }

    private long countActivePlatformAdminsExcluding(UUID excludedUserId) {
        return userRepository.findAllByRoleNames(PLATFORM_ADMIN_ROLE_NAMES).stream()
                .filter(this::isPlatformAdmin)
                .filter(user -> !user.getUuid().equals(excludedUserId))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .count();
    }

    private UserStatus parseManagedStatus(String requestedStatus) {
        String normalized = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACTIVE" -> UserStatus.ACTIVE;
            case "SUSPENDED" -> UserStatus.SUSPENDED;
            case "DISABLED" -> UserStatus.DISABLED;
            default -> throw new IllegalArgumentException("Unsupported platform admin status: " + requestedStatus);
        };
    }

    private PlatformAdminUserResponse toResponse(UserEntity user, UserProfileEntity profile) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .sorted()
                .toList();

        return PlatformAdminUserResponse.builder()
                .userId(user.getUuid())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .roles(roles)
                .breakGlass(roles.contains(BREAK_GLASS_ROLE))
                .verified(user.isVerified())
                .twoFactorEnabled(profile != null && profile.isTwoFactorEnabled())
                .mustChangePassword(user.isMustChangePassword())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        return normalized;
    }

    private String generateTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
        StringBuilder builder = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
