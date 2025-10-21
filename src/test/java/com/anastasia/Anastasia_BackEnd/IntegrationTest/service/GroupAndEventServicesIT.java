package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.CheckInQRRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.CheckInRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.MarkAbsentRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.report.EventReport;
import com.anastasia.Anastasia_BackEnd.model.group.AddManagersResponse;
import com.anastasia.Anastasia_BackEnd.model.group.AddUsersToGroupRequest;
import com.anastasia.Anastasia_BackEnd.model.group.BatchInviteRequest;
import com.anastasia.Anastasia_BackEnd.model.group.GroupDTO;
import com.anastasia.Anastasia_BackEnd.model.group.GroupManagerRequest;
import com.anastasia.Anastasia_BackEnd.model.group.RemoveManagersResponse;
import com.anastasia.Anastasia_BackEnd.model.group.RemoveUsersFromGroupRequest;
import com.anastasia.Anastasia_BackEnd.model.group.RemoveUsersFromGroupResponse;
import com.anastasia.Anastasia_BackEnd.model.group.SimpleGroupEntity;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleType;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.repository.GroupRepository;
import com.anastasia.Anastasia_BackEnd.service.event.EventAttendanceService;
import com.anastasia.Anastasia_BackEnd.service.event.EventReportService;
import com.anastasia.Anastasia_BackEnd.service.event.EventService;
import com.anastasia.Anastasia_BackEnd.service.event.QrCheckInService;
import com.anastasia.Anastasia_BackEnd.service.group.GroupService;
import com.anastasia.Anastasia_BackEnd.testsupport.ServiceIntegrationTestBase;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Integration Tests")
@Feature("Service Layer - Group & Event Domain")
@Transactional
class GroupAndEventServicesIT extends ServiceIntegrationTestBase {

    @Autowired private GroupService groupService;
    @Autowired private GroupRepository groupRepository;
    @Autowired private EventService eventService;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventAttendanceService attendanceService;
    @Autowired private EventAttendanceRepository attendanceRepository;
    @Autowired private EventReportService eventReportService;
    @Autowired private QrCheckInService qrCheckInService;

    private UserEntity managerUser;
    private UserEntity memberUser;
    private UserEntity extraUser;
    private UserEntity qrUser;

    @BeforeEach
    void initUsers() {
        MockitoAnnotations.openMocks(this);
        TenantContext.setTenantId(tenant.getId());

        Role ownerRole = fetchRole(RoleType.OWNER);
        Role adminRole = fetchRole(RoleType.ADMIN);

        managerUser = persistUser("manager+" + UUID.randomUUID() + "@integration.com", ownerRole);
        memberUser = persistUser("member+" + UUID.randomUUID() + "@integration.com", adminRole);
        extraUser = persistUser("extra+" + UUID.randomUUID() + "@integration.com", ownerRole);
        qrUser = persistUser("qr+" + UUID.randomUUID() + "@integration.com", ownerRole);
    }

