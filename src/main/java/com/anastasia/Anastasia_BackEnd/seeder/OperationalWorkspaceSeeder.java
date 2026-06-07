package com.anastasia.Anastasia_BackEnd.seeder;

import com.anastasia.Anastasia_BackEnd.common.utils.ChurchNumberUtils;
import com.anastasia.Anastasia_BackEnd.common.utils.PhoneNumberUtils;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.calendar.service.ChurchCalendarSeedService;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetVisibility;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingInterval;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WorkspaceInitializationMode;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantWorkspaceLifecycleService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserPreferencesEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserProfileEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserPreferencesRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserProfileRepository;
import com.anastasia.Anastasia_BackEnd.seeder.seeders.RoleAndPermissionSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class OperationalWorkspaceSeeder {

    private static final SubscriptionPlan DEFAULT_PLAN = SubscriptionPlan.ENTERPRISE;
    private static final int OWNER_UPSERT_MAX_ATTEMPTS = 5;

    private final OperationalWorkspaceSeedingProperties properties;
    private final RoleAndPermissionSeeder roleAndPermissionSeeder;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final TenantWorkspaceLifecycleService tenantWorkspaceLifecycleService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final ChurchCalendarSeedService churchCalendarSeedService;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final TokenRepository tokenRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedOperationalWorkspace() {
        if (!properties.isEnabled()) {
            return;
        }

        validateConfiguration();
        roleAndPermissionSeeder.seedPermissions();
        roleAndPermissionSeeder.seedDefaultRoles();

        TenantEntity tenant = resolveOrCreateTenant();
        UserEntity owner = resolveOrCreateOwner(tenant);

        tenant = upsertTenant(tenant);
        owner = upsertOwner(owner, tenant);
        entityManager.clear();
        tenant = resolveCurrentTenantForUpsert(tenant);
        owner = resolveCurrentOwnerForUpsert(owner);
        if (properties.isReset() && tenant.getId() != null && owner.getUuid() != null) {
            tenantWorkspaceLifecycleService.clearWorkspaceContent(tenant.getId(), owner.getUuid());
            tokenRepository.revokeAllActiveTokensByUserUuid(owner.getUuid(), Instant.now());
        }
        upsertSubscription(tenant, owner);
        ChurchEntity church = upsertChurch(tenant);
        upsertAdminAssignment(tenant, owner);
        upsertOwnerProfile(owner, tenant);
        upsertOwnerPreferences(owner);
        churchCalendarSeedService.seedDefaults(tenant, church, owner);

        log.info("Operational workspace ensured for tenant slug {}", tenant.getSlug());
    }

    private void validateConfiguration() {
        requireText(properties.getTenantSlug(), "app.operational-seeding.tenant-slug");
        requireText(properties.getTenantDisplayName(), "app.operational-seeding.tenant-display-name");
        requireText(properties.getTenantOwnerName(), "app.operational-seeding.tenant-owner-name");
        requireText(properties.getTenantOwnerEmail(), "app.operational-seeding.tenant-owner-email");
        requireText(properties.getTenantOwnerPhone(), "app.operational-seeding.tenant-owner-phone");
        requireText(properties.getOwnerPassword(), "app.operational-seeding.owner-password");
        requireText(properties.getChurchName(), "app.operational-seeding.church-name");
        requireText(properties.getChurchEmail(), "app.operational-seeding.church-email");
        requireText(properties.getChurchPhone(), "app.operational-seeding.church-phone");
        requireText(properties.getChurchAvatarUrl(), "app.operational-seeding.church-avatar-url");
        requireText(properties.getChurchGpsLocation(), "app.operational-seeding.church-gps-location");
    }

    private TenantEntity resolveOrCreateTenant() {
        String slug = normalizedSlug(properties.getTenantSlug());
        Optional<TenantEntity> existingBySlug = tenantRepository.findBySlug(slug);
        if (existingBySlug.isPresent()) {
            return existingBySlug.get();
        }

        tenantRepository.findByOwnerEmailIgnoreCase(normalizedEmail(properties.getTenantOwnerEmail()))
                .filter(tenant -> !slug.equals(tenant.getSlug()))
                .ifPresent(tenant -> {
                    throw new IllegalStateException("Operational owner email already belongs to tenant slug " + tenant.getSlug());
                });

        return TenantEntity.builder().build();
    }

    private UserEntity resolveOrCreateOwner(TenantEntity tenant) {
        String ownerEmail = normalizedEmail(properties.getTenantOwnerEmail());
        Optional<UserEntity> existing = userRepository.findByEmailIgnoreCase(ownerEmail);
        if (existing.isPresent()) {
            UserEntity user = existing.get();
            if (user.getAffiliatedTenantId() != null
                    && (tenant.getId() == null || !tenant.getId().equals(user.getAffiliatedTenantId()))) {
                throw new IllegalStateException("Operational owner email is already linked to another tenant");
            }
            return user;
        }
        return UserEntity.builder().build();
    }

    private TenantEntity upsertTenant(TenantEntity tenant) {
        Instant now = Instant.now();
        tenant.setDisplayName(properties.getTenantDisplayName().trim());
        tenant.setSlug(normalizedSlug(properties.getTenantSlug()));
        tenant.setTenantType(TenantType.CHURCH);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setOwnerName(properties.getTenantOwnerName().trim());
        tenant.setOwnerEmail(normalizedEmail(properties.getTenantOwnerEmail()));
        tenant.setPhoneNumber(PhoneNumberUtils.normalize(properties.getTenantOwnerPhone()));
        tenant.setPhoneVerified(true);
        tenant.setPhoneVerifiedAt(now);
        tenant.setDefaultTimezone(defaultIfBlank(properties.getTenantTimezone(), "America/New_York"));
        tenant.setDefaultLocale(defaultIfBlank(properties.getTenantLocale(), "en"));
        tenant.setCountryCode(defaultIfBlank(properties.getTenantCountryCode(), "US"));
        tenant.setBillingEmail(normalizedEmail(defaultIfBlank(properties.getTenantBillingEmail(), properties.getTenantOwnerEmail())));
        tenant.setWorkspaceInitializationMode(WorkspaceInitializationMode.EMPTY);
        tenant.setDemoTemplate(false);
        tenant.setDemoWorkspace(false);
        tenant.setActivatedAt(now);
        tenant.setSuspendedAt(null);
        tenant.setDeactivatedAt(null);
        tenant.setClosedAt(null);
        tenant.setScheduledPurgeAt(null);
        tenant.setPurgedAt(null);
        tenant.setArchiveScheduledAt(null);
        tenant.setArchivedAt(null);
        tenant.setSuspensionReason(null);
        tenant.setDeletedAt(null);
        return tenantRepository.saveAndFlush(tenant);
    }

    private void upsertSubscription(TenantEntity tenant, UserEntity owner) {
        Instant now = Instant.now();
        TenantSubscriptionEntity subscription = tenant.getSubscription();
        if (subscription == null) {
            subscription = TenantSubscriptionEntity.builder().build();
        }

        subscription.setPlan(DEFAULT_PLAN);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setBillingInterval(BillingInterval.MONTHLY);
        subscription.setProvider(BillingProvider.MANUAL);
        subscription.setTrialStartAt(null);
        subscription.setTrialEndAt(null);
        subscription.setCurrentPeriodStartAt(now);
        subscription.setCurrentPeriodEndAt(now.plusSeconds(365L * 24 * 60 * 60));
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCanceledAt(null);
        subscription.setStartedAt(subscription.getStartedAt() == null ? now : subscription.getStartedAt());
        subscription.setEndedAt(null);
        subscription.setPausedAt(null);
        subscription.setResumedAt(now);
        subscription.setLastPaymentAt(now);
        subscription.setGracePeriodEndsAt(null);
        subscription.setPendingPlan(null);
        subscription.setPendingPlanEffectiveAt(null);
        subscription.setStatusChangedAt(now);
        subscription.setStatusChangeReason("Operational workspace bootstrap");
        subscription.setCreatedByUserId(owner.getUuid());
        subscription.setUpdatedByUserId(owner.getUuid());
        subscription.setDeletedAt(null);
        tenant.assignSubscription(subscription);
        tenantRepository.saveAndFlush(tenant);
    }

    private ChurchEntity upsertChurch(TenantEntity tenant) {
        ChurchEntity church = churchRepository.findByTenantId(tenant.getId())
                .orElseGet(ChurchEntity::new);

        church.setTenant(tenant);
        church.setStatus(ChurchStatus.ACTIVE);
        church.setChurchNumber(StringUtils.hasText(church.getChurchNumber()) ? church.getChurchNumber() : generateUniqueChurchNumber(properties.getChurchName(), 5));
        church.setChurchName(properties.getChurchName().trim());
        church.setPrefix(defaultIfBlank(properties.getChurchPrefix(), "St."));
        church.setPrefixLocal(defaultIfBlank(properties.getChurchPrefixLocal(), properties.getChurchPrefix()));
        church.setChurchNameLocal(defaultIfBlank(properties.getChurchNameLocal(), properties.getChurchName()));
        church.setAddress(Address.builder()
                .addressLine1(properties.getChurchAddressLine1().trim())
                .addressLine2(trimToNull(properties.getChurchAddressLine2()))
                .city(properties.getChurchCity().trim())
                .stateProvince(properties.getChurchStateProvince().trim())
                .postalCode(properties.getChurchPostalCode().trim())
                .country(properties.getChurchCountry().trim())
                .build());
        church.setNeighborhood(properties.getChurchNeighborhood().trim());
        church.setNeighborhoodLocal(defaultIfBlank(properties.getChurchNeighborhoodLocal(), properties.getChurchNeighborhood()));
        church.setDiocese(properties.getChurchDiocese().trim());
        church.setDioceseLocal(defaultIfBlank(properties.getChurchDioceseLocal(), properties.getChurchDiocese()));
        church.setEmail(normalizedEmail(properties.getChurchEmail()));
        church.setPhone(PhoneNumberUtils.normalize(properties.getChurchPhone()));
        church.setTimezone(defaultIfBlank(properties.getChurchTimezone(), "America/New_York"));
        church.setLocale(defaultIfBlank(properties.getChurchLocale(), "en-US"));
        church.setDenomination(defaultIfBlank(properties.getChurchDenomination(), "Orthodox"));
        church.setDescription(properties.getChurchDescription().trim());
        church.setDescriptionLocal(defaultIfBlank(properties.getChurchDescriptionLocal(), properties.getChurchDescription()));
        church.setUsesOurServices(true);
        church.setGpsLocation(properties.getChurchGpsLocation().trim());
        church.setLatitude(properties.getChurchLatitude());
        church.setLongitude(properties.getChurchLongitude());
        church.setWebsite(properties.getChurchWebsite().trim());
        church.setInstagram(properties.getChurchInstagram().trim());
        church.setYoutube(properties.getChurchYoutube().trim());
        church.setFacebook(properties.getChurchFacebook().trim());
        church.setActivatedAt(Instant.now());
        church.setDeactivatedAt(null);
        church.setDeletedAt(null);
        church.setProfilePicture(resolveOrCreateImageAsset(
                tenant.getId(),
                tenant.getId(),
                ImageAssetType.CHURCH,
                properties.getChurchAvatarUrl(),
                ImageAssetVisibility.PUBLIC
        ));

        ChurchEntity savedChurch = churchRepository.saveAndFlush(church);
        tenant.assignChurch(savedChurch);
        tenantRepository.saveAndFlush(tenant);
        return savedChurch;
    }

    private UserEntity upsertOwner(UserEntity owner, TenantEntity tenant) {
        UserEntity currentOwner = owner;
        ObjectOptimisticLockingFailureException lastFailure = null;

        for (int attempt = 1; attempt <= OWNER_UPSERT_MAX_ATTEMPTS; attempt++) {
            try {
                return persistOwnerInNewTransaction(currentOwner, tenant);
            } catch (ObjectOptimisticLockingFailureException ex) {
                lastFailure = ex;
                currentOwner = resolveCurrentOwnerForUpsert(currentOwner);
                log.warn(
                        "Retrying operational owner upsert after optimistic locking failure for tenant {} (attempt {}/{})",
                        tenant.getSlug(),
                        attempt,
                        OWNER_UPSERT_MAX_ATTEMPTS,
                        ex
                );
            }
        }

        if (currentOwner != null && currentOwner.getUuid() != null) {
            log.error(
                    "Operational owner upsert remained contended for tenant {} after {} attempts; continuing startup with the persisted owner state",
                    tenant.getSlug(),
                    OWNER_UPSERT_MAX_ATTEMPTS,
                    lastFailure
            );
            return currentOwner;
        }

        throw lastFailure;
    }

    private UserEntity persistOwnerInNewTransaction(UserEntity owner, TenantEntity tenant) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> persistOwner(owner, tenant));
    }

    private UserEntity persistOwner(UserEntity owner, TenantEntity tenant) {
        Instant now = Instant.now();
        owner.setFullName(properties.getTenantOwnerName().trim());
        owner.setEmail(normalizedEmail(properties.getTenantOwnerEmail()));
        owner.setPassword(passwordEncoder.encode(properties.getOwnerPassword()));
        owner.setPhoneNumber(PhoneNumberUtils.normalize(properties.getTenantOwnerPhone()));
        owner.setEmailVerifiedAt(now);
        owner.setPhoneVerifiedAt(now);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setUserType(UserType.TENANT);
        owner.setMustChangePassword(false);
        owner.setTemporaryPasswordIssuedAt(null);
        owner.setLockedAt(null);
        owner.setLockedUntil(null);
        owner.setFailedLoginAttempts(0);
        owner.setDeletedAt(null);
        owner.setTimezone(defaultIfBlank(properties.getTenantTimezone(), "America/New_York"));
        owner.assignAffiliatedTenant(tenant);
        owner.setRoles(resolveOwnerRoles());
        UserEntity savedOwner = userRepository.saveAndFlush(owner);
        savedOwner.setProfileAvatar(resolveOrCreateImageAsset(
                tenant.getId(),
                savedOwner.getUuid(),
                ImageAssetType.USER,
                properties.getOwnerAvatarUrl(),
                ImageAssetVisibility.PRIVATE
        ));
        return userRepository.saveAndFlush(savedOwner);
    }

    private UserEntity resolveCurrentOwnerForUpsert(UserEntity owner) {
        if (owner != null && owner.getUuid() != null) {
            return userRepository.findById(owner.getUuid())
                    .orElseGet(() -> userRepository.findByEmailIgnoreCase(normalizedEmail(properties.getTenantOwnerEmail()))
                            .orElseGet(() -> UserEntity.builder().build()));
        }

        return userRepository.findByEmailIgnoreCase(normalizedEmail(properties.getTenantOwnerEmail()))
                .orElseGet(() -> UserEntity.builder().build());
    }

    private TenantEntity resolveCurrentTenantForUpsert(TenantEntity tenant) {
        if (tenant != null && tenant.getId() != null) {
            return tenantRepository.findById(tenant.getId())
                    .orElseGet(() -> tenantRepository.findBySlug(normalizedSlug(properties.getTenantSlug()))
                            .orElseGet(() -> TenantEntity.builder().build()));
        }

        return tenantRepository.findBySlug(normalizedSlug(properties.getTenantSlug()))
                .orElseGet(() -> TenantEntity.builder().build());
    }

    private void upsertAdminAssignment(TenantEntity tenant, UserEntity owner) {
        TenantAdminAssignmentEntity assignment = tenantAdminAssignmentRepository
                .findByTenant_IdAndUserId(tenant.getId(), owner.getUuid())
                .orElseGet(TenantAdminAssignmentEntity::new);

        assignment.setTenant(tenant);
        assignment.setUserId(owner.getUuid());
        assignment.setRole(TenantRole.PRIMARY_ADMIN);
        assignment.setStatus(MembershipStatus.ACTIVE);
        assignment.setBillingContact(true);
        assignment.setCreatedByUserId(owner.getUuid());
        assignment.setUpdatedByUserId(owner.getUuid());
        tenantAdminAssignmentRepository.saveAndFlush(assignment);
    }

    private void upsertOwnerProfile(UserEntity owner, TenantEntity tenant) {
        UserProfileEntity profile = userProfileRepository.findById(owner.getUuid())
                .orElseGet(UserProfileEntity::new);
        profile.setUser(owner);
        profile.setLocation(defaultIfBlank(properties.getOwnerProfileLocation(), "New York, USA"));
        profile.setPhoneNumber(PhoneNumberUtils.normalize(properties.getTenantOwnerPhone()));
        profile.setPhoneVerified(true);
        profile.setRecoveryEmail(normalizedEmail(properties.getTenantBillingEmail()));
        profile.setRecoveryEmailVerified(false);
        profile.setRecoveryEmailVerifiedAt(null);
        profile.setProfileImageUrl(resolveOrCreateImageAsset(
                tenant.getId(),
                owner.getUuid(),
                ImageAssetType.USER,
                properties.getOwnerAvatarUrl(),
                ImageAssetVisibility.PRIVATE
        ).getImageUrl());
        profile.setTwoFactorEnabled(false);
        profile.setTotpSecretBase32(null);
        profile.setTotpSetupAt(null);
        profile.setTotpEnabledAt(null);
        userProfileRepository.saveAndFlush(profile);
    }

    private void upsertOwnerPreferences(UserEntity owner) {
        UserPreferencesEntity preferences = userPreferencesRepository.findById(owner.getUuid())
                .orElseGet(UserPreferencesEntity::new);
        preferences.setUser(owner);
        preferences.setThemeMode(defaultIfBlank(preferences.getThemeMode(), "SYSTEM"));
        preferences.setLanguage(defaultIfBlank(preferences.getLanguage(), "en"));
        preferences.setLocale(defaultIfBlank(preferences.getLocale(), "en-US"));
        preferences.setDateFormat(defaultIfBlank(preferences.getDateFormat(), "MMM d, yyyy"));
        preferences.setFirstDayOfWeek(defaultIfBlank(preferences.getFirstDayOfWeek(), "SUNDAY"));
        preferences.setEmailNotifications(true);
        preferences.setPushNotifications(true);
        preferences.setMarketingNotifications(false);
        preferences.setSharePresence(true);
        preferences.setAnalyticsOptIn(true);
        preferences.setAutoDetectLocation(true);
        userPreferencesRepository.saveAndFlush(preferences);
    }

    private ImageAssetEntity resolveOrCreateImageAsset(UUID tenantId,
                                                       UUID ownerId,
                                                       ImageAssetType type,
                                                       String imageUrl,
                                                       ImageAssetVisibility visibility) {
        ImageAssetEntity asset = imageAssetRepository
                .findByTenantIdAndOwnerIdAndImageAssetTypeAndDeletedAtIsNull(tenantId, ownerId, type)
                .orElseGet(ImageAssetEntity::new);
        asset.setTenantId(tenantId);
        asset.setOwnerId(ownerId);
        asset.setImageAssetType(type);
        asset.setImageUrl(imageUrl.trim());
        asset.setVisibility(visibility);
        asset.setDeletedAt(null);
        return imageAssetRepository.saveAndFlush(asset);
    }

    private Set<Role> resolveOwnerRoles() {
        Role ownerRole = roleRepository.findByRoleName("OWNER")
                .orElseThrow(() -> new IllegalStateException("OWNER role not found"));
        Role primaryAdminRole = roleRepository.findByRoleName("PRIMARY_ADMIN")
                .orElseThrow(() -> new IllegalStateException("PRIMARY_ADMIN role not found"));
        return new HashSet<>(Set.of(ownerRole, primaryAdminRole));
    }

    private String generateUniqueChurchNumber(String churchName, int length) {
        String prefix = ChurchNumberUtils.derivePrefix(churchName);
        String churchNumber;
        do {
            churchNumber = securityUtils.generateUniqueIDNumber(length, prefix);
        } while (churchRepository.existsByChurchNumber(churchNumber));
        return churchNumber;
    }

    private String normalizedSlug(String slug) {
        return requireText(slug, "tenant slug")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String normalizedEmail(String email) {
        return requireText(email, "email").trim().toLowerCase(Locale.ROOT);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(fieldName + " must be configured when operational seeding is enabled");
        }
        return value;
    }
}
