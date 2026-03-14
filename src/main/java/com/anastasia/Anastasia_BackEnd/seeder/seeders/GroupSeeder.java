package com.anastasia.Anastasia_BackEnd.seeder.seeders;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.time.Instant;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class GroupSeeder {

    private static final Logger log = LoggerFactory.getLogger(GroupSeeder.class);

    private static final int GROUPS_PER_CHURCH = 3;
    private static final int MAX_GROUP_MEMBERS = 20;
    private static final int MAX_GROUP_MANAGERS = 2;

    private final GroupRepository groupRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;

    public List<GroupEntity> seedGroups(List<ChurchEntity> churches) {
        if (groupRepository.count() > 0) {
            log.debug("Skipping group seeding; groups already exist.");
            return Collections.emptyList();
        }

        if (churches == null || churches.isEmpty()) {
            churches = churchRepository.findAll();
        }
        if (churches.isEmpty()) {
            log.warn("Skipping group seeding; no churches available.");
            return Collections.emptyList();
        }

        Faker faker = new Faker();
        List<GroupEntity> groupsToPersist = new ArrayList<>();

        for (ChurchEntity church : churches) {
            UUID tenantId = Optional.ofNullable(church.getTenant())
                    .map(t -> t.getId())
                    .orElse(null);

            if (tenantId == null) {
                log.debug("Skipping church {} because tenant information is missing.", church.getChurchName());
                continue;
            }

            List<UserEntity> churchUsers = new ArrayList<>(
                    userRepository.findAllUsersByChurchIdOptimized(church.getChurchId()));

            if (churchUsers.isEmpty()) {
                log.debug("Skipping groups for church {} because no users are linked yet.", church.getChurchName());
                continue;
            }

            int groupsForChurch = Math.min(GROUPS_PER_CHURCH, Math.max(1, churchUsers.size() / 5));

            for (int i = 0; i < groupsForChurch; i++) {
                Collections.shuffle(churchUsers);

                Set<UserEntity> managers = pickSubset(churchUsers, MAX_GROUP_MANAGERS);
                Set<UserEntity> members = pickSubset(churchUsers, MAX_GROUP_MEMBERS);
                members.addAll(managers); // ensure managers belong to the group roster

                GroupEntity group = GroupEntity.builder()
                        .tenantId(tenantId)
                        .church(church)
                        .groupName(faker.company().buzzword() + " Ministry " + faker.number().numberBetween(1, 99))
                        .description(faker.lorem().sentence(12))
                        .avatar(faker.avatar().image())
                        .visibility(faker.options().option("PUBLIC", "PRIVATE"))
                        .build();

                UUID auditUserId = managers.stream()
                        .findFirst()
                        .or(() -> members.stream().findFirst())
                        .map(UserEntity::getUuid)
                        .orElse(null);

                if (auditUserId != null) {
                    group.setCreatedBy(auditUserId);
                    group.setUpdatedBy(auditUserId);
                }
                Instant now = Instant.now();
                group.setCreatedAt(now);
                group.setUpdatedAt(now);

                // Assign managers
                for (UserEntity manager : managers) {
                    group.getManagers().add(manager);
                    manager.addGroup(group);
                }

                // Assign members
                for (UserEntity member : members) {
                    member.addGroup(group);
                }

                groupsToPersist.add(group);
            }
        }

        if (groupsToPersist.isEmpty()) {
            log.warn("No groups created during seeding.");
            return Collections.emptyList();
        }

        List<GroupEntity> savedGroups = groupRepository.saveAll(groupsToPersist);
        log.info("Seeded {} groups across {} churches.", savedGroups.size(), churches.size());
        return savedGroups;
    }

    private Set<UserEntity> pickSubset(List<UserEntity> source, int maxSize) {
        if (source.isEmpty() || maxSize <= 0) {
            return Collections.emptySet();
        }

        int targetSize = ThreadLocalRandom.current().nextInt(1, Math.min(maxSize, source.size()) + 1);
        Set<UserEntity> selection = new HashSet<>(targetSize);
        Collections.shuffle(source);
        for (int i = 0; i < targetSize; i++) {
            selection.add(source.get(i));
        }
        return selection;
    }
}
