package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WorkspaceInitializationMode;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantOnboardingSessionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingEmailVerificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantDemoTemplateCloneService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantDemoWorkspaceSeederService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantOnboardingProvisioningService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantPlanBillingCatalog;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TenantOnboardingProvisioningServiceTest {

    @Mock private TenantOnboardingSessionRepository onboardingSessionRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ChurchMapper churchMapper;
    @Mock private TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private OnboardingEmailVerificationService onboardingEmailVerificationService;
    @Mock private TenantDemoWorkspaceSeederService tenantDemoWorkspaceSeederService;
    @Mock private TenantDemoTemplateCloneService tenantDemoTemplateCloneService;
    @Mock private LocalizedMessageService messageService;

    private TenantOnboardingProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new TenantOnboardingProvisioningService(
                onboardingSessionRepository,
                tenantRepository,
                churchRepository,
                userRepository,
                roleRepository,
                churchMapper,
                tenantAdminAssignmentRepository,
                securityUtils,
                new ObjectMapper(),
                new TenantPlanBillingCatalog(messageService),
                onboardingEmailVerificationService,
                tenantDemoWorkspaceSeederService,
                tenantDemoTemplateCloneService,
                messageService
        );

        when(messageService.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(messageService.get(anyString(), anyString(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(tenantRepository.save(any(TenantEntity.class))).thenAnswer(invocation -> {
            TenantEntity tenant = invocation.getArgument(0);
            if (tenant.getId() == null) {
                tenant.setId(UUID.randomUUID());
            }
            return tenant;
        });
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantAdminAssignmentRepository.save(any(TenantAdminAssignmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assertOwnerIdentityEligibleRejectsUserAlreadyLinkedToTenant() {
        TenantEntity existingTenant = TenantEntity.builder().id(UUID.randomUUID()).build();
        UserEntity existingUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("owner@example.com")
                .fullName("Owner")
                .userType(UserType.TENANT)
                .status(UserStatus.ACTIVE)
                .affiliatedTenant(existingTenant)
                .build();
        existingUser.assignAffiliatedTenant(existingTenant);

        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> service.assertOwnerIdentityEligible("owner@example.com", "+15555550123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    void finalizeProvisioningIfReadyPromotesStandaloneExistingUser() {
        UUID sessionId = UUID.randomUUID();
        Role userRole = Role.builder().id(1L).roleName("USER").build();
        Role ownerRole = Role.builder().id(2L).roleName("OWNER").build();
        Role primaryAdminRole = Role.builder().id(3L).roleName("PRIMARY_ADMIN").build();

        UserEntity existingUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("owner@example.com")
                .fullName("Guest Owner")
                .userType(UserType.GUEST)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();

        TenantOnboardingSessionEntity session = TenantOnboardingSessionEntity.builder()
                .id(sessionId)
                .status(OnboardingSessionStatus.PAYMENT_CONFIRMED)
                .tenantType(TenantType.PRIEST)
                .selectedPlan(SubscriptionPlan.FREE)
                .ownerName("Promoted Owner")
                .ownerEmail("owner@example.com")
                .ownerPhone("+15555550123")
                .draftPayloadJson("{\"workspaceInitializationMode\":\"" + WorkspaceInitializationMode.EMPTY + "\"}")
                .draftPasswordHash("encoded-password")
                .paymentRequired(false)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(onboardingSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(existingUser));
        when(tenantRepository.findByPhoneNumber("+15555550123")).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("OWNER")).thenReturn(Optional.of(ownerRole));
        when(roleRepository.findByRoleName("PRIMARY_ADMIN")).thenReturn(Optional.of(primaryAdminRole));
        when(onboardingEmailVerificationService.isVerified("owner@example.com")).thenReturn(true);

        service.finalizeProvisioningIfReady(sessionId);

        assertThat(session.getStatus()).isEqualTo(OnboardingSessionStatus.PROVISIONED);
        assertThat(session.getProvisionedTenantId()).isNotNull();
        assertThat(session.getProvisionedOwnerUserId()).isEqualTo(existingUser.getUuid());
        assertThat(existingUser.getTenantId()).isEqualTo(session.getProvisionedTenantId());
        assertThat(existingUser.getUserType()).isEqualTo(UserType.TENANT);
        assertThat(existingUser.getPhoneNumber()).isEqualTo("+15555550123");
        assertThat(existingUser.getRoles()).contains(userRole, ownerRole, primaryAdminRole);
        verify(userRepository).save(existingUser);
    }

    @Test
    void finalizeProvisioningIfReadyMarksProvisioningFailedForBusinessConflict() {
        UUID sessionId = UUID.randomUUID();
        TenantEntity existingTenant = TenantEntity.builder().id(UUID.randomUUID()).build();
        UserEntity existingUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("owner@example.com")
                .fullName("Owner")
                .userType(UserType.TENANT)
                .status(UserStatus.ACTIVE)
                .affiliatedTenant(existingTenant)
                .build();
        existingUser.assignAffiliatedTenant(existingTenant);

        TenantOnboardingSessionEntity session = TenantOnboardingSessionEntity.builder()
                .id(sessionId)
                .status(OnboardingSessionStatus.PAYMENT_CONFIRMED)
                .tenantType(TenantType.CHURCH)
                .selectedPlan(SubscriptionPlan.BASIC)
                .ownerName("Owner")
                .ownerEmail("owner@example.com")
                .ownerPhone("+15555550123")
                .draftPayloadJson("{}")
                .draftPasswordHash("encoded-password")
                .paymentRequired(true)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(onboardingSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(existingUser));
        when(tenantRepository.findByPhoneNumber("+15555550123")).thenReturn(Optional.empty());

        service.finalizeProvisioningIfReady(sessionId);

        assertThat(session.getStatus()).isEqualTo(OnboardingSessionStatus.PROVISIONING_FAILED);
        assertThat(session.getFailureReason()).contains("already linked");
    }
}
