package com.anastasia.Anastasia_BackEnd.UnitTests.service.staff;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.CreateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.UpdateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffPositionType;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import com.anastasia.Anastasia_BackEnd.modules.staff.service.StaffServiceImpl;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Behaviors:
 * - Staff creation must enforce tenant context, unique emails, and valid hire/end date ranges before persisting.
 * - Updates must block illegal manager relationships and any tenant boundary violation.
 * Edge cases: duplicate emails, reversed hire/end dates, and self-referential managers.
 */
@LenientMockitoTest
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffServiceImplUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StaffRepository staffRepository;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private LocalizedMessageService messageService;

    @InjectMocks private StaffServiceImpl staffService;

    private final UUID tenantId = UUID.randomUUID();
    private TenantEntity tenant;
    private ChurchEntity church;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
        tenant = new TenantEntity();
        tenant.setId(tenantId);
        tenant.setOwnerName("Owner");
        church = new ChurchEntity();
        church.setChurchId(5L);
        church.setTenant(tenant);
        church.setChurchNumber("CH001");
        tenant.setChurch(church);
        when(messageService.get(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(messageService.get(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_shouldRejectWhenEmailAlreadyTaken() {
        CreateStaffRequest request = new CreateStaffRequest(
                "John Doe",
                "existing@example.com",
                "123456",
                church.getChurchId(),
                StaffPositionType.SECRETARY,
                StaffEmploymentStatus.ACTIVE,
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                null,
                null
        );
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> staffService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is already in use");
    }

    @Test
    void create_shouldRejectWhenEndDateBeforeHireDate() {
        CreateStaffRequest request = new CreateStaffRequest(
                "John Doe",
                "new@example.com",
                "123456",
                church.getChurchId(),
                StaffPositionType.SECRETARY,
                StaffEmploymentStatus.ACTIVE,
                null,
                null,
                null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(2),
                null,
                null
        );
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(churchRepository.findById(church.getChurchId())).thenReturn(Optional.of(church));

        assertThatThrownBy(() -> staffService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date cannot be earlier than hire date");
    }

    @Test
    void update_shouldRejectSelfReferencingManager() {
        UpdateStaffRequest request = new UpdateStaffRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1L,
                null
        );
        StaffEntity staff = StaffEntity.builder()
                .id(1L)
                .tenant(tenant)
                .user(new UserEntity())
                .build();
        staff.getUser().setEmail("staff@example.com");
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> staffService.update(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Staff cannot report to itself");
    }
}
