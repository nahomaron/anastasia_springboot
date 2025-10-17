package com.anastasia.Anastasia_BackEnd.UnitTests.repository;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.GroupRepository;
import com.anastasia.Anastasia_BackEnd.testsupport.TestAuditorAwareConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"test", "test-server"})
@DataJpaTest(
        excludeAutoConfiguration = {
                com.anastasia.Anastasia_BackEnd.config.ApplicationConfig.class
        }
)
@Import(TestAuditorAwareConfig.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class GroupRepositoryUnitTest {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EntityManager entityManager;

    private GroupEntity group;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        entityManager.persist(tenant);

        ChurchEntity church = TestDataUtil.createTestChurchEntity(tenant);
        entityManager.persist(church);

        tenant.setChurch(church);
        entityManager.persist(tenant);

        group = TestDataUtil.createTestGroupEntity(church, tenant.getId() != null ? tenant.getId() : UUID.randomUUID());
        entityManager.persist(group);
        entityManager.flush();
    }

    @Test
    void existsByGroupName_shouldReturnTrue() {
        assertThat(groupRepository.existsByGroupName(group.getGroupName())).isTrue();
    }

    @Test
    void existsByGroupName_shouldReturnFalseForUnknownName() {
        assertThat(groupRepository.existsByGroupName("Missing Group")).isFalse();
    }
}
