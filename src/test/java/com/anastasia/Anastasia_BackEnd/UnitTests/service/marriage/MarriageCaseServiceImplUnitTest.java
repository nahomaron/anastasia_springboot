package com.anastasia.Anastasia_BackEnd.UnitTests.service.marriage;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantAdminNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.service.CalendarEntryService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageMemberInitiationRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageLanguageCode;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePairingMode;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageAuditEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageCaseNoteRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageCaseRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageCertificateAmendmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageCertificateRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageCertificateSequenceConfigRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageConfessorApprovalRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageImpedimentRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageManualPaymentRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriagePartyDocumentRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriagePartyRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriagePartySubmissionRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriagePairingTokenRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriagePriestAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageRequirementAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageRequirementTemplateRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageReviewRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageScheduleRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageStatusHistoryRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageWitnessRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.service.MarriageCaseServiceImpl;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.support.MarriageCaseReferenceGenerator;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.support.MarriageSecuritySupport;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.validation.MarriageCaseDomainValidator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
@Tag("experimental")
class MarriageCaseServiceImplUnitTest {

    @Mock private MarriageCaseRepository marriageCaseRepository;
    @Mock private MarriagePartyRepository marriagePartyRepository;
    @Mock private MarriagePartySubmissionRepository marriagePartySubmissionRepository;
    @Mock private MarriagePartyDocumentRepository marriagePartyDocumentRepository;
    @Mock private MarriageRequirementTemplateRepository marriageRequirementTemplateRepository;
    @Mock private MarriageRequirementAssignmentRepository marriageRequirementAssignmentRepository;
    @Mock private MarriagePairingTokenRepository marriagePairingTokenRepository;
    @Mock private MarriageStatusHistoryRepository marriageStatusHistoryRepository;
    @Mock private MarriageAuditEventRepository marriageAuditEventRepository;
    @Mock private MarriageReviewRepository marriageReviewRepository;
    @Mock private MarriageCaseNoteRepository marriageCaseNoteRepository;
    @Mock private MarriageConfessorApprovalRepository marriageConfessorApprovalRepository;
    @Mock private MarriageImpedimentRepository marriageImpedimentRepository;
    @Mock private MarriageManualPaymentRepository marriageManualPaymentRepository;
    @Mock private MarriageWitnessRepository marriageWitnessRepository;
    @Mock private MarriagePriestAssignmentRepository marriagePriestAssignmentRepository;
    @Mock private MarriageScheduleRepository marriageScheduleRepository;
    @Mock private MarriageCertificateRepository marriageCertificateRepository;
    @Mock private MarriageCertificateSequenceConfigRepository marriageCertificateSequenceConfigRepository;
    @Mock private MarriageCertificateAmendmentRepository marriageCertificateAmendmentRepository;
    @Mock private MarriageSecuritySupport marriageSecuritySupport;
    @Mock private MarriageCaseReferenceGenerator marriageCaseReferenceGenerator;
    @Mock private MarriageCaseDomainValidator marriageCaseDomainValidator;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private CalendarEntryService calendarEntryService;
    @Mock private CalendarEntryRepository calendarEntryRepository;
    @Mock private TenantAdminNotificationService tenantAdminNotificationService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private MarriageCaseServiceImpl marriageCaseService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userUuid = UUID.randomUUID();
    private UserEntity user;
    private ChurchEntity church;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setUuid(userUuid);
        Adult_MemberEntity membership = new Adult_MemberEntity();
        membership.setId(777L);
        user.setMembership(membership);
        setAuthentication(user);
        church = new ChurchEntity();
        church.setChurchNumber("CH-001");
        TenantEntity tenant = new TenantEntity();
        tenant.setId(tenantId);
        church.setTenant(tenant);
        when(churchRepository.findByChurchNumber("CH-001")).thenReturn(Optional.of(church));
        when(marriageSecuritySupport.requireCurrentUser()).thenReturn(user);
        when(marriageCaseReferenceGenerator.nextReference()).thenReturn("REF-1");
        when(marriageRequirementTemplateRepository.findByChurch_ChurchIdAndEnabledTrueOrderByOrderIndexAsc(any()))
                .thenReturn(List.of());
        when(marriageCaseRepository.save(any())).thenAnswer(invocation -> {
            MarriageCaseEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
        AtomicLong partyId = new AtomicLong(5);
        when(marriagePartyRepository.save(any())).thenAnswer(invocation -> {
            MarriagePartyEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void startMemberInitiatedCase_shouldUseInvitationPairingModeWhenCounterpartEmailProvided() {
        MarriageMemberInitiationRequest request = new MarriageMemberInitiationRequest(
                "CH-001",
                MarriagePartyRole.GROOM,
                MarriageLanguageCode.EN,
                "Partner Local",
                "Partner English",
                "partner@example.com",
                "555-1234",
                false
        );

        marriageCaseService.startMemberInitiatedCase(request);

        ArgumentCaptor<MarriageCaseEntity> captor = ArgumentCaptor.forClass(MarriageCaseEntity.class);
        verify(marriageCaseRepository, times(2)).save(captor.capture());
        MarriageCaseEntity capturedCase = captor.getAllValues().get(1);

        assertThat(capturedCase.getPairingMode()).isEqualTo(MarriagePairingMode.INVITATION_LINK);
        verify(tenantAdminNotificationService).notifyMarriageCaseSubmitted(capturedCase, userUuid);
    }

    private void setAuthentication(UserEntity user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(user));
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
