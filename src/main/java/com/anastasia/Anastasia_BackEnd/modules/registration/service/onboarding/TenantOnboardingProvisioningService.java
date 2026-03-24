package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.common.utils.ChurchNumberUtils;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingInterval;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionProviderLinkEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WorkspaceInitializationMode;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantOnboardingSessionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantOnboardingProvisioningService {

    private final TenantOnboardingSessionRepository onboardingSessionRepository;
    private final TenantRepository tenantRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ChurchMapper churchMapper;
    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final TenantPlanBillingCatalog billingCatalog;
    private final OnboardingEmailVerificationService onboardingEmailVerificationService;
    private final TenantDemoWorkspaceSeederService tenantDemoWorkspaceSeederService;
    private final LocalizedMessageService messageService;

    @Transactional
    public void finalizeProvisioningIfReady(UUID onboardingSessionId) {
        TenantOnboardingSessionEntity session = onboardingSessionRepository.findById(onboardingSessionId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "onboarding.session.notFound",
                        "Onboarding session not found"
                )));

        if (session.getStatus() == OnboardingSessionStatus.PROVISIONED) {
            return;
        }
        if (session.getStatus() != OnboardingSessionStatus.PAYMENT_CONFIRMED
                && session.getStatus() != OnboardingSessionStatus.CHECKOUT_SKIPPED) {
            return;
        }

        try {
            TenantEntity tenant = provisionTenant(session);
            session.setProvisionedTenantId(tenant.getId());
            session.setStatus(OnboardingSessionStatus.PROVISIONED);
            session.setProvisionedAt(Instant.now());
            session.setFailureReason(null);
            onboardingSessionRepository.save(session);
        } catch (RuntimeException ex) {
            session.setStatus(OnboardingSessionStatus.PROVISIONING_FAILED);
            session.setFailureReason(trimError(ex.getMessage()));
            onboardingSessionRepository.save(session);
            throw ex;
        }
    }

    private TenantEntity provisionTenant(TenantOnboardingSessionEntity session) {
        Optional<TenantEntity> byTenantId = session.getProvisionedTenantId() == null
                ? Optional.empty()
                : tenantRepository.findById(session.getProvisionedTenantId());
        if (byTenantId.isPresent()) {
            return byTenantId.get();
        }

        Optional<UserEntity> existingUser = userRepository.findByEmail(session.getOwnerEmail());
        Optional<TenantEntity> existingTenantByPhone = tenantRepository.findByPhoneNumber(session.getOwnerPhone());
        if (existingUser.isPresent() || existingTenantByPhone.isPresent()) {
            return resolveExistingOrFail(session, existingUser, existingTenantByPhone);
        }

        String displayName = resolveDisplayName(session);
        TenantEntity tenant = TenantEntity.builder()
                .displayName(displayName)
                .slug(resolveUniqueSlug(displayName))
                .tenantType(session.getTenantType())
                .ownerName(session.getOwnerName())
                .ownerEmail(session.getOwnerEmail())
                .phoneNumber(session.getOwnerPhone())
                .status(TenantStatus.ACTIVE)
                .activatedAt(Instant.now())
                .billingEmail(session.getOwnerEmail())
                .build();

        boolean paidFlow = session.isPaymentRequired();
        TenantSubscriptionEntity subscription = TenantSubscriptionEntity.builder()
                .plan(session.getSelectedPlan())
                .status(paidFlow ? SubscriptionStatus.ACTIVE : SubscriptionStatus.TRIALING)
                .provider(paidFlow ? BillingProvider.STRIPE : BillingProvider.MANUAL)
                .billingInterval(BillingInterval.MONTHLY)
                .currentPeriodStartAt(Instant.now())
                .currentPeriodEndAt(Instant.now().plusSeconds(30L * 24 * 60 * 60))
                .lastPaymentAt(paidFlow ? Instant.now() : null)
                .build();
        if (paidFlow) {
            subscription.addProviderLink(TenantSubscriptionProviderLinkEntity.builder()
                    .provider(BillingProvider.STRIPE)
                    .providerCustomerId(session.getProviderCustomerId())
                    .providerSubscriptionId(session.getProviderSubscriptionId())
                    .providerPriceReference(resolvePriceId(session.getSelectedPlan()))
                    .active(true)
                    .build());
        }
        tenant.assignSubscription(subscription);

        TenantEntity savedTenant = tenantRepository.save(tenant);
        provisionChurchIfNeeded(savedTenant, session);
        UserEntity owner = provisionOwner(savedTenant, session);
        seedWorkspaceIfRequested(savedTenant, session, owner);

        session.setProvisionedOwnerUserId(owner.getUuid());
        return savedTenant;
    }

    private TenantEntity resolveExistingOrFail(TenantOnboardingSessionEntity session,
                                               Optional<UserEntity> existingUser,
                                               Optional<TenantEntity> existingTenantByPhone) {
        if (existingUser.isPresent() && existingTenantByPhone.isPresent()) {
            UserEntity user = existingUser.get();
            TenantEntity tenant = existingTenantByPhone.get();
            boolean sameTenant = user.getTenant() != null && tenant.getId().equals(user.getTenant().getId());
            boolean typeMatches = session.getTenantType() == tenant.getTenantType();
            if (sameTenant && typeMatches) {
                session.setProvisionedOwnerUserId(user.getUuid());
                return tenant;
            }
        }
        throw new IllegalStateException(messageService.get(
                "onboarding.provisioning.contactCollision",
                "Cannot provision onboarding session due to email/phone collision"
        ));
    }

    private void provisionChurchIfNeeded(TenantEntity tenant, TenantOnboardingSessionEntity session) {
        if (tenant.getTenantType() != TenantType.CHURCH) {
            return;
        }

        DraftTenantPayload draft = parseDraft(session);
        if (draft.church() == null) {
            throw new IllegalStateException(messageService.get(
                    "onboarding.provisioning.churchDraftMissing",
                    "Church onboarding draft is missing church data"
            ));
        }

        ChurchEntity church = churchMapper.churchDTOToEntity(draft.church());
        church.setTenant(tenant);
        church.setUsesOurServices(true);
        if (church.getProfilePicture() != null) {
            church.getProfilePicture().setImageAssetType(ImageAssetType.CHURCH);
            church.getProfilePicture().setOwnerId(tenant.getId());
        }
        church.setChurchNumber(generateUniqueChurchNumber(church.getChurchName(), 5));
        ChurchEntity savedChurch = churchRepository.save(church);
        tenant.assignChurch(savedChurch);
        tenantRepository.save(tenant);
    }

    private UserEntity provisionOwner(TenantEntity tenant, TenantOnboardingSessionEntity session) {
        Role ownerRole = roleRepository.findByRoleName("OWNER")
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "role.owner.notFound",
                        "Owner role not found"
                )));

        Role primaryAdminRole = roleRepository.findByRoleName("PRIMARY_ADMIN")
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "role.primaryAdmin.notFound",
                        "Primary admin role not found"
                )));

        UserEntity owner = UserEntity.builder()
                .fullName(session.getOwnerName())
                .email(session.getOwnerEmail())
                .password(session.getDraftPasswordHash())
                .affiliatedTenant(tenant)
                .roles(new HashSet<>(Set.of(primaryAdminRole, ownerRole)))
                .userType(UserType.TENANT)
                .emailVerifiedAt(onboardingEmailVerificationService.isVerified(session.getOwnerEmail()) ? Instant.now() : null)
                .status(onboardingEmailVerificationService.isVerified(session.getOwnerEmail()) ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION)
                .build();
        UserEntity savedOwner = userRepository.save(owner);

        TenantAdminAssignmentEntity primaryAdminAssignment = TenantAdminAssignmentEntity.builder()
                .tenant(tenant)
                .userId(savedOwner.getUuid())
                .role(TenantRole.PRIMARY_ADMIN)
                .status(MembershipStatus.ACTIVE)
                .isBillingContact(true)
                .createdByUserId(savedOwner.getUuid())
                .updatedByUserId(savedOwner.getUuid())
                .build();
        tenantAdminAssignmentRepository.save(primaryAdminAssignment);

        return savedOwner;
    }

    private DraftTenantPayload parseDraft(TenantOnboardingSessionEntity session) {
        try {
            return objectMapper.readValue(session.getDraftPayloadJson(), DraftTenantPayload.class);
        } catch (Exception ex) {
            throw new IllegalStateException(messageService.get(
                    "onboarding.session.draftPayload.invalid",
                    "Invalid onboarding draft payload"
            ));
        }
    }

    private void seedWorkspaceIfRequested(TenantEntity tenant,
                                          TenantOnboardingSessionEntity session,
                                          UserEntity owner) {
        DraftTenantPayload draft = parseDraft(session);
        WorkspaceInitializationMode mode = draft.workspaceInitializationMode() == null
                ? WorkspaceInitializationMode.EMPTY
                : draft.workspaceInitializationMode();
        if (mode != WorkspaceInitializationMode.SEEDED) {
            return;
        }
        tenantDemoWorkspaceSeederService.seedDemoWorkspace(tenant, owner);
    }

    private String resolvePriceId(SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.BASIC || plan == SubscriptionPlan.ADVANCED || plan == SubscriptionPlan.PREMIUM) {
            return billingCatalog.resolve(plan).getPriceId();
        }
        return null;
    }

    private String resolveDisplayName(TenantOnboardingSessionEntity session) {
        DraftTenantPayload draft = parseDraft(session);
        ChurchDTO church = draft.church();
        if (session.getTenantType() == TenantType.CHURCH && church != null && StringUtils.hasText(church.getChurchNameLocal())) {
            return church.getChurchNameLocal().trim();
        }
        return session.getOwnerName().trim();
    }

    private String resolveUniqueSlug(String displayName) {
        String normalized = Normalizer.normalize(displayName == null ? "" : displayName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "tenant-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String candidate = normalized;
        int suffix = 2;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = normalized + "-" + suffix++;
        }
        return candidate;
    }

    private String generateUniqueChurchNumber(String churchName, int length) {
        String baseLetter = ChurchNumberUtils.derivePrefix(churchName);
        String churchNumber;
        do {
            churchNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (churchRepository.existsByChurchNumber(churchNumber));
        return churchNumber;
    }

    private String trimError(String message) {
        if (message == null || message.isBlank()) {
            return messageService.get(
                    "onboarding.provisioning.unknownError",
                    "Unknown provisioning error"
            );
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private record DraftTenantPayload(ChurchDTO church, WorkspaceInitializationMode workspaceInitializationMode) {}
}
