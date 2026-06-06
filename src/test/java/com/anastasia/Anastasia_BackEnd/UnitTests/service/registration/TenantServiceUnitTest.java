package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantServiceImpl;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@LenientMockitoTest
public class TenantServiceUnitTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantMapper tenantMapper;
    @Mock private ChurchMapper churchMapper;
    @Mock private AuthService authService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleRepository roleRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private LocalizedMessageService messageService;

    @InjectMocks
    private TenantServiceImpl tenantService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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

//    @Test
//    void subscribeTenant_shouldSaveTenantAndCreateUser() throws MessagingException {
//        TenantDTO dto = TestDataUtil.createTestTenantDTO();
//        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
//        Role ownerRole = TestDataUtil.createTestOwnerRole(tenant);
//
//        when(tenantRepository.save(any())).thenReturn(tenant);
//        when(roleRepository.findByRoleName("OWNER")).thenReturn(Optional.of(ownerRole));
//
//        tenantService.subscribeTenant(dto);
//
//        verify(tenantRepository, times(1)).save(any());
//        verify(authService, times(1)).createUser(any(UserEntity.class));
//    }

    @Test
    void subscribeTenant_shouldSaveTenantAndCreateUser() throws MessagingException {
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        dto.setPhoneNumber("1234567890");

        TenantEntity tenantEntity = TestDataUtil.createTestTenantEntity();
        when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity);
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("ST12345");
        when(churchRepository.existsByChurchNumber(anyString())).thenReturn(false);
        ChurchEntity churchEntity = TestDataUtil.createTestChurchEntity(tenantEntity);
        when(churchMapper.churchDTOToEntity(any())).thenReturn(churchEntity);
        when(churchRepository.save(any(ChurchEntity.class))).thenReturn(churchEntity);

        Role ownerRole = TestDataUtil.createTestOwnerRole(tenantEntity);
        Role adminRole = Role.builder().roleName("ADMIN").build();
        when(roleRepository.findByRoleName("OWNER")).thenReturn(Optional.of(ownerRole));
        when(roleRepository.findByRoleName("PRIMARY_ADMIN")).thenReturn(Optional.of(adminRole));

        tenantService.subscribeTenant(dto);

        verify(tenantRepository, times(2)).save(any(TenantEntity.class));
        verify(authService, times(1)).createUser(any(UserEntity.class));
        verify(tenantRepository).save(argThat(saved ->
                saved.isPhoneVerified() && saved.getPhoneVerifiedAt() != null
        ));
        // Optionally, verify no other interactions if strict mocks are desired
//         verifyNoMoreInteractions(tenantRepository, roleRepository, authService);
    }

    @Test
    void subscribeTenant_shouldRejectPaidPlansInLegacyFlow() {
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        dto.setSubscriptionPlan(SubscriptionPlan.BASIC);

        assertThrows(AccessDeniedException.class, () -> tenantService.subscribeTenant(dto));
        verifyNoInteractions(tenantRepository, churchRepository, authService);
    }

    @Test
    void subscribeTenant_shouldThrowWhenRoleNotFound() {
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        assertThrows(RuntimeException.class, () -> tenantService.subscribeTenant(dto));
    }

    @Test
    void findAll_shouldReturnPageOfTenants() {
        TenantEntity entity = TestDataUtil.createTestTenantEntity();
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        Page<TenantEntity> page = new PageImpl<>(List.of(entity));
        when(tenantRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(tenantMapper.tenantEntityToDTO(entity)).thenReturn(dto);

        Page<TenantDTO> result = tenantService.findAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getTenants_shouldReturnList() {
        when(tenantRepository.findAll()).thenReturn(List.of(TestDataUtil.createTestTenantEntity()));
        List<TenantEntity> tenants = tenantService.getTenants();
        assertThat(tenants).hasSize(1);
    }

    @Test
    void findTenantEntityById_shouldReturnTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantEntity entity = TestDataUtil.createTestTenantEntity();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(entity));

        Optional<TenantEntity> found = tenantService.findTenantEntityById(tenantId);
        assertThat(found).isPresent();
    }

    @Test
    void unsubscribeTenant_shouldDeactivateTenant() {
        TenantEntity entity = TestDataUtil.createTestTenantEntity();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "platform-admin",
                        null,
                        List.of(new SimpleGrantedAuthority("MANAGE_TENANTS"))
                )
        );
        when(tenantRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        tenantService.unsubscribeTenant(entity.getId());

        assertThat(entity.getStatus()).isEqualTo(com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus.DEACTIVATED);
        verify(tenantRepository).save(entity);
    }

    @Test
    void unsubscribeTenant_shouldThrowIfNotFound() {
        UUID tenantId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "platform-admin",
                        null,
                        List.of(new SimpleGrantedAuthority("MANAGE_TENANTS"))
                )
        );
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> tenantService.unsubscribeTenant(tenantId));
    }

    @Test
    void unsubscribeTenant_shouldDenyCrossTenantMutationForScopedUser() {
        TenantEntity actorTenant = TestDataUtil.createTestTenantEntity();
        TenantEntity targetTenant = TestDataUtil.createTestTenantEntity();
        UserEntity user = TestDataUtil.createTestUserEntityA();
        user.setTenant(actorTenant);

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("OWN_SUBSCRIPTION"))
                )
        );

        assertThrows(AccessDeniedException.class, () -> tenantService.unsubscribeTenant(targetTenant.getId()));
        verify(tenantRepository, never()).findById(targetTenant.getId());
        verify(tenantRepository, never()).save(any(TenantEntity.class));
    }

    @Test
    void updateTenant_shouldDenyCrossTenantMutationForScopedUser() {
        TenantEntity actorTenant = TestDataUtil.createTestTenantEntity();
        TenantEntity targetTenant = TestDataUtil.createTestTenantEntity();
        UserEntity user = TestDataUtil.createTestUserEntityA();
        user.setTenant(actorTenant);

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("MANAGE_TENANT_BILLING"))
                )
        );

        assertThrows(AccessDeniedException.class, () -> tenantService.updateTenant(targetTenant.getId(), TestDataUtil.createTestTenantDTO()));
        verify(tenantRepository, never()).findById(targetTenant.getId());
        verify(tenantRepository, never()).save(any(TenantEntity.class));
    }

    @Test
    void unsubscribeTenant_shouldAllowPlatformTenantManagersAcrossTenants() {
        TenantEntity targetTenant = TestDataUtil.createTestTenantEntity();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "platform-admin",
                        null,
                        List.of(new SimpleGrantedAuthority("MANAGE_TENANTS"))
                )
        );
        when(tenantRepository.findById(targetTenant.getId())).thenReturn(Optional.of(targetTenant));

        tenantService.unsubscribeTenant(targetTenant.getId());

        assertThat(targetTenant.getStatus()).isEqualTo(com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus.DEACTIVATED);
        verify(tenantRepository).save(targetTenant);
    }

    @Test
    void mappedReadMethods_areTransactionalReadOnly() throws NoSuchMethodException {
        assertTransactionalReadOnly("findAll", org.springframework.data.domain.Pageable.class);
        assertTransactionalReadOnly("findTenantDtoById", UUID.class);
        assertTransactionalReadOnly("findTenantDtoByPhoneNumber", String.class);
    }

    private void assertTransactionalReadOnly(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = TenantServiceImpl.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }
}