    @Test
    void groupAndEventServices_endToEndFlow() {
        // ---- Group lifecycle ----
        GroupDTO groupDTO = TestDataUtil.createTestGroupDTO(church.getChurchNumber());
        groupDTO.setManagers(new HashSet<>(Set.of(managerUser.getUuid())));
        groupDTO.setUsers(new HashSet<>(Set.of(memberUser.getUuid())));

        SimpleGroupEntity simpleGroup = groupService.createGroup(groupDTO);
        assertThat(simpleGroup.getGroupId()).isNotNull();

        var savedGroup = groupRepository.findById(simpleGroup.getGroupId())
                .orElseThrow(() -> new AssertionError("Group not persisted"));
        assertThat(savedGroup.getManagers()).extracting(UserEntity::getUuid).contains(managerUser.getUuid());
        assertThat(savedGroup.getUsers()).extracting(UserEntity::getUuid).contains(memberUser.getUuid());

        groupService.addUsersToGroup(simpleGroup.getGroupId(),
                AddUsersToGroupRequest.builder().userIds(Set.of(extraUser.getUuid())).build());

        Page<SimpleUserDTO> membersPage = groupService.listGroupMembers(simpleGroup.getGroupId(), PageRequest.of(0, 10));
        assertThat(membersPage.getContent()).extracting(SimpleUserDTO::uuid).contains(memberUser.getUuid(), extraUser.getUuid());

        RemoveUsersFromGroupResponse removalResponse = groupService.removeMembersFromGroup(simpleGroup.getGroupId(),
                RemoveUsersFromGroupRequest.builder()
                        .userIds(new ArrayList<>(List.of(memberUser.getUuid())))
                        .build());
        assertThat(removalResponse.getRemovedCount()).isEqualTo(1);
        assertThat(removalResponse.getRemovedUserIds()).containsExactly(memberUser.getUuid());

        savedGroup = groupRepository.findById(simpleGroup.getGroupId()).orElseThrow();
        assertThat(savedGroup.getUsers()).extracting(UserEntity::getUuid).containsExactly(extraUser.getUuid());

        List<SimpleUserDTO> managers = groupService.getGroupManagers(simpleGroup.getGroupId());
        assertThat(managers).extracting(SimpleUserDTO::uuid).containsExactly(managerUser.getUuid());

        AddManagersResponse addManagersResponse = groupService.addManagersToGroup(simpleGroup.getGroupId(),
                GroupManagerRequest.builder().managerIds(Set.of(extraUser.getUuid())).build());
        assertThat(addManagersResponse.getAddedManagerIds()).containsExactly(extraUser.getUuid());

        RemoveManagersResponse removeManagersResponse = groupService.removeManagersFromGroup(simpleGroup.getGroupId(),
                GroupManagerRequest.builder().managerIds(Set.of(extraUser.getUuid())).build());
        assertThat(removeManagersResponse.getRemovedManagerIds()).containsExactly(extraUser.getUuid());

        List<com.anastasia.Anastasia_BackEnd.model.group.GroupUserCandidateDTO> userStatus =
                groupService.getGroupUserStatus(simpleGroup.getGroupId());
        assertThat(userStatus).isNotNull();

        BatchInviteRequest inviteRequest = BatchInviteRequest.builder()
                .groupEmails(new HashSet<>(Set.of(qrUser.getEmail())))
                .build();
        var inviteResponse = groupService.batchInviteUsersToGroup(simpleGroup.getGroupId(), inviteRequest);
        assertThat(inviteResponse.getInvitedCount()).isEqualTo(1);
        assertThat(inviteResponse.getInvitedUserIds()).containsExactly(qrUser.getUuid());

        // ---- Event lifecycle ----
        EventEntity event = EventEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .title("Integration Event")
                .description("Full flow test")
                .date(LocalDate.now())
                .location("Main Hall")
                .startTime(LocalTime.now().minusMinutes(10))
                .endTime(LocalTime.now().plusMinutes(50))
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();
        event.setCreatedDate(LocalDateTime.now());

        EventEntity savedEvent = eventService.createEvent(event);
        assertThat(savedEvent.getEventId()).isNotNull();

        EventEntity updatedEvent = EventEntity.builder()
                .title("Updated Event Title")
                .build();
        savedEvent.setTitle(updatedEvent.getTitle());
        EventEntity afterUpdate = eventService.updateEvent(savedEvent.getEventId(), savedEvent);
        assertThat(afterUpdate.getTitle()).isEqualTo("Updated Event Title");

        // Attendance operations
        attendanceService.checkIn(CheckInRequestDTO.builder()
                .eventId(savedEvent.getEventId())
                .userId(extraUser.getUuid())
                .checkInMethod("MANUAL")
                .checkedInBy(managerUser.getUuid())
                .build());

        attendanceService.markAbsent(MarkAbsentRequestDTO.builder()
                .eventId(savedEvent.getEventId())
                .userId(memberUser.getUuid())
                .markedAbsentBy(managerUser.getUuid())
                .build());

        qrCheckInService.checkInWithQR(CheckInQRRequestDTO.builder()
                .eventId(savedEvent.getEventId())
                .userId(qrUser.getUuid())
                .latitude(40.71281)
                .longitude(-74.00601)
                .build());

        assertThat(attendanceRepository.findByEventId(savedEvent.getEventId())).hasSize(3);
        assertThat(attendanceService.getAttendanceByUser(extraUser.getUuid())).hasSize(1);
        assertThat(attendanceService.getAttendanceByEventAndStatus(savedEvent.getEventId(), AttendanceStatus.CHECKED_IN))
                .hasSize(2);

        EventReport report = eventReportService.generateEventReport(savedEvent.getEventId());
        assertThat(report.getEventSummary().getInvitedCount()).isEqualTo(3);
        assertThat(report.getEventSummary().getCheckedInCount()).isEqualTo(2);
        assertThat(report.getEventSummary().getAbsentCount()).isEqualTo(1);
        assertThat(report.getUserAttendanceReport()).hasSize(3);

        // Clean-up
        eventService.deleteEvent(savedEvent.getEventId());
        assertThat(eventRepository.findById(savedEvent.getEventId())).isEmpty();
    }
}
