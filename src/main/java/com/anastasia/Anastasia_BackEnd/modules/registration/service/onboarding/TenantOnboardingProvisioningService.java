package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.PhoneVerificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingInterval;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantUserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantOnboardingSessionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantUserRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final TenantUserRepository tenantUserRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final TenantPlanBillingCatalog billingCatalog;
    private final OnboardingEmailVerificationService onboardingEmailVerificationService;

    @Transactional
    public void finalizeProvisioningIfReady(UUID onboardingSessionId) {
        TenantOnboardingSessionEntity session = onboardingSessionRepository.findById(onboardingSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding session not found"));

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
            session.setProvisionedAt(LocalDateTime.now());
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

        TenantEntity tenant = TenantEntity.builder()
                .tenantType(session.getTenantType())
                .ownerName(session.getOwnerName())
                .phoneNumber(session.getOwnerPhone())
                .isActiveTenant(true)
                .build();

        boolean paidFlow = session.isPaymentRequired();
        TenantSubscriptionEntity subscription = TenantSubscriptionEntity.builder()
                .plan(session.getSelectedPlan())
                .status(paidFlow ? SubscriptionStatus.ACTIVE : SubscriptionStatus.TRIALING)
                .provider(paidFlow ? BillingProvider.STRIPE : BillingProvider.MANUAL)
                .providerCustomerId(session.getProviderCustomerId())
                .providerSubscriptionId(session.getProviderSubscriptionId())
                .stripePriceId(resolvePriceId(session.getSelectedPlan()))
                .billingInterval(BillingInterval.MONTHLY)
                .currentPeriodStartAt(LocalDateTime.now())
                .currentPeriodEndAt(LocalDateTime.now().plusMonths(1))
                .lastPaymentAt(paidFlow ? LocalDateTime.now() : null)
                .build();
        tenant.assignSubscription(subscription);

        TenantEntity savedTenant = tenantRepository.save(tenant);
        provisionChurchIfNeeded(savedTenant, session);
        UserEntity owner = provisionOwner(savedTenant, session);

        session.setProvisionedOwnerUserId(owner.getUuid());
        try {
            phoneVerificationService.startVerification(session.getOwnerPhone());
        } catch (RuntimeException ex) {
            log.warn("Phone verification kickoff failed for onboarding session {}: {}", session.getId(), ex.getMessage());
        }
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
        throw new IllegalStateException("Cannot provision onboarding session due to email/phone collision");
    }

    private void provisionChurchIfNeeded(TenantEntity tenant, TenantOnboardingSessionEntity session) {
        if (tenant.getTenantType() != TenantType.CHURCH) {
            return;
        }

        DraftTenantPayload draft = parseDraft(session);
        if (draft.church() == null) {
            throw new IllegalStateException("Church onboarding draft is missing church data");
        }

        ChurchEntity church = churchMapper.churchDTOToEntity(draft.church());
        church.setTenant(tenant);
        church.setUsesOurServices(true);
        if (church.getProfilePicture() != null) {
            church.getProfilePicture().setAvatarType(AvatarType.CHURCH);
            church.getProfilePicture().setOwnerId(tenant.getId());
        }
        church.setChurchNumber(generateUniqueChurchNumber(church.getChurchName(), 5));
        ChurchEntity savedChurch = churchRepository.save(church);
        tenant.assignChurch(savedChurch);
        tenantRepository.save(tenant);
    }

    private UserEntity provisionOwner(TenantEntity tenant, TenantOnboardingSessionEntity session) {
        Role ownerRole = roleRepository.findByRoleName("OWNER")
                .orElseThrow(() -> new IllegalStateException("Owner role not found"));
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Admin role not found"));

        UserEntity owner = UserEntity.builder()
                .fullName(session.getOwnerName())
                .email(session.getOwnerEmail())
                .password(session.getDraftPasswordHash())
                .tenant(tenant)
                .roles(new HashSet<>(Set.of(ownerRole, adminRole)))
                .userType(UserType.TENANT)
                .verified(onboardingEmailVerificationService.isVerified(session.getOwnerEmail()))
                .build();
        UserEntity savedOwner = userRepository.save(owner);

        TenantUserEntity primaryAdminAssignment = TenantUserEntity.builder()
                .tenant(tenant)
                .userId(savedOwner.getUuid())
                .role(TenantRole.PRIMARY_ADMIN)
                .status(MembershipStatus.ACTIVE)
                .isBillingContact(true)
                .createdByUserId(savedOwner.getUuid())
                .updatedByUserId(savedOwner.getUuid())
                .build();
        tenantUserRepository.save(primaryAdminAssignment);

        return savedOwner;
    }

    private DraftTenantPayload parseDraft(TenantOnboardingSessionEntity session) {
        try {
            return objectMapper.readValue(session.getDraftPayloadJson(), DraftTenantPayload.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid onboarding draft payload");
        }
    }

    private String resolvePriceId(SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.BASIC || plan == SubscriptionPlan.ADVANCED || plan == SubscriptionPlan.PREMIUM) {
            return billingCatalog.resolve(plan).getPriceId();
        }
        return null;
    }

    private String generateUniqueChurchNumber(String churchName, int length) {
        String baseLetter = "CH";
        if (churchName != null) {
            String trimmed = churchName.trim();
            if (trimmed.length() >= 2) {
                if (trimmed.startsWith("st.") && trimmed.length() >= 5) {
                    baseLetter = trimmed.substring(3, 5).toUpperCase();
                } else {
                    baseLetter = trimmed.substring(0, 2).toUpperCase();
                }
            }
        }

        String churchNumber;
        do {
            churchNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (churchRepository.existsByChurchNumber(churchNumber));
        return churchNumber;
    }

    private String trimError(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown provisioning error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private record DraftTenantPayload(ChurchDTO church) {}
}
