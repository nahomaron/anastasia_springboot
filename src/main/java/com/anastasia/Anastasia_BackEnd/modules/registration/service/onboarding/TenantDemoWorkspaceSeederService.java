package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentParticipantEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentSource;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatusHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AssignedRole;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ContactPreference;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.LocationType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ParticipantRole;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntrySourceType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.service.ChurchCalendarSeedService;
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
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MaritalStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffPositionType;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDemoWorkspaceSeederService {

    private static final String DEMO_SOURCE = "DEMO_SEED";
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEMO_PASSWORD = "{demo-seed}";

    private final ChurchRepository churchRepository;
    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final PriestRepository priestRepository;
    private final StaffRepository staffRepository;
    private final GroupRepository groupRepository;
    private final EventRepository eventRepository;
    private final CalendarEntryRepository calendarEntryRepository;
    private final ChurchCalendarSeedService churchCalendarSeedService;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seedDemoWorkspace(TenantEntity tenant, UserEntity owner) {
        ChurchEntity church = churchRepository.findByTenantId(tenant.getId())
                .orElse(null);
        if (church == null) {
            log.warn("Skipping demo workspace seeding for tenant {} because no church was provisioned.", tenant.getId());
            return;
        }

        Instant baseInstant = tenant.getActivatedAt() != null ? tenant.getActivatedAt() : Instant.now();
        List<Adult_MemberEntity> adults = seedMembersIfMissing(tenant, church, owner);
        List<Child_MemberEntity> children = seedChildrenIfMissing(tenant, church, owner, adults);
        List<PriestEntity> priests = seedPriestsIfMissing(tenant, church, owner, baseInstant);
        List<StaffEntity> staff = seedStaffIfMissing(tenant, church, owner, baseInstant);
        List<GroupEntity> groups = seedGroupsIfMissing(tenant, church, owner);
        seedEventsIfMissing(tenant, church, owner, groups);
        seedCalendarEntriesIfMissing(tenant, church, owner, baseInstant);
        seedAppointmentsIfMissing(tenant, church, owner, adults, children, priests, staff, baseInstant);
    }

    private List<Adult_MemberEntity> seedMembersIfMissing(TenantEntity tenant, ChurchEntity church, UserEntity owner) {
        if (memberRepository.countByTenantId(tenant.getId()) > 0) {
            return memberRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId(), Pageable.ofSize(20)).getContent();
        }

        Instant now = Instant.now();
        List<Adult_MemberEntity> members = new ArrayList<>();
        members.add(buildMember(tenant, church, owner, now, "M10001", "Daniel", "Tesfai", "Abraha", "Martha", "Michael", MemberGender.MALE, LocalDate.of(1992, 3, 18), MaritalStatus.MARRIED, "Teacher"));
        members.add(buildMember(tenant, church, owner, now, "M10002", "Rahel", "Gebru", "Tekle", "Selam", "Berhe", MemberGender.FEMALE, LocalDate.of(1988, 7, 9), MaritalStatus.MARRIED, "Nurse"));
        members.add(buildMember(tenant, church, owner, now, "M10003", "Yonas", "Kiros", "Hagos", "Mimi", "Samuel", MemberGender.MALE, LocalDate.of(1998, 11, 2), MaritalStatus.SINGLE, "Engineer"));
        members.add(buildMember(tenant, church, owner, now, "M10004", "Helen", "Isaac", "Petros", "Aster", "Yosef", MemberGender.FEMALE, LocalDate.of(1995, 1, 24), MaritalStatus.SINGLE, "Accountant"));
        members.add(buildMember(tenant, church, owner, now, "M10005", "Samuel", "Haile", "Ghebre", "Mebrat", "Araya", MemberGender.MALE, LocalDate.of(1979, 5, 30), MaritalStatus.MARRIED, "Choir Director"));
        members.add(buildMember(tenant, church, owner, now, "M10006", "Saba", "Meron", "Kidane", "Roman", "Asmelash", MemberGender.FEMALE, LocalDate.of(2001, 9, 14), MaritalStatus.SINGLE, "Student"));
        return memberRepository.saveAll(members);
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

    private List<Child_MemberEntity> seedChildrenIfMissing(TenantEntity tenant,
                                                           ChurchEntity church,
                                                           UserEntity owner,
                                                           List<Adult_MemberEntity> adults) {
        if (childRepository.countByTenantId(tenant.getId()) > 0) {
            return childRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId(), Pageable.ofSize(20)).getContent();
        }
        if (adults.size() < 2) {
            return Collections.emptyList();
        }

        Adult_MemberEntity father = adults.get(0);
        Adult_MemberEntity mother = adults.get(1);
        Instant now = Instant.now();

        List<Child_MemberEntity> children = List.of(
                buildChild(tenant, church, owner, now, "C20001", "Mikal", "Daniel", "Tesfai", "Rahel", "Berhe", LocalDate.of(2017, 4, 12), MemberGender.MALE, father, mother),
                buildChild(tenant, church, owner, now, "C20002", "Selam", "Daniel", "Tesfai", "Rahel", "Berhe", LocalDate.of(2020, 8, 6), MemberGender.FEMALE, father, mother)
        );
        return childRepository.saveAll(children);
    }

    private Child_MemberEntity buildChild(TenantEntity tenant,
                                          ChurchEntity church,
                                          UserEntity owner,
                                          Instant now,
                                          String membershipNumber,
                                          String firstName,
                                          String fatherName,
                                          String grandFatherName,
                                          String motherName,
                                          String mothersFather,
                                          LocalDate birthday,
                                          MemberGender gender,
                                          Adult_MemberEntity father,
                                          Adult_MemberEntity mother) {
        Child_MemberEntity child = Child_MemberEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .churchNumber(church.getChurchNumber())
                .membershipNumber(membershipNumber)
                .statusValue(MemberLifecycleStatus.ACTIVE)
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
                .phone("+291755000100")
                .email((firstName + "." + fatherName + "@demo.anastasia.local").toLowerCase(Locale.ROOT))
                .primaryGuardianPhone("+291755000101")
                .guardianRelationship("Parent")
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .fatherOfConfession("Abune Michael")
                .father(father)
                .mother(mother)
                .sourceSystem(DEMO_SOURCE)
                .externalId("child:" + membershipNumber)
                .registeredAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .approvedAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .statusChangedAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .address(Address.builder()
                        .addressLine1("Main Street")
                        .city("Asmara")
                        .stateProvince("Maekel")
                        .country("Eritrea")
                        .postalCode("1001")
                        .build())
                .build();
        child.setApprovedByChurch(true);
        child.setCreatedBy(owner.getUuid());
        child.setUpdatedBy(owner.getUuid());
        child.setCreatedAt(now);
        child.setUpdatedAt(now);
        return child;
    }

    private List<PriestEntity> seedPriestsIfMissing(TenantEntity tenant,
                                                    ChurchEntity church,
                                                    UserEntity owner,
                                                    Instant baseInstant) {
        if (priestRepository.countByChurch_Tenant_Id(tenant.getId()) > 0) {
            return priestRepository.findByChurch_ChurchId(church.getChurchId());
        }

        Instant now = Instant.now();
        UserEntity priestUser = createDemoUser(
                tenant,
                owner,
                "abune.michael." + tenant.getId().toString().substring(0, 8) + "@demo.anastasia.local",
                "Abune Michael Tesfai",
                UserType.PRIEST,
                RoleType.PRIEST,
                "+291733000100",
                now
        );

        PriestEntity priest = PriestEntity.builder()
                .priestNumber("P" + tenant.getId().toString().substring(0, 5).toUpperCase(Locale.ROOT))
                .user(priestUser)
                .church(church)
                .churchNumber(church.getChurchNumber())
                .status(PriestStatus.ACTIVE)
                .spiritualChildren(24)
                .prefixes("Abune")
                .firstName("Michael")
                .fatherName("Tesfai")
                .grandFatherName("Abraha")
                .phoneNumber("+291733000101")
                .churchEmail("abune.michael@" + church.getChurchNumber().toLowerCase(Locale.ROOT) + ".demo.local")
                .birthdate(LocalDate.of(1978, 2, 9).toString())
                .languages(new HashSet<>(Set.of("Tigrinya", "English")))
                .levelOfEducation("Theology")
                .address(Address.builder()
                        .addressLine1("Church residence")
                        .city("Asmara")
                        .stateProvince("Maekel")
                        .country("Eritrea")
                        .postalCode("1001")
                        .build())
                .isActive(true)
                .build();
        return priestRepository.saveAll(List.of(priest));
    }

    private List<StaffEntity> seedStaffIfMissing(TenantEntity tenant,
                                                 ChurchEntity church,
                                                 UserEntity owner,
                                                 Instant baseInstant) {
        if (staffRepository.countByTenant_Id(tenant.getId()) > 0) {
            return staffRepository.findByTenant_Id(tenant.getId());
        }

        Instant now = Instant.now();
        UserEntity secretaryUser = createDemoUser(
                tenant,
                owner,
                "hana.secretary." + tenant.getId().toString().substring(0, 8) + "@demo.anastasia.local",
                "Hana Ghebre",
                UserType.STAFF,
                RoleType.STAFF,
                "+291744000100",
                now
        );
        UserEntity coordinatorUser = createDemoUser(
                tenant,
                owner,
                "yonas.events." + tenant.getId().toString().substring(0, 8) + "@demo.anastasia.local",
                "Yonas Mehari",
                UserType.STAFF,
                RoleType.STAFF,
                "+291744000101",
                now
        );

        StaffEntity secretary = StaffEntity.builder()
                .staffNumber("S" + tenant.getId().toString().substring(0, 5).toUpperCase(Locale.ROOT) + "1")
                .tenant(tenant)
                .church(church)
                .churchNumber(church.getChurchNumber())
                .user(secretaryUser)
                .positionType(StaffPositionType.SECRETARY)
                .employmentStatus(StaffEmploymentStatus.ACTIVE)
                .department("Administration")
                .primaryPhone("+291744000200")
                .hireDate(LocalDate.ofInstant(baseInstant, ZoneOffset.UTC))
                .notes("Demo staff account for administrative workflows.")
                .invitedAt(now)
                .inviteAcceptedAt(now)
                .firstLoginAt(now)
                .lastCredentialResetAt(now)
                .build();
        secretary.setCreatedBy(owner.getUuid());
        secretary.setUpdatedBy(owner.getUuid());
        secretary.setCreatedAt(now);
        secretary.setUpdatedAt(now);

        StaffEntity coordinator = StaffEntity.builder()
                .staffNumber("S" + tenant.getId().toString().substring(0, 5).toUpperCase(Locale.ROOT) + "2")
                .tenant(tenant)
                .church(church)
                .churchNumber(church.getChurchNumber())
                .user(coordinatorUser)
                .positionType(StaffPositionType.EVENTS_COORDINATOR)
                .employmentStatus(StaffEmploymentStatus.ACTIVE)
                .department("Ministry")
                .primaryPhone("+291744000201")
                .hireDate(LocalDate.ofInstant(baseInstant.plusSeconds(7 * 24 * 60L * 60L), ZoneOffset.UTC))
                .notes("Demo staff account for scheduling and events.")
                .invitedAt(now)
                .inviteAcceptedAt(now)
                .firstLoginAt(now)
                .lastCredentialResetAt(now)
                .build();
        coordinator.setCreatedBy(owner.getUuid());
        coordinator.setUpdatedBy(owner.getUuid());
        coordinator.setCreatedAt(now);
        coordinator.setUpdatedAt(now);

        return staffRepository.saveAll(List.of(secretary, coordinator));
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

    private void seedCalendarEntriesIfMissing(TenantEntity tenant,
                                              ChurchEntity church,
                                              UserEntity owner,
                                              Instant baseInstant) {
        boolean hadExistingEntries = calendarEntryRepository.countByTenantId(tenant.getId()) > 0;

        churchCalendarSeedService.seedDefaults(tenant, church, owner);
        if (hadExistingEntries) {
            return;
        }

        Instant now = Instant.now();
        List<CalendarEntryEntity> entries = List.of(
                buildCalendarEntry(tenant, church, owner, now, baseInstant.plusSeconds(1 * 24 * 60L * 60L), 2, "Welcome orientation", "A short walkthrough of the seeded workspace and key modules.", CalendarEntryType.PARISH_EVENT, Set.of(CalendarCategory.EVENTS)),
                buildCalendarEntry(tenant, church, owner, now, baseInstant.plusSeconds(3 * 24 * 60L * 60L), 1, "Administrative review", "Check member directory completeness, staff access, and calendar setup.", CalendarEntryType.PERSONAL_NOTE, Set.of(CalendarCategory.PERSONAL)),
                buildCalendarEntry(tenant, church, owner, now, baseInstant.plusSeconds(5 * 24 * 60L * 60L), 2, "Community prayer evening", "A sample parish calendar entry starting from the onboarding date.", CalendarEntryType.CELEBRATION, Set.of(CalendarCategory.LITURGY, CalendarCategory.WORSHIP))
        );
        calendarEntryRepository.saveAll(entries);
    }

    private CalendarEntryEntity buildCalendarEntry(TenantEntity tenant,
                                                   ChurchEntity church,
                                                   UserEntity owner,
                                                   Instant now,
                                                   Instant startAt,
                                                   int durationHours,
                                                   String title,
                                                   String description,
                                                   CalendarEntryType type,
                                                   Set<CalendarCategory> categories) {
        CalendarEntryEntity entry = CalendarEntryEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .ownerUser(owner)
                .type(type)
                .title(title)
                .description(description)
                .calendarSystem(CalendarSystem.GREGORIAN)
                .startAtUtc(startAt)
                .endAtUtc(startAt.plusSeconds(durationHours * 60L * 60L))
                .timezone(DEFAULT_TIMEZONE)
                .allDay(false)
                .visibility(CalendarVisibility.PUBLIC)
                .status(CalendarEntryStatus.SCHEDULED)
                .statusChangedAt(now)
                .sourceEntityType(CalendarEntrySourceType.MANUAL)
                .categories(new HashSet<>(categories))
                .createdBy(owner.getUuid())
                .updatedBy(owner.getUuid())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return entry;
    }

    private void seedAppointmentsIfMissing(TenantEntity tenant,
                                           ChurchEntity church,
                                           UserEntity owner,
                                           List<Adult_MemberEntity> adults,
                                           List<Child_MemberEntity> children,
                                           List<PriestEntity> priests,
                                           List<StaffEntity> staff,
                                           Instant baseInstant) {
        if (appointmentRepository.countByTenantId(tenant.getId()) > 0 || adults.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        UserEntity priestUser = priests.isEmpty() ? owner : priests.get(0).getUser();
        UserEntity staffUser = staff.isEmpty() ? owner : staff.get(0).getUser();
        Adult_MemberEntity primaryAdult = adults.get(0);
        Child_MemberEntity primaryChild = children.isEmpty() ? null : children.get(0);

        AppointmentEntity counseling = buildAppointment(
                tenant,
                church,
                owner,
                now,
                baseInstant.plusSeconds(2 * 24 * 60L * 60L + 10 * 60L * 60L),
                "Pastoral counseling",
                "A sample one-on-one pastoral appointment.",
                AppointmentType.SPIRITUAL_COUNSELING,
                AppointmentStatus.CONFIRMED,
                primaryAdult,
                primaryChild,
                priestUser,
                staffUser
        );

        AppointmentEntity baptismPrep = buildAppointment(
                tenant,
                church,
                owner,
                now,
                baseInstant.plusSeconds(6 * 24 * 60L * 60L + 15 * 60L * 60L),
                "Baptism preparation",
                "A sample family appointment with staff follow-up and priest assignment.",
                AppointmentType.BAPTISM_PREP,
                AppointmentStatus.PENDING_CONFIRMATION,
                adults.size() > 1 ? adults.get(1) : primaryAdult,
                primaryChild,
                priestUser,
                staffUser
        );

        appointmentRepository.saveAll(List.of(counseling, baptismPrep));
    }

    private AppointmentEntity buildAppointment(TenantEntity tenant,
                                               ChurchEntity church,
                                               UserEntity owner,
                                               Instant now,
                                               Instant startAt,
                                               String title,
                                               String description,
                                               AppointmentType type,
                                               AppointmentStatus status,
                                               Adult_MemberEntity adult,
                                               Child_MemberEntity child,
                                               UserEntity priestUser,
                                               UserEntity staffUser) {
        CalendarEntryEntity calendarEntry = CalendarEntryEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .ownerUser(owner)
                .type(CalendarEntryType.APPOINTMENT)
                .title(title)
                .description(description)
                .calendarSystem(CalendarSystem.GREGORIAN)
                .startAtUtc(startAt)
                .endAtUtc(startAt.plusSeconds(60L * 60L))
                .timezone(DEFAULT_TIMEZONE)
                .allDay(false)
                .visibility(CalendarVisibility.STAFF)
                .status(CalendarEntryStatus.SCHEDULED)
                .statusChangedAt(now)
                .sourceEntityType(CalendarEntrySourceType.MANUAL)
                .categories(new HashSet<>(Set.of(CalendarCategory.APPOINTMENTS)))
                .createdBy(owner.getUuid())
                .updatedBy(owner.getUuid())
                .createdAt(now)
                .updatedAt(now)
                .build();
        calendarEntry = calendarEntryRepository.save(calendarEntry);

        AppointmentEntity appointment = AppointmentEntity.builder()
                .tenantId(tenant.getId())
                .church(church)
                .calendarEntry(calendarEntry)
                .title(title)
                .description(description)
                .type(type)
                .status(status)
                .source(AppointmentSource.MANUAL)
                .locationType(LocationType.ONSITE)
                .locationLabel("Church office")
                .startAtUtc(startAt)
                .endAtUtc(startAt.plusSeconds(60L * 60L))
                .timezone(DEFAULT_TIMEZONE)
                .notesForMember("This appointment is part of the seeded demo workspace.")
                .privateNotesExists(false)
                .contactPhone(adult != null ? adult.getPhone() : "+291700000000")
                .contactEmail(adult != null ? adult.getEmail() : owner.getEmail())
                .contactPreference(ContactPreference.EITHER)
                .firstVisit(false)
                .sacramentRelated(type == AppointmentType.BAPTISM_PREP)
                .requestedAt(now)
                .confirmedAt(status == AppointmentStatus.CONFIRMED ? now : null)
                .build();
        appointment.setCreatedBy(owner.getUuid());
        appointment.setUpdatedBy(owner.getUuid());
        appointment.setCreatedAt(now);
        appointment.setUpdatedAt(now);

        appointment.getParticipants().add(AppointmentParticipantEntity.builder()
                .appointment(appointment)
                .memberId(adult != null ? adult.getId() : null)
                .fullName(adult != null ? adult.getFirstName() + " " + adult.getFatherName() : "Demo Member")
                .familyMember(false)
                .role(ParticipantRole.MEMBER)
                .build());

        if (child != null) {
            appointment.getParticipants().add(AppointmentParticipantEntity.builder()
                    .appointment(appointment)
                    .memberId(child.getId())
                    .fullName(child.getFirstName() + " " + child.getFatherName())
                    .familyMember(true)
                    .role(ParticipantRole.FAMILY)
                    .build());
        }

        if (priestUser != null) {
            appointment.getAssignments().add(AppointmentAssignmentEntity.builder()
                    .appointment(appointment)
                    .userId(priestUser.getUuid())
                    .role(AssignedRole.PRIEST)
                    .build());
        }
        if (staffUser != null) {
            appointment.getAssignments().add(AppointmentAssignmentEntity.builder()
                    .appointment(appointment)
                    .userId(staffUser.getUuid())
                    .role(AssignedRole.STAFF)
                    .build());
        }

        appointment.getStatusHistory().add(AppointmentStatusHistoryEntity.builder()
                .appointment(appointment)
                .fromStatus(null)
                .toStatus(status)
                .reason("Demo workspace seed")
                .changedByUserId(owner.getUuid())
                .changedAt(now)
                .build());

        return appointment;
    }

    private UserEntity createDemoUser(TenantEntity tenant,
                                      UserEntity owner,
                                      String email,
                                      String fullName,
                                      UserType userType,
                                      RoleType roleType,
                                      String phoneNumber,
                                      Instant now) {
        Role role = roleRepository.findByRoleName(roleType.name()).orElse(null);
        Set<Role> roles = role == null ? Collections.emptySet() : Set.of(role);
        UserEntity user = UserEntity.builder()
                .fullName(fullName)
                .email(email.toLowerCase(Locale.ROOT))
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .roles(new HashSet<>(roles))
                .userType(userType)
                .status(UserStatus.ACTIVE)
                .emailVerifiedAt(now)
                .affiliatedTenant(tenant)
                .createdBy(owner.getUuid())
                .updatedBy(owner.getUuid())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return userRepository.save(user);
    }
}
