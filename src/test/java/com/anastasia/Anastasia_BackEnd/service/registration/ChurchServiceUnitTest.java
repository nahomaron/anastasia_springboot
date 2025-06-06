package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChurchServiceUnitTest {

    @Mock private ChurchRepository churchRepository;
    @Mock private ChurchMapper churchMapper;
    @Mock private TenantRepository tenantRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private ChurchServiceImpl churchService;

    private ChurchEntity church;
    private TenantEntity tenant;

    @BeforeEach
    void setUp() {
        tenant = TestDataUtil.createTestTenantEntity();
        church = TestDataUtil.createTestChurchEntity(tenant);
    }

    @Test
    void testFindAllChurches() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ChurchEntity> expectedPage = new PageImpl<>(List.of(church));
        when(churchRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<ChurchEntity> result = churchService.findAll(pageable);

        assertThat(result).isEqualTo(expectedPage);
    }

    @Test
    void testExistsById() {
        when(churchRepository.existsById(1L)).thenReturn(true);
        assertThat(churchService.exists(1L)).isTrue();
    }

    @Test
    void testUpdateChurch_whenExists() {
        when(churchRepository.findById(1L)).thenReturn(Optional.of(church));

        ChurchEntity update = TestDataUtil.createTestChurchEntity(tenant);
        update.setChurchName("Updated Church");

        churchService.updateChurch(1L, update);

        verify(churchRepository).save(update);
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
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("CH1234");
        when(churchRepository.existsByChurchNumber("CH1234")).thenReturn(false);
        when(churchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChurchEntity newChurch = TestDataUtil.createTestChurchEntity(tenant);
        String result = churchService.createChurch(newChurch);

        assertThat(result).isEqualTo("CH1234");
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
}
