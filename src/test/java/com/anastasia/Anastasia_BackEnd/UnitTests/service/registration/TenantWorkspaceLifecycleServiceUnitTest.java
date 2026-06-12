package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditEventType;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditLogService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.TransactionRepository;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupJoinRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.FamilyRelationshipRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberTransferRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardTemplateRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PromoRedemptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantEntitlementAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantFeatureOverrideRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantPlanGrantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.WebhookEventReceiptRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantWorkspaceLifecycleService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantDemoTemplateCloneService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantDemoWorkspaceSeederService;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.TenantUserPermissionGrantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TenantWorkspaceLifecycleServiceUnitTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private TenantUserPermissionGrantRepository tenantUserPermissionGrantRepository;
    @Mock private PriestRepository priestRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ChildRepository childRepository;
    @Mock private FamilyRelationshipRepository familyRelationshipRepository;
    @Mock private MembershipCardAuditRepository membershipCardAuditRepository;
    @Mock private MembershipCardRepository membershipCardRepository;
    @Mock private MembershipCardTemplateRepository membershipCardTemplateRepository;
    @Mock private GroupJoinRequestRepository groupJoinRequestRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private EventRepository eventRepository;
    @Mock private CalendarEntryRepository calendarEntryRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private TenantPlanGrantRepository tenantPlanGrantRepository;
    @Mock private TenantFeatureOverrideRepository tenantFeatureOverrideRepository;
    @Mock private PromoRedemptionRepository promoRedemptionRepository;
    @Mock private TenantSubscriptionEventRepository tenantSubscriptionEventRepository;
    @Mock private TenantEntitlementAuditRepository tenantEntitlementAuditRepository;
    @Mock private WebhookEventReceiptRepository webhookEventReceiptRepository;
    @Mock private MemberTransferRequestRepository memberTransferRequestRepository;
    @Mock private PaymentIntentRepository paymentIntentRepository;
    @Mock private PaymentSubscriptionRepository paymentSubscriptionRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private FundRepository fundRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TenantDemoTemplateCloneService tenantDemoTemplateCloneService;
    @Mock private TenantDemoWorkspaceSeederService tenantDemoWorkspaceSeederService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private TenantWorkspaceLifecycleService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "demoGracePeriodDays", 14L);
        ReflectionTestUtils.setField(service, "paidRetentionDeletionDays", 30L);
        ReflectionTestUtils.setField(service, "retentionWarningDays", 7L);
    }

    @Test
    void syncTenantLifecycleSchedulesThirtyDayDeletionForPaidCancellationAtPeriodEnd() {
        Instant currentPeriodEnd = Instant.parse("2026-07-01T00:00:00Z");
        TenantEntity tenant = paidTenant(
                SubscriptionStatus.ACTIVE,
                currentPeriodEnd,
                true,
                null
        );
        when(tenantRepository.findWithSubscriptionById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(TenantEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantEntity updated = service.syncTenantLifecycle(tenant.getId(), UUID.randomUUID());

        assertThat(updated.getScheduledDeletionAt()).isEqualTo(currentPeriodEnd.plus(Duration.ofDays(30)));
        assertThat(updated.getArchiveScheduledAt()).isNull();
        assertThat(updated.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void syncTenantLifecycleSuspendsCanceledPaidWorkspaceAndKeepsThirtyDayDeletionDeadline() {
        Instant canceledAt = Instant.parse("2026-05-01T00:00:00Z");
        TenantEntity tenant = paidTenant(
                SubscriptionStatus.CANCELED,
                Instant.parse("2026-07-01T00:00:00Z"),
                false,
                canceledAt
        );
        when(tenantRepository.findWithSubscriptionById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(TenantEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantEntity updated = service.syncTenantLifecycle(tenant.getId(), UUID.randomUUID());

        assertThat(updated.getScheduledDeletionAt()).isEqualTo(canceledAt.plus(Duration.ofDays(30)));
        assertThat(updated.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(updated.getSuspensionReason()).contains("30 days");
    }

    @Test
    void syncTenantLifecycleKeepsDemoCancellationOnDemoPurgePathWhenWorkspaceNeverPaid() {
        Instant canceledAt = Instant.parse("2026-06-01T00:00:00Z");
        TenantEntity tenant = demoTenant(
                SubscriptionStatus.CANCELED,
                canceledAt,
                canceledAt
        );
        when(tenantRepository.findWithSubscriptionById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(TenantEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantEntity updated = service.syncTenantLifecycle(tenant.getId(), UUID.randomUUID());

        assertThat(updated.getScheduledPurgeAt()).isEqualTo(canceledAt.plus(Duration.ofDays(14)));
        assertThat(updated.getScheduledDeletionAt()).isNull();
        assertThat(updated.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(updated.getSuspensionReason()).contains("Demo trial expired");
    }

    @Test
    void processRetentionPoliciesDeletesTenantWhenThirtyDayDeadlineArrives() {
        Instant scheduledDeletionAt = Instant.now().minus(Duration.ofDays(1));
        TenantEntity tenant = paidTenant(
                SubscriptionStatus.CANCELED,
                scheduledDeletionAt.minus(Duration.ofDays(30)),
                false,
                scheduledDeletionAt.minus(Duration.ofDays(30))
        );
        tenant.setScheduledDeletionAt(scheduledDeletionAt);
        UserEntity owner = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email(tenant.getOwnerEmail())
                .build();

        when(tenantRepository.findActiveDemoWorkspaces()).thenReturn(List.of());
        when(tenantRepository.findDemoWorkspacesDueForPurge(any(Instant.class))).thenReturn(List.of());
        when(tenantRepository.findTenantsDueForDeletion(any(Instant.class))).thenReturn(List.of(tenant));
        when(tenantRepository.findTenantsDueForArchive(any(Instant.class))).thenReturn(List.of());
        when(tenantRepository.save(any(TenantEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findTenantAdmin(tenant.getId())).thenReturn(Optional.of(owner));
        when(userRepository.findByAffiliatedTenantId(tenant.getId())).thenReturn(List.of(owner));
        when(churchRepository.findByTenantId(tenant.getId())).thenReturn(Optional.empty());
        when(tenantAdminAssignmentRepository.findByTenant_IdOrderByCreatedAtAsc(tenant.getId())).thenReturn(List.of());
        when(membershipCardAuditRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(paymentIntentRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(paymentSubscriptionRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(transactionRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(fundRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(accountRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(membershipCardRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(membershipCardTemplateRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(familyRelationshipRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(appointmentRepository.findByTenantIdOrderByStartAtUtcAsc(tenant.getId())).thenReturn(List.of());
        when(calendarEntryRepository.findByTenantIdOrderByStartAtUtcAsc(tenant.getId())).thenReturn(List.of());
        when(eventRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(groupJoinRequestRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(groupRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(childRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(memberRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(staffRepository.findByTenant_Id(tenant.getId())).thenReturn(List.of());
        when(tenantPlanGrantRepository.findByTenant_IdOrderByCreatedAtDesc(tenant.getId())).thenReturn(List.of());
        when(tenantFeatureOverrideRepository.findByTenant_IdOrderByCreatedAtDesc(tenant.getId())).thenReturn(List.of());
        when(promoRedemptionRepository.findByTenant_IdOrderByCreatedAtDesc(tenant.getId())).thenReturn(List.of());
        when(tenantSubscriptionEventRepository.findByTenantIdOrderByOccurredAtDesc(tenant.getId())).thenReturn(List.of());
        when(tenantEntitlementAuditRepository.findByTenant_IdOrderByOccurredAtDesc(tenant.getId())).thenReturn(List.of());
        when(webhookEventReceiptRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(memberTransferRequestRepository.findByFromTenant_IdOrToTenant_Id(tenant.getId(), tenant.getId())).thenReturn(List.of());

        service.processRetentionPolicies();

        assertThat(tenant.getDeletedAt()).isNotNull();
        assertThat(tenant.getPurgedAt()).isNotNull();
        assertThat(tenant.getScheduledDeletionAt()).isNull();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.CLOSED);
        verify(tenantRepository).save(tenant);
        verify(auditLogService).record(
                eq(AuditEventType.DATA_DELETION_EXECUTED),
                eq("SUCCESS"),
                isNull(),
                eq(tenant.getOwnerEmail()),
                eq(tenant.getId()),
                eq("TENANT"),
                eq(tenant.getId().toString()),
                eq("retention-policy"),
                contains("Deleting tenant workspace")
        );
    }

    private TenantEntity paidTenant(SubscriptionStatus status,
                                    Instant currentPeriodEndAt,
                                    boolean cancelAtPeriodEnd,
                                    Instant canceledAt) {
        TenantEntity tenant = TenantEntity.builder()
                .id(UUID.randomUUID())
                .ownerEmail("owner@example.com")
                .status(TenantStatus.ACTIVE)
                .build();

        TenantSubscriptionEntity subscription = TenantSubscriptionEntity.builder()
                .tenant(tenant)
                .plan(SubscriptionPlan.BASIC)
                .status(status)
                .provider(BillingProvider.STRIPE)
                .currentPeriodEndAt(currentPeriodEndAt)
                .cancelAtPeriodEnd(cancelAtPeriodEnd)
                .canceledAt(canceledAt)
                .lastPaymentAt(Instant.parse("2026-04-01T00:00:00Z"))
                .build();
        tenant.assignSubscription(subscription);
        return tenant;
    }

    private TenantEntity demoTenant(SubscriptionStatus status,
                                    Instant currentPeriodEndAt,
                                    Instant canceledAt) {
        TenantEntity tenant = TenantEntity.builder()
                .id(UUID.randomUUID())
                .ownerEmail("owner@example.com")
                .status(TenantStatus.ACTIVE)
                .demoWorkspace(true)
                .build();

        TenantSubscriptionEntity subscription = TenantSubscriptionEntity.builder()
                .tenant(tenant)
                .plan(SubscriptionPlan.FREE)
                .status(status)
                .provider(BillingProvider.MANUAL)
                .currentPeriodEndAt(currentPeriodEndAt)
                .canceledAt(canceledAt)
                .build();
        tenant.assignSubscription(subscription);
        return tenant;
    }
}
