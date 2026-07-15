package com.anastasia.Anastasia_BackEnd.UnitTests.calendar;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntrySourceType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.service.CalendarEntryServiceImpl;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class CalendarEntryServiceImplUnitTest {

    @Mock private CalendarEntryRepository entryRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;

    @InjectMocks private CalendarEntryServiceImpl service;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEntry_shouldRejectNonAppointmentTypeForAppointmentOnlyUser() {
        TenantContext.setTenantId(UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pw", "MANAGE_APPOINTMENT")
        );

        CalendarEntryRequest request = request(CalendarEntryType.EVENT);

        assertThatThrownBy(() -> service.createEntry(request, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Appointment-scoped users");
    }

    @Test
    void updateEntry_shouldRejectNonAppointmentEntriesForAppointmentOnlyUser() {
        UUID tenantId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pw", "MANAGE_APPOINTMENT")
        );

        CalendarEntryEntity entry = CalendarEntryEntity.builder()
                .id(entryId)
                .tenantId(tenantId)
                .type(CalendarEntryType.EVENT)
                .title("Existing")
                .calendarSystem(CalendarSystem.GREGORIAN)
                .startAtUtc(Instant.parse("2026-01-01T10:00:00Z"))
                .timezone("UTC")
                .visibility(CalendarVisibility.PUBLIC)
                .status(CalendarEntryStatus.SCHEDULED)
                .sourceEntityType(CalendarEntrySourceType.MANUAL)
                .build();
        when(entryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.updateEntry(entryId, request(CalendarEntryType.APPOINTMENT), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only modify appointment calendar entries");
    }

    @Test
    void updateEntry_shouldPreserveExistingOwner() {
        UUID tenantId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID originalOwnerId = UUID.randomUUID();
        UUID updaterId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pw", "MANAGE_EVENTS")
        );

        UserEntity originalOwner = new UserEntity();
        originalOwner.setUuid(originalOwnerId);
        originalOwner.setTenantId(tenantId);

        CalendarEntryEntity entry = CalendarEntryEntity.builder()
                .id(entryId)
                .tenantId(tenantId)
                .ownerUser(originalOwner)
                .type(CalendarEntryType.EVENT)
                .title("Existing")
                .calendarSystem(CalendarSystem.GREGORIAN)
                .startAtUtc(Instant.parse("2026-01-01T10:00:00Z"))
                .timezone("UTC")
                .visibility(CalendarVisibility.PUBLIC)
                .status(CalendarEntryStatus.SCHEDULED)
                .sourceEntityType(CalendarEntrySourceType.MANUAL)
                .build();
        entry.setOwnerUserId(originalOwnerId);

        when(entryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(entryRepository.save(any(CalendarEntryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateEntry(entryId, request(CalendarEntryType.EVENT), updaterId);

        assertThat(entry.getOwnerUser()).isSameAs(originalOwner);
        assertThat(entry.getOwnerUserId()).isEqualTo(originalOwnerId);
        verify(userRepository, never()).findById(updaterId);
    }

    @Test
    void updateEntry_shouldAllowAppointmentScopedUsersToUpdateAppointmentEntries() {
        UUID tenantId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pw", "MANAGE_APPOINTMENT")
        );

        TenantEntity tenant = new TenantEntity();
        tenant.setId(tenantId);

        ChurchEntity church = new ChurchEntity();
        church.setChurchId(1L);
        church.setTenant(tenant);

        UserEntity owner = new UserEntity();
        owner.setUuid(ownerId);
        owner.setTenantId(tenantId);

        CalendarEntryEntity entry = CalendarEntryEntity.builder()
                .id(entryId)
                .tenantId(tenantId)
                .church(church)
                .ownerUser(owner)
                .type(CalendarEntryType.APPOINTMENT)
                .title("Appointment")
                .calendarSystem(CalendarSystem.GREGORIAN)
                .startAtUtc(Instant.parse("2026-01-01T10:00:00Z"))
                .timezone("UTC")
                .visibility(CalendarVisibility.PRIVATE)
                .status(CalendarEntryStatus.SCHEDULED)
                .sourceEntityType(CalendarEntrySourceType.MANUAL)
                .build();

        when(entryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(entryRepository.save(any(CalendarEntryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateEntry(entryId, request(CalendarEntryType.APPOINTMENT), ownerId);

        verify(entryRepository).save(entry);
    }

    @Test
    void updateEntry_shouldRejectCrossTenantUpdates() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pw", "MANAGE_EVENTS")
        );

        TenantEntity tenant = new TenantEntity();
        tenant.setId(otherTenantId);

        ChurchEntity church = new ChurchEntity();
        church.setChurchId(1L);
        church.setTenant(tenant);

        CalendarEntryEntity entry = CalendarEntryEntity.builder()
                .id(entryId)
                .tenantId(otherTenantId)
                .church(church)
                .type(CalendarEntryType.EVENT)
                .title("Existing")
                .calendarSystem(CalendarSystem.GREGORIAN)
                .startAtUtc(Instant.parse("2026-01-01T10:00:00Z"))
                .timezone("UTC")
                .visibility(CalendarVisibility.PUBLIC)
                .status(CalendarEntryStatus.SCHEDULED)
                .sourceEntityType(CalendarEntrySourceType.MANUAL)
                .build();

        when(entryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.updateEntry(entryId, request(CalendarEntryType.EVENT), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("current tenant");
    }

    private CalendarEntryRequest request(CalendarEntryType type) {
        return new CalendarEntryRequest(
                type,
                "Title",
                "Description",
                CalendarSystem.GREGORIAN,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"),
                "UTC",
                false,
                CalendarVisibility.PUBLIC,
                Set.of(),
                null,
                Set.of(),
                Set.of()
        );
    }
}
