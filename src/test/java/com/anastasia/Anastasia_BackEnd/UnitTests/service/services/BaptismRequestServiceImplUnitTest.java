package com.anastasia.Anastasia_BackEnd.UnitTests.service.services;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantAdminNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismLanguageDetailsRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismServiceRequestCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismServiceRequestResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.MemberServiceRequestListItemResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.UploadedDocumentRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismLanguageDetails;
import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestStatus;
import com.anastasia.Anastasia_BackEnd.modules.services.service.BaptismRequestServiceImpl;
import com.anastasia.Anastasia_BackEnd.modules.services.repository.BaptismRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("experimental")
class BaptismRequestServiceImplUnitTest {

    @Mock private BaptismRequestRepository baptismRequestRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantAdminNotificationService tenantAdminNotificationService;

    @InjectMocks private BaptismRequestServiceImpl baptismRequestService;

    private final UUID userUuid = UUID.randomUUID();
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setUuid(userUuid);
        setAuthentication(user);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void create_shouldNormalizeChurchNumberAndTrimValues() {
        ChurchEntity church = new ChurchEntity();
        church.setChurchNumber("\"CH-123\"");
        TenantEntity tenant = new TenantEntity();
        tenant.setId(UUID.randomUUID());
        church.setTenant(tenant);
        when(churchRepository.findByChurchNumber("CH-123")).thenReturn(Optional.of(church));
        when(baptismRequestRepository.existsByRequestNumber(any())).thenReturn(false);
        when(baptismRequestRepository.save(any())).thenAnswer(invocation -> {
            BaptismRequestEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return entity;
        });

        BaptismServiceRequestCreateRequest request = new BaptismServiceRequestCreateRequest(
                "\"CH-123\"",
                LocalDate.of(2018, 9, 1),
                LocalDate.of(2018, 10, 1),
                sampleLanguageDetails("Baby"),
                sampleLanguageDetails("Baby"),
                new UploadedDocumentRequest(" https://example.com/photo.png ", " 1024 "),
                new UploadedDocumentRequest("https://example.com/birth.png", "512"),
                new UploadedDocumentRequest("https://example.com/father.png", null),
                new UploadedDocumentRequest("https://example.com/priest.png", "256")
        );

        BaptismServiceRequestResponse response = baptismRequestService.create(request);

        ArgumentCaptor<BaptismRequestEntity> captor = ArgumentCaptor.forClass(BaptismRequestEntity.class);
        verify(baptismRequestRepository).save(captor.capture());
        BaptismRequestEntity saved = captor.getValue();

        assertThat(saved.getChurchNumber()).isEqualTo("CH-123");
        assertThat(saved.getBabyPhotoUrl()).isEqualTo("https://example.com/photo.png");
        assertThat(saved.getBabyPhotoSize()).isEqualTo("1024");
        assertThat(saved.getStatus()).isEqualTo(BaptismRequestStatus.PENDING);
        assertThat(response.id()).isEqualTo(11L);
        verify(tenantAdminNotificationService).notifyBaptismRequestSubmitted(saved, userUuid);
    }

    @Test
    void create_shouldRejectInvalidDates() {
        when(churchRepository.findByChurchNumber(any())).thenReturn(Optional.of(new ChurchEntity()));
        BaptismServiceRequestCreateRequest request = new BaptismServiceRequestCreateRequest(
                "CH-001",
                LocalDate.of(2020, 1, 2),
                LocalDate.of(2020, 1, 1),
                sampleLanguageDetails("Baby"),
                sampleLanguageDetails("Baby"),
                new UploadedDocumentRequest("url", null),
                new UploadedDocumentRequest("url", null),
                new UploadedDocumentRequest("url", null),
                new UploadedDocumentRequest("url", null)
        );

        assertThatThrownBy(() -> baptismRequestService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Baptism date cannot be earlier than birth date");
    }

    @Test
    void create_shouldFailWhenTenantMissing() {
        ChurchEntity church = new ChurchEntity();
        church.setTenant(null);
        church.setChurchNumber("CH-111");
        when(churchRepository.findByChurchNumber(any())).thenReturn(Optional.of(church));
        BaptismServiceRequestCreateRequest request = new BaptismServiceRequestCreateRequest(
                "CH-111",
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 2, 1),
                sampleLanguageDetails("Baby"),
                sampleLanguageDetails("Baby"),
                new UploadedDocumentRequest("url", null),
                new UploadedDocumentRequest("url", null),
                new UploadedDocumentRequest("url", null),
                new UploadedDocumentRequest("url", null)
        );

        assertThatThrownBy(() -> baptismRequestService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    void listMine_shouldMapRequests() {
        BaptismRequestEntity entity = new BaptismRequestEntity();
        entity.setId(5L);
        entity.setRequestNumber("BAP-123");
        entity.setStatus(BaptismRequestStatus.APPROVED);
        entity.setCreatedAt(Instant.now());
        entity.setEnglish(BaptismLanguageDetails.builder().fullName("Baby Name").baptismalName("Baptism Name").fatherFullName("Father").motherFullName("Mother").godParentFullName("God").priestFullName("Priest").churchOfBaptismName("Church").build());
        entity.setChurch(new ChurchEntity());
        entity.getChurch().setChurchNameLocal("St. Paul");
        entity.setChurchNumber("CH-001");
        when(baptismRequestRepository.findByRequestedByUser_UuidOrderByCreatedAtDesc(userUuid))
                .thenReturn(List.of(entity));

        List<MemberServiceRequestListItemResponse> responses = baptismRequestService.listMine();

        assertThat(responses).hasSize(1);
        MemberServiceRequestListItemResponse item = responses.get(0);
        assertThat(item.requestNumber()).isEqualTo("BAP-123");
        assertThat(item.requestedForName()).isEqualTo("Baby Name");
        assertThat(item.serviceType()).isEqualTo("BAPTISM");
    }

    private static BaptismLanguageDetailsRequest sampleLanguageDetails(String suffix) {
        return new BaptismLanguageDetailsRequest(
                "Full" + suffix,
                "Baptism" + suffix,
                "Father" + suffix,
                "Mother" + suffix,
                "God" + suffix,
                "Priest" + suffix,
                "Church" + suffix
        );
    }

    private void setAuthentication(UserEntity user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(user));
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
