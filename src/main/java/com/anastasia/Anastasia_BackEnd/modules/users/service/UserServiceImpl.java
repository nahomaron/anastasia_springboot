package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalePreferenceService;
import com.anastasia.Anastasia_BackEnd.common.utils.PhoneNumberUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.UsersMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberTransferService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserResponseIDs;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MembershipSummary;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MemberTransferResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.BackupCodesResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateRecoveryEmailRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateUserPreferencesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateTwoFactorRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UpdateUserProfileRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserPreferencesResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.VerifyRecoveryEmailCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.VerifyTotpSetupRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TotpSetupResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantInviteResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantMembershipAction;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserRowResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUsersMetricsResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUsersPageResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserMembershipsResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserProfileResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserPreferencesEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserProfileEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserTwoFactorBackupCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserPreferencesRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserTwoFactorBackupCodeRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserProfileRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.security.TotpUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UsersMapper usersMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final ChildRepository childRepository;
    private final TenantRepository tenantRepository;
    private final MemberTransferService memberTransferService;
    private final MembershipCardService membershipCardService;
    private final AuthService authService;
    private final EmailNotificationService emailNotificationService;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserTwoFactorBackupCodeRepository backupCodeRepository;
    private final UserRecoveryEmailVerificationService recoveryEmailVerificationService;
    private final TokenRepository tokenRepository;
    private final LocalePreferenceService localePreferenceService;
    private final LocalizedMessageService messageService;
    private final TenantUserAccessPolicy accessPolicy;

    private static final String BACKUP_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int BACKUP_CODES_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    @Override
    public UserEntity convertToEntity(UserDTO userDTO) {
        return usersMapper.userDTOToUserEntity(userDTO);
    }

    @Override
    public UserDTO convertToDTO(UserEntity userEntity) {
//        System.out.println("Converting UserEntity to DTO: " + userEntity);
        return usersMapper.userEntityToUserDTO(userEntity);
    }


    @Cacheable(value = "users_all", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Page<UserResponseIDs> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toIdResponse);
    }

    @Cacheable(value = "users", key = "#userId")
    @Override
    public Optional<SimpleUserDTO> findOne(UUID userId) {
        return userRepository.findById(userId).map(this::toSimpleUserDTO);
    }

    @Override
    public Optional<UserEntity> findEntity(UUID userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public UserProfileResponse getCurrentUserProfile() {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);
        return toUserProfileResponse(user, profile);
    }

    @Transactional
    @Override
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);

        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isBlank()) {
                throw new IllegalArgumentException(messageService.get("user.profile.fullName.empty", "Full name cannot be empty"));
            }
            user.setFullName(fullName);
        }

        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(messageService.get("validation.user.profile.dateOfBirth.past", "Date of birth must be in the past"));
            }
            profile.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null) {
            profile.setGender(trimToNull(request.getGender()));
        }
        if (request.getLocation() != null) {
            profile.setLocation(trimToNull(request.getLocation()));
        }
        if (request.getProfileAvatar() != null) {
            ImageAssetDTO profileAvatar = request.getProfileAvatar();
            String imageUrl = trimToNull(profileAvatar.getImageUrl());
            profile.setProfileImageUrl(imageUrl);
            if (imageUrl != null) {
                ImageAssetEntity avatar = ImageAssetEntity.builder()
                        .imageUrl(imageUrl)
                        .imageSize(trimToNull(profileAvatar.getImageSize()))
                        .imageAssetType(ImageAssetType.USER)
                        .ownerId(user.getUuid())
                        .build();
                avatar = imageAssetRepository.save(avatar);
                user.setProfileAvatar(avatar);
            } else {
                user.setProfileAvatar(null);
            }
        } else if (request.getProfileImageUrl() != null) {
            String imageUrl = trimToNull(request.getProfileImageUrl());
            profile.setProfileImageUrl(imageUrl);
            if (imageUrl != null) {
                ImageAssetEntity avatar = ImageAssetEntity.builder()
                        .imageUrl(imageUrl)
                        .imageAssetType(ImageAssetType.USER)
                        .ownerId(user.getUuid())
                        .build();
                avatar = imageAssetRepository.save(avatar);
                user.setProfileAvatar(avatar);
            } else {
                user.setProfileAvatar(null);
            }
        }
        if (request.getPhoneNumber() != null) {
            String normalizedPhone = trimToNull(PhoneNumberUtils.normalize(request.getPhoneNumber()));
            if (normalizedPhone != null && !normalizedPhone.equals(profile.getPhoneNumber())) {
                profile.setPhoneVerified(false);
            }
            profile.setPhoneNumber(normalizedPhone);
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
        return toUserProfileResponse(user, profile);
    }

    @Transactional
    @Override
    public UserProfileResponse updateCurrentUserRecoveryEmail(UpdateRecoveryEmailRequest request) {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);

        String recoveryEmail = request.getRecoveryEmail() == null
                ? null
                : request.getRecoveryEmail().trim().toLowerCase(Locale.ROOT);

        if (recoveryEmail == null || recoveryEmail.isBlank()) {
            throw new IllegalArgumentException(messageService.get("validation.user.recoveryEmail.required", "Recovery email is required"));
        }
        if (recoveryEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException(messageService.get(
                    "user.recoveryEmail.mustDifferFromLogin",
                    "Recovery email must be different from login email"
            ));
        }

        profile.setRecoveryEmail(recoveryEmail);
        profile.setRecoveryEmailVerified(false);
        profile.setRecoveryEmailVerifiedAt(null);
        userProfileRepository.save(profile);
        return toUserProfileResponse(user, profile);
    }

    @Transactional
    @Override
    public void sendRecoveryEmailVerificationCode() {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);
        if (profile.getRecoveryEmail() == null || profile.getRecoveryEmail().isBlank()) {
            throw new IllegalStateException(messageService.get(
                    "user.recoveryEmail.requiredBeforeVerificationRequest",
                    "Set a recovery email before requesting verification."
            ));
        }
        recoveryEmailVerificationService.sendCode(profile.getRecoveryEmail());
    }

    @Transactional
    @Override
    public boolean verifyRecoveryEmailCode(VerifyRecoveryEmailCodeRequest request) {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);
        if (profile.getRecoveryEmail() == null || profile.getRecoveryEmail().isBlank()) {
            throw new IllegalStateException(messageService.get(
                    "user.recoveryEmail.requiredBeforeVerification",
                    "Set a recovery email before verifying it."
            ));
        }

        boolean verified = recoveryEmailVerificationService.verifyCode(profile.getRecoveryEmail(), request.getCode());
        if (verified) {
            profile.setRecoveryEmailVerified(true);
            profile.setRecoveryEmailVerifiedAt(Instant.now());
            userProfileRepository.save(profile);
        }
        return verified;
    }

    @Transactional
    @Override
    public UserProfileResponse updateCurrentUserTwoFactor(UpdateTwoFactorRequest request) {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);

        if (request.isEnabled()) {
            throw new IllegalStateException(messageService.get(
                    "user.twoFactor.enableViaSetupFlow",
                    "Use TOTP setup flow to enable two-factor authentication."
            ));
        }

        profile.setTwoFactorEnabled(false);
        profile.setTotpEnabledAt(null);
        profile.setTotpSecretBase32(null);
        profile.setTotpSetupAt(null);
        userProfileRepository.save(profile);
        backupCodeRepository.deleteByUserId(user.getUuid());
        return toUserProfileResponse(user, profile);
    }

    @Transactional(readOnly = true)
    @Override
    public UserPreferencesResponse getCurrentUserPreferences() {
        UserEntity user = getCurrentAuthenticatedUser();
        UserPreferencesEntity preferences = getOrCreatePreferences(user);
        return toUserPreferencesResponse(preferences);
    }

    @Transactional
    @Override
    public UserPreferencesResponse updateCurrentUserPreferences(UpdateUserPreferencesRequest request) {
        UserEntity user = getCurrentAuthenticatedUser();
        UserPreferencesEntity preferences = getOrCreatePreferences(user);

        if (request.getThemeMode() != null) {
            String theme = request.getThemeMode().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("SYSTEM", "LIGHT", "DARK").contains(theme)) {
                throw new IllegalArgumentException(messageService.get(
                        "validation.preferences.themeMode.invalid",
                        "Theme mode must be SYSTEM, LIGHT, or DARK."
                ));
            }
            preferences.setThemeMode(theme);
        }
        if (request.getLanguage() != null) {
            localePreferenceService.validateLanguage(request.getLanguage());
            preferences.setLanguage(localePreferenceService.normalizeLanguage(request.getLanguage()));
            if (request.getLocale() == null) {
                preferences.setLocale(localePreferenceService.normalizeLocale(request.getLanguage()));
            }
        }
        if (request.getLocale() != null) {
            localePreferenceService.validateLocale(request.getLocale());
            preferences.setLocale(localePreferenceService.normalizeLocale(request.getLocale()));
        }
        if (request.getDateFormat() != null) {
            preferences.setDateFormat(defaultIfBlank(request.getDateFormat(), "MMM d, yyyy"));
        }
        if (request.getFirstDayOfWeek() != null) {
            String firstDay = request.getFirstDayOfWeek().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("SUNDAY", "MONDAY").contains(firstDay)) {
                throw new IllegalArgumentException(messageService.get(
                        "validation.preferences.firstDayOfWeek.invalid",
                        "First day of week must be SUNDAY or MONDAY."
                ));
            }
            preferences.setFirstDayOfWeek(firstDay);
        }
        if (request.getReducedMotion() != null) {
            preferences.setReducedMotion(request.getReducedMotion());
        }
        if (request.getCompactUi() != null) {
            preferences.setCompactUi(request.getCompactUi());
        }
        if (request.getEmailNotifications() != null) {
            preferences.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getPushNotifications() != null) {
            preferences.setPushNotifications(request.getPushNotifications());
        }
        if (request.getMarketingNotifications() != null) {
            preferences.setMarketingNotifications(request.getMarketingNotifications());
        }
        if (request.getSharePresence() != null) {
            preferences.setSharePresence(request.getSharePresence());
        }
        if (request.getAnalyticsOptIn() != null) {
            preferences.setAnalyticsOptIn(request.getAnalyticsOptIn());
        }
        if (request.getAutoDetectLocation() != null) {
            preferences.setAutoDetectLocation(request.getAutoDetectLocation());
        }

        userPreferencesRepository.save(preferences);
        return toUserPreferencesResponse(preferences);
    }

    @Transactional
    @Override
    public TotpSetupResponse initiateTotpSetup() {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);

        if (profile.getRecoveryEmail() == null || profile.getRecoveryEmail().isBlank() || !profile.isRecoveryEmailVerified()) {
            throw new IllegalStateException(messageService.get(
                    "user.twoFactor.recoveryEmailRequired",
                    "Verify a recovery email before starting two-factor setup."
            ));
        }

        String secret = TotpUtils.generateSecretBase32();
        profile.setTotpSecretBase32(secret);
        profile.setTotpSetupAt(Instant.now());
        profile.setTwoFactorEnabled(false);
        profile.setTotpEnabledAt(null);
        userProfileRepository.save(profile);

        return TotpSetupResponse.builder()
                .secret(secret)
                .otpauthUri(TotpUtils.buildOtpauthUri("Anastasia", user.getEmail(), secret))
                .build();
    }

    @Transactional
    @Override
    public BackupCodesResponse verifyTotpSetup(VerifyTotpSetupRequest request) {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);

        String secret = trimToNull(profile.getTotpSecretBase32());
        if (secret == null) {
            throw new IllegalStateException(messageService.get("user.twoFactor.setupRequired", "Start TOTP setup before verifying."));
        }

        boolean valid = TotpUtils.verifyTotpCode(secret, request.getCode(), java.time.Instant.now(), 1);
        if (!valid) {
            throw new IllegalArgumentException(messageService.get("user.twoFactor.invalidTotp", "Invalid TOTP code."));
        }

        profile.setTwoFactorEnabled(true);
        profile.setTotpEnabledAt(Instant.now());
        userProfileRepository.save(profile);

        return generateAndStoreBackupCodes(user);
    }

    @Transactional
    @Override
    public BackupCodesResponse regenerateBackupCodes() {
        UserEntity user = getCurrentAuthenticatedUser();
        UserProfileEntity profile = getOrCreateProfile(user);
        if (!profile.isTwoFactorEnabled() || trimToNull(profile.getTotpSecretBase32()) == null) {
            throw new IllegalStateException(messageService.get(
                    "user.twoFactor.enableBeforeBackupCodes",
                    "Enable TOTP before generating backup codes."
            ));
        }
        return generateAndStoreBackupCodes(user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserSessionResponse> listCurrentUserSessions(String currentBearerToken) {
        UserEntity user = getCurrentAuthenticatedUser();
        List<Token> tokens = tokenRepository.findByUserUuidAndTokenTypeOrderByIdDesc(user.getUuid(), TokenType.BEARER);
        return tokens.stream()
                .map(token -> UserSessionResponse.builder()
                        .sessionId(token.getId())
                        .tokenType(token.getTokenType().name())
                        .createdAt(token.getCreatedAt())
                        .expiresAt(token.getExpiresAt())
                        .revoked(token.isRevoked())
                        .expired(token.isExpired())
                        .current(currentBearerToken != null && currentBearerToken.equals(token.getToken()))
                        .build())
                .toList();
    }

    @Transactional
    @Override
    public void revokeCurrentUserSession(Integer sessionId) {
        UserEntity user = getCurrentAuthenticatedUser();
        Token token = tokenRepository.findByIdAndUserUuid(sessionId, user.getUuid())
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        if (token.getSessionId() != null && !token.getSessionId().isBlank()) {
            revokeSessionFamily(user.getUuid(), token.getSessionId());
            return;
        }
        tokenRepository.revokeTokenByIdAndUserUuid(token.getId(), user.getUuid(), Instant.now());
    }

    @Transactional
    @Override
    public void revokeOtherCurrentUserSessions(String currentBearerToken) {
        UserEntity user = getCurrentAuthenticatedUser();
        List<Token> tokens = tokenRepository.findAllActiveTokensByUserUuid(user.getUuid());
        Set<String> preservedSessionIds = new HashSet<>();
        Set<String> sessionIdsToRevoke = new HashSet<>();
        Set<Integer> standaloneTokenIdsToRevoke = new HashSet<>();
        for (Token token : tokens) {
            if (currentBearerToken != null && currentBearerToken.equals(token.getToken())) {
                if (token.getSessionId() != null && !token.getSessionId().isBlank()) {
                    preservedSessionIds.add(token.getSessionId());
                }
                continue;
            }
            if (token.getSessionId() != null && preservedSessionIds.contains(token.getSessionId())) {
                continue;
            }
            if (token.getSessionId() != null && !token.getSessionId().isBlank()) {
                sessionIdsToRevoke.add(token.getSessionId());
            } else {
                standaloneTokenIdsToRevoke.add(token.getId());
            }
        }

        Instant now = Instant.now();
        sessionIdsToRevoke.forEach(sessionId -> tokenRepository.revokeAllActiveTokensByUserUuidAndSessionId(user.getUuid(), sessionId, now));
        standaloneTokenIdsToRevoke.forEach(tokenId -> tokenRepository.revokeTokenByIdAndUserUuid(tokenId, user.getUuid(), now));
    }

    private void revokeSessionFamily(UUID userId, String sessionFamilyId) {
        tokenRepository.revokeAllActiveTokensByUserUuidAndSessionId(userId, sessionFamilyId, Instant.now());
    }

    @Caching(
            put = {@CachePut(value = "users", key = "#result.uuid()")},
            evict = {
                    @CacheEvict(value = "users_all", keyGenerator = "tenantAwareKeyGenerator", allEntries = true),
                    @CacheEvict(value = "users_all_list", keyGenerator = "tenantAwareKeyGenerator", allEntries = true)
            }
    )
    @Override
    public SimpleUserDTO updateUserDetails(UserEntity userEntity, Principal connectedUser) {
//        var currentUser = (UserEntity) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        if (!(connectedUser instanceof Authentication)){
            throw new IllegalStateException(messageService.get("auth.user.authorizationInvalid", "Invalid user authorization"));
        }

        Authentication authentication = (Authentication) connectedUser;
        Object principal = authentication.getPrincipal();

        if(!(principal instanceof UserPrincipal)){
            throw new IllegalStateException(messageService.get("auth.user.principalInvalid", "Invalid user principal"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;

        return userRepository.findById(userPrincipal.getUserUuid()).map(existingUser -> {
            Optional.ofNullable(userEntity.getFullName()).ifPresent(existingUser::setFullName);
            Optional.ofNullable(userEntity.getEmail()).ifPresent(existingUser::setEmail);
            return toSimpleUserDTO(userRepository.save(existingUser));
        }).orElseThrow(() -> new RuntimeException("User doesn't exist"));
    }


    @Caching(
            evict = {
                    @CacheEvict(value = "users",
                            key = "#root.target.getCurrentUserId()"
                    )
            }
    )
    @Override
    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {
        if (!(connectedUser instanceof Authentication)) {
            throw new IllegalStateException(messageService.get("auth.user.authenticationInvalid", "Invalid user authentication"));
        }

        Authentication authentication = (Authentication) connectedUser;
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            throw new IllegalStateException(messageService.get("auth.user.principalInvalid", "Invalid user principal"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;

        // Fetch the UserEntity from the database using the UserPrincipal's ID or email
        UserEntity user = userRepository.findByEmail(userPrincipal.getUsername()) // or findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException(messageService.get("auth.changePassword.currentIncorrect", "Incorrect password provided"));
        }

        // Check if the new password matches confirmation
        if (!request.isPasswordMatch()) {
            throw new BadCredentialsException(messageService.get("auth.changePassword.mismatch", "Passwords do not match"));
        }

        // Update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#userId"
                    ),
                    @CacheEvict(value = "users_all",
                            keyGenerator = "tenantAwareKeyGenerator", allEntries = true),
                    @CacheEvict(value = "users_all_list",
                            keyGenerator = "tenantAwareKeyGenerator", allEntries = true)
            }
    )
    @Override
    public void assignRolesToUser(UUID userId, AssignRolesRequest request) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new IllegalStateException(messageService.get("tenant.context.missing", "Tenant ID is not set in the context"));
        }

        if (user.getTenantId() == null || !tenantId.equals(user.getTenantId())) {
            throw new EntityNotFoundException(messageService.get("user.access.notFoundInTenant", "User not found in current tenant"));
        }

        if (accessPolicy.isProtectedAccount(user)) {
            throw new IllegalArgumentException(messageService.get(
                    "user.access.protectedAccount",
                    "Protected tenant account cannot be modified"
            ));
        }

        List<Role> requestedRoles = roleRepository.findAllById(request.roleIds());
        Set<Long> resolvedIds = requestedRoles.stream().map(Role::getId).collect(Collectors.toSet());
        if (!resolvedIds.equals(new LinkedHashSet<>(request.roleIds()))) {
            throw new IllegalArgumentException(messageService.get("user.access.roles.invalid", "One or more requested roles were not found"));
        }

        List<Role> invalidRoles = requestedRoles.stream()
                .filter(role -> !accessPolicy.isAssignableThroughTenantAccess(role, tenantId))
                .toList();
        if (!invalidRoles.isEmpty()) {
            throw new IllegalArgumentException(messageService.get(
                    "user.access.roles.notAssignable",
                    "One or more requested roles cannot be assigned through tenant access"
            ));
        }

        user.setRoles(new LinkedHashSet<>(requestedRoles));

        userRepository.save(user);
    }

    @Cacheable(value = "users_all_list", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public List<UserResponseIDs> findAll() {
        return userRepository.findAll().stream().map(this::toIdResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<SimpleUserDTO> searchUsers(String query, Set<String> roles) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get("tenant.context.missing", "Tenant ID is not set in the context"));
        }
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) {
            return List.of();
        }
        if (roles == null || roles.isEmpty()) {
            return userRepository.searchByTenantId(tenantId, q);
        }
        return userRepository.searchByTenantIdAndRoles(tenantId, q, roles);
    }

    @Transactional(readOnly = true)
    @Override
    public TenantUsersPageResponse listTenantUsers(String query, String status, String role, int page, int size) {
        UUID tenantId = requireTenantId();
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "fullName"));

        Specification<UserEntity> spec = byTenant(tenantId)
                .and(searchByQuery(query))
                .and(filterByStatus(status))
                .and(filterByRole(role));

        Page<UserEntity> userPage = userRepository.findAll(spec, pageable);
        List<TenantUserRowResponse> items = userPage.getContent().stream()
                .map(this::toTenantUserRow)
                .toList();

        TenantUsersMetricsResponse metrics = computeTenantMetrics(tenantId);
        List<String> roles = userRepository.findByTenantId(tenantId).stream()
                .flatMap(user -> user.getRoles().stream())
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .toList();

        return TenantUsersPageResponse.builder()
                .items(items)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .sizeOptions(List.of(20, 50, 100, 200))
                .roles(roles)
                .metrics(metrics)
                .build();
    }

    @Transactional
    @Override
    public TenantInviteResponse inviteUserToTenant(String email) {
        UUID tenantId = requireTenantId();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException(messageService.get("validation.auth.email.required", "Email is required"));
        }

        Optional<UserEntity> existingInTenant = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, normalizedEmail);
        if (existingInTenant.isPresent()) {
            UserEntity user = existingInTenant.get();
            if (!user.isVerified()) {
                try {
                    authService.resendActivationEmail(user.getEmail());
                } catch (Exception e) {
                    throw new IllegalStateException(messageService.get("auth.activation.resendFailed", "Failed to resend activation email"));
                }
            } else {
                sendTenantInviteEmail(normalizedEmail);
            }
            return TenantInviteResponse.builder()
                    .email(normalizedEmail)
                    .existingUser(true)
                    .message(messageService.get("user.tenantInvite.sent", "Invitation email sent."))
                    .build();
        }

        // Prevent leaking or hijacking users from other tenants by email.
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(messageService.get(
                    "user.tenantInvite.emailBelongsToAnotherTenant",
                    "Email is already associated with another tenant."
            ));
        }

        sendTenantInviteEmail(normalizedEmail);
        return TenantInviteResponse.builder()
                .email(normalizedEmail)
                .existingUser(false)
                .message(messageService.get("user.tenantInvite.sent", "Invitation email sent."))
                .build();
    }

    @Transactional
    @Override
    public TenantUserRowResponse applyMembershipAction(UUID userId, TenantMembershipAction action) {
        UUID tenantId = requireTenantId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!tenantId.equals(user.getTenantId())) {
            throw new EntityNotFoundException("User not found in current tenant");
        }

        if (accessPolicy.isProtectedAccount(user)) {
            throw new IllegalArgumentException(messageService.get(
                    "user.membership.protectedAccount",
                    "Protected tenant account cannot be modified by membership actions."
            ));
        }

        switch (action) {
            case APPROVE, RESTORE -> {
                if (user.getMembership() != null) {
                    user.getMembership().setStatus(MemberStatus.ACTIVE.name());
                }
            }
            case DENY, SUSPEND -> {
                if (user.getMembership() != null) {
                    user.getMembership().setStatus(MemberStatus.NON_ACTIVE.name());
                    membershipCardService.revokeCardByMembershipNumber(
                            tenantId,
                            user.getMembership().getMembershipNumber(),
                            "Membership status changed to " + action.name());
                }
            }
        }

        UserEntity saved = userRepository.save(user);
        return toTenantUserRow(saved);
    }

    @Transactional
    @Override
    public MemberTransferResponse createMemberTransferRequest(UUID userId, UUID targetTenantId, String reason) {
        UUID actorTenantId = requireTenantId();
        UUID actorUserId = getCurrentUserId();
        MemberTransferRequestEntity request = memberTransferService.createTransferRequest(
                actorTenantId,
                userId,
                targetTenantId,
                actorUserId,
                reason
        );
        return toMemberTransferResponse(request);
    }

    @Transactional
    @Override
    public MemberTransferResponse approveMemberTransferRequest(UUID transferRequestId, String decisionNote) {
        UUID actorTenantId = requireTenantId();
        UUID actorUserId = getCurrentUserId();
        MemberTransferRequestEntity request = memberTransferService.approveTransferRequest(
                actorTenantId,
                transferRequestId,
                actorUserId,
                decisionNote
        );
        return toMemberTransferResponse(request);
    }

    @Transactional
    @Override
    public MemberTransferResponse rejectMemberTransferRequest(UUID transferRequestId, String decisionNote) {
        UUID actorTenantId = requireTenantId();
        UUID actorUserId = getCurrentUserId();
        MemberTransferRequestEntity request = memberTransferService.rejectTransferRequest(
                actorTenantId,
                transferRequestId,
                actorUserId,
                decisionNote
        );
        return toMemberTransferResponse(request);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#userId"
                    ),
                    @CacheEvict(value = "users_all", keyGenerator = "tenantAwareKeyGenerator", allEntries = true),
                    @CacheEvict(value = "users_all_list", keyGenerator = "tenantAwareKeyGenerator", allEntries = true)
            }
    )
    @Override
    public void deleteUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        tokenRepository.deleteAllByUserUuid(userId);

        userRepository.delete(user);
    }

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserUuid(); // or userPrincipal.getId();
        }
        throw new RuntimeException(messageService.get("auth.user.notAuthenticated", "No authenticated user found."));
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#root.target.getCurrentUserId()"),
                    @CacheEvict(value = "users_all", keyGenerator = "tenantAwareKeyGenerator", allEntries = true),
                    @CacheEvict(value = "users_all_list", keyGenerator = "tenantAwareKeyGenerator", allEntries = true)
            }
    )
    @Override
    public void updateProfileAvatar(ImageAssetDTO imageAssetDTO) {
        UUID userId = getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ImageAssetEntity avatar = ImageAssetEntity.builder()
                .imageUrl(imageAssetDTO.getImageUrl())
                .imageSize(imageAssetDTO.getImageSize())
                .imageAssetType(ImageAssetType.USER)
                .ownerId(user.getUuid())
                .build();

        avatar = imageAssetRepository.save(avatar);
        user.setProfileAvatar(avatar);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserMembershipsResponse getCurrentUserMemberships() {
        UUID userId = getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Adult_MemberEntity adultMembership = user.getMembership();
        MembershipSummary selfMembership = null;
        List<MembershipSummary> managedMemberships = new ArrayList<>();

        if (adultMembership != null) {
            selfMembership = toSummary(adultMembership, "SELF", true);

            Long parentId = adultMembership.getId();
            List<Child_MemberEntity> children = childRepository.findByFatherIdOrMotherId(parentId, parentId);
            for (Child_MemberEntity child : children) {
                boolean primaryGuardian = isPrimaryGuardian(child, parentId);
                managedMemberships.add(toSummary(child, "CHILD", primaryGuardian));
            }
        }

        return UserMembershipsResponse.builder()
                .selfMembership(selfMembership)
                .managedMemberships(managedMemberships)
                .build();
    }

    private boolean isPrimaryGuardian(Child_MemberEntity child, Long parentId) {
        if (child == null || parentId == null) {
            return false;
        }
        if (child.getFather() != null && parentId.equals(child.getFather().getId())) {
            return true;
        }
        return child.getMother() != null && parentId.equals(child.getMother().getId());
    }

    private MembershipSummary toSummary(BaseMember member, String relationshipToUser, boolean isPrimaryGuardian) {
        if (member == null) {
            return null;
        }

        String fullName = String.join(" ",
                nullToEmpty(member.getFirstName()),
                nullToEmpty(member.getFatherName()),
                nullToEmpty(member.getGrandFatherName())
        ).trim();

        String churchName = member.getChurch() != null ? member.getChurch().getChurchNameLocal() : null;

        return MembershipSummary.builder()
                .memberId(member.getMembershipNumber())
                .fullName(fullName.isBlank() ? null : fullName)
                .relationshipToUser(relationshipToUser)
                .status(mapMembershipStatus(member.getStatus()))
                .churchName(churchName)
                .isPrimaryGuardian(isPrimaryGuardian)
                .build();
    }

    private String mapMembershipStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "PENDING" -> "PENDING";
            case "APPROVED", "ACTIVE" -> "ACTIVE";
            case "NON_ACTIVE", "DECEASED" -> "TERMINATED";
            default -> status.toUpperCase();
        };
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private UserResponseIDs toIdResponse(UserEntity user) {
        return UserResponseIDs.builder()
                .uuid(user.getUuid())
                .build();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set in the context");
        }
        return tenantId;
    }

    private Specification<UserEntity> byTenant(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("affiliatedTenantId"), tenantId);
    }

    private Specification<UserEntity> searchByQuery(String queryText) {
        return (root, query, cb) -> {
            String q = queryText == null ? "" : queryText.trim().toLowerCase(Locale.ROOT);
            if (q.isBlank()) {
                return cb.conjunction();
            }

            var membershipJoin = root.join("membership", jakarta.persistence.criteria.JoinType.LEFT);
            String pattern = "%" + q + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(cb.coalesce(membershipJoin.get("membershipNumber"), "")), pattern)
            );
        };
    }

    private Specification<UserEntity> filterByStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
                return cb.conjunction();
            }
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            var membershipJoin = root.join("membership", jakarta.persistence.criteria.JoinType.LEFT);
            return switch (normalized) {
                case "ACTIVE" -> cb.and(
                        cb.equal(root.get("status"), UserStatus.ACTIVE),
                        membershipJoin.get("statusValue").in(MemberStatus.ACTIVE.name(), MemberStatus.APPROVED.name())
                );
                case "INVITED" -> cb.or(
                        cb.isNull(root.get("membershipId")),
                        cb.equal(root.get("status"), UserStatus.PENDING_VERIFICATION),
                        cb.equal(membershipJoin.get("statusValue"), MemberStatus.PENDING.name())
                );
                case "DISABLED" -> cb.or(
                        cb.equal(root.get("status"), UserStatus.DISABLED),
                        cb.equal(root.get("status"), UserStatus.SUSPENDED),
                        cb.equal(root.get("status"), UserStatus.DELETED),
                        membershipJoin.get("statusValue").in(MemberStatus.NON_ACTIVE.name(), MemberStatus.DECEASED.name())
                );
                case "LOCKED" -> cb.equal(root.get("status"), UserStatus.LOCKED);
                default -> cb.conjunction();
            };
        };
    }

    private Specification<UserEntity> filterByRole(String role) {
        return (root, query, cb) -> {
            if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
                return cb.conjunction();
            }
            query.distinct(true);
            var rolesJoin = root.join("roles", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.equal(rolesJoin.get("roleName"), role);
        };
    }

    private TenantUsersMetricsResponse computeTenantMetrics(UUID tenantId) {
        List<UserEntity> users = userRepository.findByTenantId(tenantId);
        Map<TenantUserStatus, Long> counters = new HashMap<>();
        counters.put(TenantUserStatus.ACTIVE, 0L);
        counters.put(TenantUserStatus.INVITED, 0L);
        counters.put(TenantUserStatus.DISABLED, 0L);
        counters.put(TenantUserStatus.LOCKED, 0L);

        for (UserEntity user : users) {
            TenantUserStatus status = resolveTenantUserStatus(user);
            counters.put(status, counters.get(status) + 1L);
        }

        return TenantUsersMetricsResponse.builder()
                .total(users.size())
                .active(counters.get(TenantUserStatus.ACTIVE))
                .invited(counters.get(TenantUserStatus.INVITED))
                .disabled(counters.get(TenantUserStatus.DISABLED))
                .locked(counters.get(TenantUserStatus.LOCKED))
                .build();
    }

    private TenantUserRowResponse toTenantUserRow(UserEntity user) {
        List<String> roles = accessPolicy.explicitRolesForTenant(user, user.getTenantId()).stream()
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .sorted()
                .toList();

        List<String> groups = user.getGroups().stream()
                .map(group -> group.getGroupName())
                .filter(name -> name != null && !name.isBlank())
                .sorted()
                .toList();

        String membershipId = user.getMembership() != null ? user.getMembership().getMembershipNumber() : null;

        return TenantUserRowResponse.builder()
                .id(user.getUuid())
                .tenantId(user.getTenantId())
                .username(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getProfileAvatar() != null ? user.getProfileAvatar().getImageUrl() : null)
                .roles(roles)
                .groups(groups)
                .membershipId(membershipId)
                .status(resolveTenantUserStatus(user))
                .createdAt(user.getCreatedAt())
                .protectedAccount(accessPolicy.isProtectedAccount(user))
                .protectedReason(accessPolicy.protectedReason(user))
                .build();
    }

    private TenantUserStatus resolveTenantUserStatus(UserEntity user) {
        if (user.isAccountLocked()) {
            return TenantUserStatus.LOCKED;
        }

        if (user.getStatus() == UserStatus.DISABLED
                || user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.DELETED) {
            return TenantUserStatus.DISABLED;
        }

        if (user.getMembership() == null || user.getMembership().getStatus() == null) {
            return TenantUserStatus.INVITED;
        }

        String memberStatus = user.getMembership().getStatus().toUpperCase(Locale.ROOT);
        return switch (memberStatus) {
            case "PENDING" -> TenantUserStatus.INVITED;
            case "NON_ACTIVE", "DECEASED" -> TenantUserStatus.DISABLED;
            case "APPROVED", "ACTIVE" -> TenantUserStatus.ACTIVE;
            default -> TenantUserStatus.ACTIVE;
        };
    }

    private void sendTenantInviteEmail(String email) {
        UUID tenantId = requireTenantId();
        String ownerName = tenantRepository.findById(tenantId)
                .map(t -> t.getOwnerName())
                .orElse("your church");

        Map<String, Object> properties = new HashMap<>();
        properties.put("username", "Member");
        properties.put("message_content",
                "You are invited to join " + ownerName + " on Anastasia. Complete your account registration at /auth/register.");

        emailNotificationService.sendEmail(
                email,
                "Anastasia membership invitation",
                EmailTemplateName.NOTIFICATION,
                properties
        );
    }

    private SimpleUserDTO toSimpleUserDTO(UserEntity user) {
        return SimpleUserDTO.builder()
                .uuid(user.getUuid())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private MemberTransferResponse toMemberTransferResponse(MemberTransferRequestEntity request) {
        return MemberTransferResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .fromTenantId(request.getFromTenant().getId())
                .toTenantId(request.getToTenant().getId())
                .status(request.getStatus())
                .reason(request.getReason())
                .decisionNote(request.getDecisionNote())
                .requestedByUserId(request.getRequestedByUserId())
                .decidedByUserId(request.getDecidedByUserId())
                .requestedAt(request.getRequestedAt())
                .decidedAt(request.getDecidedAt())
                .executedAt(request.getExecutedAt())
                .build();
    }

    private UserEntity getCurrentAuthenticatedUser() {
        UUID userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    private UserProfileEntity getOrCreateProfile(UserEntity user) {
        UUID userId = user.getUuid();
        Optional<UserProfileEntity> existing = userProfileRepository.findById(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return userProfileRepository.save(UserProfileEntity.builder()
                    .user(user)
                    .phoneVerified(false)
                    .twoFactorEnabled(false)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return userProfileRepository.findById(userId).orElseThrow(() -> ex);
        }
    }

    private UserPreferencesEntity getOrCreatePreferences(UserEntity user) {
        UUID userId = user.getUuid();
        Optional<UserPreferencesEntity> existing = userPreferencesRepository.findById(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return userPreferencesRepository.save(UserPreferencesEntity.builder()
                    .user(user)
                    .themeMode("SYSTEM")
                    .language(localePreferenceService.normalizeLanguage("en"))
                    .locale(localePreferenceService.normalizeLocale("en"))
                    .dateFormat("MMM d, yyyy")
                    .firstDayOfWeek("SUNDAY")
                    .emailNotifications(true)
                    .pushNotifications(true)
                    .marketingNotifications(false)
                    .sharePresence(true)
                    .analyticsOptIn(true)
                    .autoDetectLocation(true)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return userPreferencesRepository.findById(userId).orElseThrow(() -> ex);
        }
    }

    private UserProfileResponse toUserProfileResponse(UserEntity user, UserProfileEntity profile) {
        long backupCodesRemaining = backupCodeRepository.countUnusedByUserId(user.getUuid());
        ImageAssetDTO profileAvatar = user.getProfileAvatar() != null
                ? ImageAssetDTO.builder()
                .imageUrl(user.getProfileAvatar().getImageUrl())
                .imageSize(user.getProfileAvatar().getImageSize())
                .build()
                : (profile.getProfileImageUrl() != null
                ? ImageAssetDTO.builder().imageUrl(profile.getProfileImageUrl()).build()
                : null);
        return UserProfileResponse.builder()
                .userId(user.getUuid())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .location(profile.getLocation())
                .phoneNumber(profile.getPhoneNumber())
                .phoneVerified(profile.isPhoneVerified())
                .recoveryEmail(profile.getRecoveryEmail())
                .recoveryEmailVerified(profile.isRecoveryEmailVerified())
                .profileAvatar(profileAvatar)
                .profileImageUrl(profile.getProfileImageUrl() != null
                        ? profile.getProfileImageUrl()
                        : (user.getProfileAvatar() != null ? user.getProfileAvatar().getImageUrl() : null))
                .twoFactorEnabled(profile.isTwoFactorEnabled())
                .totpConfigured(profile.getTotpSecretBase32() != null && !profile.getTotpSecretBase32().isBlank())
                .backupCodesRemaining(backupCodesRemaining)
                .build();
    }

    private UserPreferencesResponse toUserPreferencesResponse(UserPreferencesEntity preferences) {
        return UserPreferencesResponse.builder()
                .userId(preferences.getUserId())
                .themeMode(preferences.getThemeMode())
                .locale(preferences.getLocale())
                .dateFormat(preferences.getDateFormat())
                .firstDayOfWeek(preferences.getFirstDayOfWeek())
                .reducedMotion(preferences.isReducedMotion())
                .compactUi(preferences.isCompactUi())
                .emailNotifications(preferences.isEmailNotifications())
                .pushNotifications(preferences.isPushNotifications())
                .marketingNotifications(preferences.isMarketingNotifications())
                .sharePresence(preferences.isSharePresence())
                .analyticsOptIn(preferences.isAnalyticsOptIn())
                .autoDetectLocation(preferences.isAutoDetectLocation())
                .build();
    }

    private BackupCodesResponse generateAndStoreBackupCodes(UserEntity user) {
        backupCodeRepository.deleteByUserId(user.getUuid());

        List<String> codes = new ArrayList<>();
        List<UserTwoFactorBackupCodeEntity> codeEntities = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            String code = generateBackupCode();
            codes.add(code);
            codeEntities.add(UserTwoFactorBackupCodeEntity.builder()
                    .user(user)
                    .codeHash(passwordEncoder.encode(code))
                    .createdAt(Instant.now())
                    .build());
        }

        backupCodeRepository.saveAll(codeEntities);
        return BackupCodesResponse.builder().codes(codes).build();
    }

    private String generateBackupCode() {
        StringBuilder raw = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            int idx = SECURE_RANDOM.nextInt(BACKUP_CODE_ALPHABET.length());
            raw.append(BACKUP_CODE_ALPHABET.charAt(idx));
        }
        String compact = raw.toString();
        return compact.substring(0, 4) + "-" + compact.substring(4);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

}
