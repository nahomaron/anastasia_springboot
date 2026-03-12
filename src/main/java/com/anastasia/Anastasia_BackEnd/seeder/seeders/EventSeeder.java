package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.Repetition;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class EventSeeder {

    private static final Logger log = LoggerFactory.getLogger(EventSeeder.class);
    private static final int EVENTS_PER_CHURCH = 4;

    private final EventRepository eventRepository;
    private final GroupRepository groupRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;

    public List<EventEntity> seedEvents(List<ChurchEntity> churches, List<GroupEntity> groups) {
        if (eventRepository.count() > 0) {
            log.debug("Skipping event seeding; events already exist.");
            return Collections.emptyList();
        }

        if (churches == null || churches.isEmpty()) {
            churches = churchRepository.findAll();
        }
        if (churches.isEmpty()) {
            log.warn("Skipping event seeding; no churches available.");
            return Collections.emptyList();
        }

        if (groups == null || groups.isEmpty()) {
            groups = groupRepository.findAll();
        }

        Map<Long, List<GroupEntity>> groupsByChurch = new HashMap<>();
        for (GroupEntity group : groups) {
            Long churchId = Optional.ofNullable(group.getChurch())
                    .map(ChurchEntity::getChurchId)
                    .orElse(null);
            if (churchId != null) {
                groupsByChurch.computeIfAbsent(churchId, key -> new ArrayList<>()).add(group);
            }
        }

        Faker faker = new Faker();
        List<EventEntity> eventsToPersist = new ArrayList<>();

        for (ChurchEntity church : churches) {
            UUID tenantId = Optional.ofNullable(church.getTenant())
                    .map(t -> t.getId())
                    .orElse(null);

            if (tenantId == null) {
                log.debug("Skipping events for church {} because tenant is missing.", church.getChurchName());
                continue;
            }

            List<UserEntity> churchUsers = new ArrayList<>(
                    userRepository.findAllUsersByChurchIdOptimized(church.getChurchId()));

            if (churchUsers.isEmpty()) {
                log.debug("Skipping events for church {} because there are no users yet.", church.getChurchName());
                continue;
            }

            List<GroupEntity> churchGroups = groupsByChurch.getOrDefault(church.getChurchId(), Collections.emptyList());

            int eventsForChurch = Math.min(EVENTS_PER_CHURCH, Math.max(1, churchUsers.size() / 8));

            for (int i = 0; i < eventsForChurch; i++) {
                LocalDate eventDate = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(5, 90));
                LocalTime start = randomTime();
                LocalTime end = start.plusHours(ThreadLocalRandom.current().nextInt(1, 3));

                Set<GroupEntity> invitedGroups = selectGroups(churchGroups);
                Set<UserEntity> invitedUsers = selectUsers(churchUsers, 15);

                EventEntity event = EventEntity.builder()
                        .tenantId(tenantId)
                        .church(church)
                        .title(faker.company().buzzword() + " Gathering")
                        .description(faker.lorem().paragraph())
                        .location(faker.address().fullAddress())
                        .startAt(LocalDateTime.of(eventDate, start))
                        .endAt(LocalDateTime.of(eventDate, end))
                        .image("https://picsum.photos/seed/" + faker.number().numberBetween(1000, 9999) + "/600/400")
                        .latitude(parseDoubleSafe(faker.address().latitude()))
                        .longitude(parseDoubleSafe(faker.address().longitude()))
                        .invitedGroups(invitedGroups)
                        .invitedUsers(invitedUsers)
                        .visibility(randomVisibility())
                        .repetition(randomRepetition())
                        .build();

                UUID auditUserId = invitedUsers.stream()
                        .findFirst()
                        .orElseGet(() -> churchUsers.get(0))
                        .getUuid();
                event.setCreatedBy(auditUserId);
                event.setLastModifiedBy(auditUserId);
                LocalDateTime now = LocalDateTime.now();
                event.setCreatedDate(now);
                event.setLastModifiedDate(now);

                Set<UserEntity> potentialManagers = invitedUsers.isEmpty()
                        ? selectUsers(churchUsers, 2)
                        : invitedUsers;

                Set<EventManagerEntity> managerEntities = buildManagers(event, potentialManagers, faker);
                event.setEventManagers(managerEntities);

                eventsToPersist.add(event);
            }
        }

        if (eventsToPersist.isEmpty()) {
            log.warn("No events created during seeding.");
            return Collections.emptyList();
        }

        List<EventEntity> savedEvents = eventRepository.saveAll(eventsToPersist);
        log.info("Seeded {} events across {} churches.", savedEvents.size(), churches.size());
        return savedEvents;
    }

    private LocalTime randomTime() {
        int hour = ThreadLocalRandom.current().nextInt(8, 18);
        int minute = ThreadLocalRandom.current().nextInt(0, 4) * 15;
        return LocalTime.of(hour, minute);
    }

    private Set<GroupEntity> selectGroups(List<GroupEntity> churchGroups) {
        if (churchGroups == null || churchGroups.isEmpty()) {
            return Collections.emptySet();
        }
        Collections.shuffle(churchGroups);
        int count = Math.min(churchGroups.size(), ThreadLocalRandom.current().nextInt(1, Math.min(3, churchGroups.size()) + 1));
        return new HashSet<>(churchGroups.subList(0, count));
    }

    private Set<UserEntity> selectUsers(List<UserEntity> source, int maxSize) {
        if (source == null || source.isEmpty() || maxSize <= 0) {
            return Collections.emptySet();
        }
        Collections.shuffle(source);
        int count = Math.min(source.size(), ThreadLocalRandom.current().nextInt(5, maxSize + 1));
        return new HashSet<>(source.subList(0, count));
    }

    private EventVisibilityType randomVisibility() {
        EventVisibilityType[] values = EventVisibilityType.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private Repetition randomRepetition() {
        Repetition[] values = Repetition.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private Set<EventManagerEntity> buildManagers(EventEntity event,
                                                  Set<UserEntity> candidates,
                                                  Faker faker) {
        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }
        List<UserEntity> candidateList = new ArrayList<>(candidates);
        Collections.shuffle(candidateList);
        int managerCount = Math.min(candidateList.size(), ThreadLocalRandom.current().nextInt(1, 3));

        Set<EventManagerEntity> managers = new HashSet<>();
        for (int i = 0; i < managerCount; i++) {
            UserEntity managerUser = candidateList.get(i);
            EventManagerEntity manager = EventManagerEntity.builder()
                    .event(event)
                    .user(managerUser)
                    .role(faker.options().option("ORGANIZER", "COORDINATOR", "HOST"))
                    .build();
            managers.add(manager);
        }
        return managers;
    }

    private Double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
