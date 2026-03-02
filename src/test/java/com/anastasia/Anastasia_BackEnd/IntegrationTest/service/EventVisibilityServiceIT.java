package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventService;
import com.anastasia.Anastasia_BackEnd.TestSupport.ServiceIntegrationTestBase;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Integration Tests")
@Feature("Service Layer - Event Visibility")
@Transactional
class EventVisibilityServiceIT extends ServiceIntegrationTestBase {

    @Autowired
    private EventService eventService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity visibleUser;
    private UserEntity outsiderUser;
    private UserEntity managerOnlyUser;
    private GroupEntity visibleGroup;
    private GroupEntity outsiderGroup;

    @BeforeEach
    void setUpVisibilityContext() {
        TenantContext.setTenantId(tenant.getId());

        Role ownerRole = fetchRole(RoleType.OWNER);
        visibleUser = persistUser("visible+" + UUID.randomUUID() + "@it.com", ownerRole);
        outsiderUser = persistUser("outsider+" + UUID.randomUUID() + "@it.com", ownerRole);
        managerOnlyUser = persistUser("manager-only+" + UUID.randomUUID() + "@it.com", ownerRole);

        visibleGroup = createGroupWithMember("Visible Group", visibleUser);
        visibleGroup.getManagers().add(managerOnlyUser);
        visibleGroup = groupRepository.saveAndFlush(visibleGroup);
        outsiderGroup = createGroupWithMember("Outsider Group", outsiderUser);
    }

    @Test
    void getVisibleEventsForUser_respectsPerEventVisibilityRules() {
        EventEntity allEvent = createEvent("All Audience", EventVisibilityType.ALL, null, null);
        eventService.createEvent(allEvent);

        EventEntity groupEventAllowed = createEvent(
                "Group Access",
                EventVisibilityType.GROUPS,
                Set.of(visibleGroup),
                null
        );
        eventService.createEvent(groupEventAllowed);

        EventEntity groupEventDenied = createEvent(
                "Other Group Only",
                EventVisibilityType.GROUPS,
                Set.of(outsiderGroup),
                null
        );
        eventService.createEvent(groupEventDenied);

        EventEntity inviteEventAllowed = createEvent(
                "Direct Invite",
                EventVisibilityType.INVITEES,
                null,
                Set.of(visibleUser)
        );
        eventService.createEvent(inviteEventAllowed);

        EventEntity inviteEventDenied = createEvent(
                "Other Invitees",
                EventVisibilityType.INVITEES,
                null,
                Set.of(outsiderUser)
        );
        eventService.createEvent(inviteEventDenied);

        EventEntity managerEventAllowed = createEvent(
                "Manager Access",
                EventVisibilityType.MANAGERS,
                null,
                null
        );
        EventEntity savedManagerAllowed = eventService.createEvent(managerEventAllowed);
        assignManager(savedManagerAllowed, visibleUser, "LEAD");

        EventEntity managerEventDenied = createEvent(
                "Other Managers",
                EventVisibilityType.MANAGERS,
                null,
                null
        );
        EventEntity savedManagerDenied = eventService.createEvent(managerEventDenied);
        assignManager(savedManagerDenied, outsiderUser, "LEAD");

        List<EventDTO> visibleForTargetUser = eventService.getVisibleEventsForUser(visibleUser.getUuid());
        assertThat(visibleForTargetUser)
                .extracting(EventDTO::getTitle)
                .containsExactlyInAnyOrder(
                        "All Audience",
                        "Group Access",
                        "Direct Invite",
                        "Manager Access"
                )
                .doesNotContain("Other Group Only", "Other Invitees", "Other Managers");

        List<EventDTO> visibleForOutsider = eventService.getVisibleEventsForUser(outsiderUser.getUuid());
        assertThat(visibleForOutsider)
                .extracting(EventDTO::getTitle)
                .containsExactlyInAnyOrder(
                        "All Audience",
                        "Other Group Only",
                        "Other Invitees",
                        "Other Managers"
                )
                .doesNotContain("Group Access", "Direct Invite", "Manager Access");

        List<EventDTO> visibleForGroupManager = eventService.getVisibleEventsForUser(managerOnlyUser.getUuid());
        assertThat(visibleForGroupManager)
                .extracting(EventDTO::getTitle)
                .contains("All Audience", "Group Access")
                .doesNotContain("Other Group Only");
    }

    private GroupEntity createGroupWithMember(String groupName, UserEntity member) {
        GroupEntity group = GroupEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .groupName(groupName + " " + UUID.randomUUID())
                .visibility("PRIVATE")
                .build();
        group.addUser(member);
        return groupRepository.save(group);
    }

    private void assignManager(EventEntity event, UserEntity user, String role) {
        EventManagerEntity manager = EventManagerEntity.builder()
                .event(event)
                .user(user)
                .role(role)
                .build();
        entityManager.persist(manager);
        entityManager.flush();
    }

    private EventEntity createEvent(String title,
                                    EventVisibilityType visibility,
                                    Set<GroupEntity> invitedGroups,
                                    Set<UserEntity> invitedUsers) {

        EventEntity.EventEntityBuilder builder = EventEntity.builder()
                .church(church)
                .title(title)
                .description("Integration visibility test")
                .date(LocalDate.now().plusDays(1))
                .location("Integration Hall")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .visibility(visibility)
                .image("https://example.com/event.png");

        if (invitedGroups != null) {
            builder.invitedGroups(new HashSet<>(invitedGroups));
        }
        if (invitedUsers != null) {
            builder.invitedUsers(new HashSet<>(invitedUsers));
        }

        return builder.build();
    }
}
