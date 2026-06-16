package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeClient;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.onboarding.OnboardingSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantOnboardingSessionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingEmailVerificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingSessionAccessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantOnboardingBillingService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantOnboardingProvisioningService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantPlanBillingCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TenantOnboardingBillingServiceTest {

    @Mock private TenantOnboardingSessionRepository onboardingSessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StripeClient stripeClient;
    @Mock private TenantOnboardingProvisioningService onboardingProvisioningService;
    @Mock private OnboardingEmailVerificationService onboardingEmailVerificationService;
    @Mock private AuthService authService;
    @Mock private LocalizedMessageService messageService;

    private TenantOnboardingBillingService service;

    @BeforeEach
    void setUp() {
        TenantPlanBillingCatalog billingCatalog = new TenantPlanBillingCatalog(messageService);
        OnboardingSessionAccessService accessService = new OnboardingSessionAccessService(messageService);
        service = new TenantOnboardingBillingService(
                onboardingSessionRepository,
                billingCatalog,
                passwordEncoder,
                new ObjectMapper(),
                stripeClient,
                onboardingProvisioningService,
                onboardingEmailVerificationService,
                accessService,
                authService,
                messageService
        );

        when(messageService.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(messageService.get(anyString(), anyString(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    }

    @Test
    void createSessionIssuesOnboardingAccessTokenForNewSession() {
        TenantDTO tenantDTO = validTenantDto();
        when(onboardingSessionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(onboardingSessionRepository.save(any(TenantOnboardingSessionEntity.class)))
                .thenAnswer(invocation -> {
                    TenantOnboardingSessionEntity saved = invocation.getArgument(0);
                    if (saved.getId() == null) {
                        saved.setId(UUID.randomUUID());
                    }
                    return saved;
                });

        OnboardingSessionResponse response = service.createSession(tenantDTO, "idem-1", null);

        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getOnboardingAccessToken()).isNotBlank();
        verify(onboardingProvisioningService).assertOwnerIdentityEligible("owner@example.com", "+15555550123");
    }

    @Test
    void getSessionRejectsInvalidAccessToken() {
        SessionFixture fixture = existingSession();
        when(onboardingSessionRepository.findById(fixture.session().getId())).thenReturn(Optional.of(fixture.session()));

        assertThatThrownBy(() -> service.getSession(fixture.session().getId(), "wrong-token"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void createSessionRequiresExistingAccessTokenForIdempotentReplay() {
        SessionFixture fixture = existingSession();
        TenantDTO tenantDTO = validTenantDto();
        when(onboardingSessionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(fixture.session()));

        assertThatThrownBy(() -> service.createSession(tenantDTO, "idem-1", null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void getSessionAllowsValidAccessToken() {
        SessionFixture fixture = existingSession();
        when(onboardingSessionRepository.findById(fixture.session().getId())).thenReturn(Optional.of(fixture.session()));

        OnboardingSessionResponse response = service.getSession(fixture.session().getId(), fixture.accessToken());

        assertThat(response.getSessionId()).isEqualTo(fixture.session().getId());
        assertThat(response.getOnboardingAccessToken()).isNull();
    }

    private TenantDTO validTenantDto() {
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setTenantType(TenantType.CHURCH);
        tenantDTO.setSubscriptionPlan(SubscriptionPlan.FREE);
        tenantDTO.setOwnerName("Owner");
        tenantDTO.setOwnerEmail("owner@example.com");
        tenantDTO.setPhoneNumber("+15555550123");
        tenantDTO.setPassword("Password123!");
        tenantDTO.setConfirmPassword("Password123!");
        tenantDTO.setTermsAccepted(true);
        tenantDTO.setTermsVersion("2026-06");
        return tenantDTO;
    }

    private SessionFixture existingSession() {
        TenantOnboardingSessionEntity session = TenantOnboardingSessionEntity.builder()
                .id(UUID.randomUUID())
                .status(OnboardingSessionStatus.DRAFT)
                .tenantType(TenantType.CHURCH)
                .selectedPlan(SubscriptionPlan.FREE)
                .ownerName("Owner")
                .ownerEmail("owner@example.com")
                .ownerPhone("+15555550123")
                .draftPayloadJson("{}")
                .draftPasswordHash("encoded-password")
                .paymentRequired(false)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        OnboardingSessionAccessService accessService = new OnboardingSessionAccessService(messageService);
        String token = accessService.issueAccessToken(session);
        return new SessionFixture(session, token);
    }

    private record SessionFixture(TenantOnboardingSessionEntity session, String accessToken) {
    }
}
