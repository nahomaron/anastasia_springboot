package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
//import com.anastasia.Anastasia_BackEnd.testsupport.TestAuditorAwareConfig;
import com.anastasia.Anastasia_BackEnd.testsupport.TestAuditorAwareConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
        excludeAutoConfiguration = {
                com.anastasia.Anastasia_BackEnd.config.ApplicationConfig.class
        }
)
@ActiveProfiles("test")
@Import(TestAuditorAwareConfig.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class UserRepositoryUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity user;
    private GroupEntity group;
    private ChurchEntity church;

    @BeforeEach
    void setup() {
        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        entityManager.persist(tenant);

        church = TestDataUtil.createTestChurchEntity(tenant);
        entityManager.persist(church);

        tenant.setChurch(church);
        entityManager.persist(tenant);

        MemberEntity member = TestDataUtil.createTestMember(church);
        entityManager.persist(member);

        group = TestDataUtil.createTestGroupEntity(church, tenant.getId());
        entityManager.persist(group);

        user = TestDataUtil.createTestUserEntityA();
        user.setMembership(member);
        user.setGroups(Set.of(group));
        entityManager.persist(user);

        group.setUsers(Set.of(user));

        entityManager.flush();
    }

    @Test
    void testFindByEmail() {
        Optional<UserEntity> found = userRepository.findByEmail("gebray@gmail.com");
        assertThat(found).isPresent();
    }

    @Test
    void testFindByGoogleId() {
        user.setGoogleId("google123");
        entityManager.flush();

        Optional<UserEntity> found = userRepository.findByGoogleId("google123");
        assertThat(found).isPresent();
    }


    @Test
    void testExistsByEmail() {
        assertThat(userRepository.existsByEmail("gebray@gmail.com")).isTrue();
    }

    @Test
    void testFindUsersByGroupId() {
        Page<SimpleUserDTO> page = userRepository.findUsersByGroupId(group.getGroupId(), PageRequest.of(0, 10));
        assertThat(page).hasSize(1);
        assertThat(page.getContent().get(0).email()).isEqualTo("gebray@gmail.com");
    }

    @Test
    void testFindAllByUuidIn() {
        List<UserEntity> found = userRepository.findAllByUuidIn(Set.of(user.getUuid()));
        assertThat(found).hasSize(1);
    }

    @Test
    void testFindAllByChurchId() {
        List<UserEntity> found = userRepository.findAllByChurchId(church.getChurchId());
        assertThat(found).hasSize(1);
    }

    @Test
    void testFindAllUsersByChurchIdOptimized() {
        List<UserEntity> found = userRepository.findAllUsersByChurchIdOptimized(church.getChurchId());
        assertThat(found).hasSize(1);
    }

    @Test
    void testFindUserUUIDsByChurchId() {
        List<UUID> uuids = userRepository.findUserUUIDsByChurchId(church.getChurchId());
        assertThat(uuids).contains(user.getUuid());
    }

    @Test
    void testFindSimpleUsersByChurchId() {
        List<SimpleUserDTO> dtos = userRepository.findSimpleUsersByChurchId(church.getChurchId());
        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).email()).isEqualTo("gebray@gmail.com");
    }

}

