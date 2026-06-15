package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditLogService;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.PublicChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchServiceImpl;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@LenientMockitoTest
class ChurchServiceUnitTest {

    @Mock private ChurchRepository churchRepository;
    @Mock private ChurchMapper churchMapper;
    @Mock private TenantRepository tenantRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private LocalizedMessageService messageService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ChurchServiceImpl churchService;

    private ChurchEntity church;
    private TenantEntity tenant;

    @BeforeEach
    void setUp() {
        tenant = TestDataUtil.createTestTenantEntity();
        church = TestDataUtil.createTestChurchEntity(tenant);
        lenient().when(messageService.get(eq("church.notFound"), anyString())).thenReturn("Church Not Found");
        lenient().when(messageService.get(eq("tenant.context.missing"), anyString())).thenReturn("Tenant ID is not set in the context");
        lenient().when(messageService.get(eq("tenant.invalid"), anyString())).thenReturn("No valid tenant found");
        lenient().when(churchMapper.churchEntityToResponse(any(ChurchEntity.class))).thenAnswer(invocation -> {
            ChurchEntity mappedChurch = invocation.getArgument(0);
            return ChurchResponse.builder()
                    .churchId(mappedChurch.getChurchId())
                    .churchNumber(mappedChurch.getChurchNumber())
                    .churchName(mappedChurch.getChurchNameLocal())
                    .diocese(mappedChurch.getDiocese())
                    .dioceseLocal(mappedChurch.getDioceseLocal())
                    .email(mappedChurch.getEmail())
                    .phone(mappedChurch.getPhone())
                    .descriptionLocal(mappedChurch.getDescriptionLocal())
                    .publicDirectoryEnabled(mappedChurch.isPublicDirectoryEnabled())
                    .status(mappedChurch.getStatus())
                    .tenantId(mappedChurch.getTenant() != null ? mappedChurch.getTenant().getId() : null)
                    .build();
        });
        lenient().when(churchMapper.churchEntityToPublicResponse(any(ChurchEntity.class))).thenAnswer(invocation -> {
            ChurchEntity mappedChurch = invocation.getArgument(0);
            return PublicChurchResponse.builder()
                    .churchNumber(mappedChurch.getChurchNumber())
                    .churchName(mappedChurch.getChurchName())
                    .city(mappedChurch.getAddress() != null ? mappedChurch.getAddress().getCity() : null)
                    .country(mappedChurch.getAddress() != null ? mappedChurch.getAddress().getCountry() : null)
                    .usesOurServices(mappedChurch.isUsesOurServices())
                    .build();
        });
    }

    @Test
    void testFindAllChurches() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ChurchEntity> entityPage = new PageImpl<>(List.of(church));
        ChurchResponse dto = ChurchResponse.builder()
                .churchName(church.getChurchNameLocal())
                .diocese(church.getDiocese())
                .dioceseLocal(church.getDioceseLocal())
                .email(church.getEmail())
                .phone(church.getPhone())
                .descriptionLocal(church.getDescriptionLocal())
                .build();

        when(churchRepository.findAllFiltered(null, pageable)).thenReturn(entityPage);
        when(churchMapper.churchEntityToResponse(church)).thenReturn(dto);

        Page<ChurchResponse> result = churchService.findAll(pageable, null, null);

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void testFindAllPublicChurches_usesOptInQueries() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ChurchEntity> entityPage = new PageImpl<>(List.of(church));

        when(churchRepository.findAllPublicFiltered(true, pageable)).thenReturn(entityPage);

        Page<PublicChurchResponse> result = churchService.findAllPublic(pageable, null, true);

        assertThat(result.getContent()).singleElement()
                .extracting(PublicChurchResponse::getChurchNumber)
                .isEqualTo(church.getChurchNumber());
        verify(churchRepository).findAllPublicFiltered(true, pageable);
        verify(churchRepository, never()).findAllFiltered(any(), any());
    }

    @Test
    void testExistsById() {
        when(churchRepository.existsById(1L)).thenReturn(true);
        assertThat(churchService.exists(1L)).isTrue();
    }

    @Test
    void testUpdateChurch_whenExists() {
        when(churchRepository.findById(1L)).thenReturn(Optional.of(church));
        when(churchRepository.save(any(ChurchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChurchEntity update = TestDataUtil.createTestChurchEntity(tenant);
        update.setChurchNameLocal("Updated Church");
        update.setDioceseLocal("ዝተሓደሰ ሃገረ ስብከት");
        update.setDescriptionLocal("ዝተሓደሰ ናይ ደብሪ መግለጺ");
        update.setStatus(ChurchStatus.ACTIVE);
        update.setPublicDirectoryEnabled(false);

        ChurchResponse response = churchService.updateChurch(1L, update);

        assertThat(church.getChurchNameLocal()).isEqualTo("Updated Church");
        assertThat(response.getDioceseLocal()).isEqualTo("ዝተሓደሰ ሃገረ ስብከት");
        assertThat(response.getDescriptionLocal()).isEqualTo("ዝተሓደሰ ናይ ደብሪ መግለጺ");
        verify(churchRepository).save(church);
        assertThat(church.getDioceseLocal()).isEqualTo("ዝተሓደሰ ሃገረ ስብከት");
        assertThat(church.getChurchNumber()).isNotBlank();
        assertThat(response.isPublicDirectoryEnabled()).isFalse();
    }

    @Test
    void testUpdateChurch_whenNotFound() {
        when(churchRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> churchService.updateChurch(1L, new ChurchEntity()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Church Not Found");
    }

    @Test
    void testDeleteChurch() {
        churchService.deleteChurch(1L);
        verify(churchRepository).deleteById(1L);
    }

    @Test
    void testFindOne_whenExists() {
        when(churchRepository.findById(1L)).thenReturn(Optional.of(church));
        Optional<ChurchEntity> result = churchService.findOne(1L);
        assertThat(result).isPresent().contains(church);
    }

    @Test
    void testFindOne_whenNotExists() {
        when(churchRepository.findById(1L)).thenReturn(Optional.empty());
        assertThat(churchService.findOne(1L)).isEmpty();
    }

    @Test
    void testCreateChurch_withValidTenantContext() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("MI1234");
        when(churchRepository.existsByChurchNumber("MI1234")).thenReturn(false);
        when(churchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChurchEntity newChurch = TestDataUtil.createTestChurchEntity(tenant);
        ChurchResponse result = churchService.createChurch(newChurch);

        assertThat(result.getChurchNumber()).isEqualTo("MI1234");
        assertThat(result.getTenantId()).isEqualTo(tenant.getId());
        verify(churchRepository).save(newChurch);
    }

    @Test
    void testCreateChurch_whenTenantIdIsMissing() {
        TenantContext.setTenantId(null);

        ChurchEntity church = new ChurchEntity();

        assertThatThrownBy(() -> churchService.createChurch(church))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tenant ID is not set in the context");
    }

    @Test
    void testCreateChurch_whenTenantNotFound() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> churchService.createChurch(new ChurchEntity()))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("No valid tenant found");
    }

    @Test
    void testGetChurches() {
        when(churchRepository.findAll()).thenReturn(List.of(church));
        List<ChurchEntity> result = churchService.getChurches();
        assertThat(result).containsExactly(church);
    }

    @Test
    void mappedReadMethods_areTransactionalReadOnly() throws NoSuchMethodException {
        Method method = ChurchServiceImpl.class.getMethod("findAll", org.springframework.data.domain.Pageable.class, String.class, Boolean.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }
}
