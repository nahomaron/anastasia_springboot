package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TenantServiceUnitTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private AuthService authService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private TenantServiceImpl tenantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void convertTenantToEntity_shouldMapCorrectly() {
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        TenantEntity entity = TestDataUtil.createTestTenantEntity();
        when(tenantMapper.tenantDTOToEntity(dto)).thenReturn(entity);

        TenantEntity result = tenantService.convertTenantToEntity(dto);
        assertThat(result).isEqualTo(entity);
    }

    @Test
    void convertTenantToDTO_shouldMapCorrectly() {
        TenantEntity entity = TestDataUtil.createTestTenantEntity();
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        when(tenantMapper.tenantEntityToDTO(entity)).thenReturn(dto);

        TenantDTO result = tenantService.convertTenantToDTO(entity);
        assertThat(result).isEqualTo(dto);
    }

    @Test
    void subscribeTenant_shouldSaveTenantAndCreateUser() throws MessagingException {
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        Role ownerRole = TestDataUtil.createTestOwnerRole(tenant);

        when(tenantRepository.save(any())).thenReturn(tenant);
        when(roleRepository.findByRoleName("OWNER")).thenReturn(Optional.of(ownerRole));

        tenantService.subscribeTenant(dto);

        verify(tenantRepository, times(1)).save(any());
        verify(authService, times(1)).createUser(any(UserEntity.class));
    }

    @Test
    void subscribeTenant_shouldThrowWhenRoleNotFound() {
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        when(roleRepository.findByRoleName("OWNER")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tenantService.subscribeTenant(dto));
    }

    @Test
    void findAll_shouldReturnPageOfTenants() {
        Page<TenantEntity> page = new PageImpl<>(List.of(TestDataUtil.createTestTenantEntity()));
        when(tenantRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<TenantEntity> result = tenantService.findAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getTenants_shouldReturnList() {
        when(tenantRepository.findAll()).thenReturn(List.of(TestDataUtil.createTestTenantEntity()));
        List<TenantEntity> tenants = tenantService.getTenants();
        assertThat(tenants).hasSize(1);
    }

    @Test
    void findTenantById_shouldReturnTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantEntity entity = TestDataUtil.createTestTenantEntity();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

        Optional<TenantEntity> found = tenantService.findTenantById(tenantId);
        assertThat(found).isPresent();
    }

    @Test
    void unsubscribeTenant_shouldDeactivateTenant() {
        TenantEntity entity = TestDataUtil.createTestTenantEntity();
        when(tenantRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        tenantService.unsubscribeTenant(entity.getId());

        assertThat(entity.isActiveTenant()).isFalse();
        verify(tenantRepository).save(entity);
    }

    @Test
    void unsubscribeTenant_shouldThrowIfNotFound() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> tenantService.unsubscribeTenant(tenantId));
    }
}

