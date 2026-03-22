package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.Repetition;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MaritalStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDemoWorkspaceSeederService {

    private static final String DEMO_SOURCE = "DEMO_SEED";
    private static final String DEFAULT_TIMEZONE = "UTC";

    private final ChurchRepository churchRepository;
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final EventRepository eventRepository;

    @Transactional
    public void seedDemoWorkspace(TenantEntity tenant, UserEntity owner) {
        ChurchEntity church = churchRepository.findByTenantId(tenant.getId())
                .orElse(null);
        if (church == null) {
            log.warn("Skipping demo workspace seeding for tenant {} because no church was provisioned.", tenant.getId());
            return;
        }

        seedMembersIfMissing(tenant, church, owner);
        List<GroupEntity> groups = seedGroupsIfMissing(tenant, church, owner);
        seedEventsIfMissing(tenant, church, owner, groups);
    }

    private void seedMembersIfMissing(TenantEntity tenant, ChurchEntity church, UserEntity owner) {
        if (memberRepository.countByTenantId(tenant.getId()) > 0) {
            return;
        }

        Instant now = Instant.now();
        List<Adult_MemberEntity> members = new ArrayList<>();
        members.add(buildMember(tenant, church, owner, now, "M10001", "Daniel", "Tesfai", "Abraha", "Martha", "Michael", MemberGender.MALE, LocalDate.of(1992, 3, 18), MaritalStatus.MARRIED, "Teacher"));
        members.add(buildMember(tenant, church, owner, now, "M10002", "Rahel", "Gebru", "Tekle", "Selam", "Berhe", MemberGender.FEMALE, LocalDate.of(1988, 7, 9), MaritalStatus.MARRIED, "Nurse"));
        members.add(buildMember(tenant, church, owner, now, "M10003", "Yonas", "Kiros", "Hagos", "Mimi", "Samuel", MemberGender.MALE, LocalDate.of(1998, 11, 2), MaritalStatus.SINGLE, "Engineer"));
        members.add(buildMember(tenant, church, owner, now, "M10004", "Helen", "Isaac", "Petros", "Aster", "Yosef", MemberGender.FEMALE, LocalDate.of(1995, 1, 24), MaritalStatus.SINGLE, "Accountant"));
        members.add(buildMember(tenant, church, owner, now, "M10005", "Samuel", "Haile", "Ghebre", "Mebrat", "Araya", MemberGender.MALE, LocalDate.of(1979, 5, 30), MaritalStatus.MARRIED, "Choir Director"));
        members.add(buildMember(tenant, church, owner, now, "M10006", "Saba", "Meron", "Kidane", "Roman", "Asmelash", MemberGender.FEMALE, LocalDate.of(2001, 9, 14), MaritalStatus.SINGLE, "Student"));
        memberRepository.saveAll(members);
    }

    private Adult_MemberEntity buildMember(TenantEntity tenant,
                                           ChurchEntity church,
                                           UserEntity owner,
                                           Instant now,
                                           String membershipNumber,
                                           String firstName,
                                           String fatherName,
                                           String grandFatherName,
                                           String motherName,
                                           String mothersFather,
                                           MemberGender gender,
                                           LocalDate birthday,
                                           MaritalStatus maritalStatus,
                                           String profession) {
        Adult_MemberEntity member = Adult_MemberEntity.builder()
                .tenantId(tenant.getId())
                .churchNumber(church.getChurchNumber())
                .church(church)
                .membershipNumber(membershipNumber)
                .statusValue(MemberLifecycleStatus.ACTIVE)
                .title(gender == MemberGender.FEMALE ? "Ms." : "Mr.")
                .firstName(firstName)
                .fatherName(fatherName)
                .grandFatherName(grandFatherName)
                .motherName(motherName)
                .mothersFather(mothersFather)
                .firstNameLocal(firstName)
                .fatherNameLocal(fatherName)
                .grandFatherNameLocal(grandFatherName)
                .motherFullNameLocal(motherName)
                .genderValue(gender)
                .birthday(birthday)
                .nationality("Eritrean")
                .placeOfBirth("Asmara")
                .email((firstName + "." + fatherName + "@demo.anastasia.local").toLowerCase())
                .phone("+291700000000")
                .whatsApp("+291700000000")
                .emergencyContactNumber("+291700000001")
                .contactRelation("Family")
                .eritreaContact("+291700000002")
                .maritalStatus(maritalStatus)
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .profession(profession)
                .fatherOfConfession("Abune Michael")
                .termsAccepted(true)
                .termsVersion("demo-seed-v1")
                .termsAcceptedAt(now)
                .registeredAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .approvedAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .statusChangedAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .sourceSystem(DEMO_SOURCE)
                .externalId("member:" + membershipNumber)
                .address(Address.builder()
                        .addressLine1("Main Street")
                        .city("Asmara")
                        .stateProvince("Maekel")
                        .country("Eritrea")
                        .postalCode("1001")
                        .build())
                .build();
        member.setApprovedByChurch(true);
        member.setApprovedByPriest(true);
        member.setCreatedBy(owner.getUuid());
        member.setUpdatedBy(owner.getUuid());
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        return member;
    }

    private List<GroupEntity> seedGroupsIfMissing(TenantEntity tenant, ChurchEntity church, UserEntity owner) {
        if (groupRepository.countByTenantId(tenant.getId()) > 0) {
            return groupRepository.findAllByTenantId(tenant.getId(), org.springframework.data.domain.Pageable.unpaged())
                    .getContent();
        }

        Instant now = Instant.now();
        List<GroupEntity> groups = List.of(
                buildGroup(tenant, church, owner, now, "Young Adults", "Weekly fellowship, mentorship, and service opportunities."),
                buildGroup(tenant, church, owner, now, "Choir Ministry", "Rehearsals, liturgy planning, and feast day preparation."),
                buildGroup(tenant, church, owner, now, "Community Care", "Volunteer coordination for home visits and member support.")
        );
        return groupRepository.saveAll(groups);
    }

    private GroupEntity buildGroup(TenantEntity tenant,
                                   ChurchEntity church,
                                   UserEntity owner,
                                   Instant now,
                                   String name,
                                   String description) {
        GroupEntity group = GroupEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .groupName(name)
                .description(description)
                .visibility("PUBLIC")
                .build();
        group.setCreatedBy(owner.getUuid());
        group.setUpdatedBy(owner.getUuid());
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        return group;
    }

    private void seedEventsIfMissing(TenantEntity tenant,
                                     ChurchEntity church,
                                     UserEntity owner,
                                     List<GroupEntity> groups) {
        if (eventRepository.countByTenantId(tenant.getId()) > 0) {
            return;
        }

        Instant now = Instant.now();
        List<EventEntity> events = List.of(
                buildEvent(tenant, church, owner, now, groups, "Sunday Liturgy", "Main sanctuary", 7, 9, EventType.LITURGY),
                buildEvent(tenant, church, owner, now, groups, "Choir Practice", "Choir room", 10, 18, EventType.MEETING),
                buildEvent(tenant, church, owner, now, groups, "Young Adults Fellowship", "Parish hall", 14, 17, EventType.YOUTH_EVENT),
                buildEvent(tenant, church, owner, now, groups, "Volunteer Planning Night", "Meeting room A", 21, 18, EventType.MEETING)
        );
        eventRepository.saveAll(events);
    }

    private EventEntity buildEvent(TenantEntity tenant,
                                   ChurchEntity church,
                                   UserEntity owner,
                                   Instant now,
                                   List<GroupEntity> groups,
                                   String title,
                                   String location,
                                   int daysFromNow,
                                   int startHourUtc,
                                   EventType type) {
        Instant startAt = LocalDateTime.of(LocalDate.now().plusDays(daysFromNow), LocalTime.of(startHourUtc, 0))
                .toInstant(ZoneOffset.UTC);
        Instant endAt = startAt.plusSeconds(2 * 60L * 60L);

        EventEntity event = EventEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .title(title)
                .description("Sample event generated for a demo workspace.")
                .location(location)
                .startAt(startAt)
                .endAt(endAt)
                .timezone(DEFAULT_TIMEZONE)
                .type(type)
                .visibility(EventVisibilityType.ALL)
                .repetition(Repetition.NONE)
                .build();
        event.setCreatedBy(owner.getUuid());
        event.setUpdatedBy(owner.getUuid());
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return event;
    }
}
