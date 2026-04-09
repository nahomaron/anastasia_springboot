package com.anastasia.Anastasia_BackEnd.UnitTests.repository;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.common.config.ApplicationConfig;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.TestSupport.TestAuditorAwareConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest(
        excludeAutoConfiguration = {
                ApplicationConfig.class
        }
)
@ContextConfiguration(classes = com.anastasia.Anastasia_BackEnd.UnitTests.config.RepositoryTestConfig.class)
@Import(TestAuditorAwareConfig.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class ChildRepositoryUnitTest {

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private EntityManager entityManager;

    private Child_MemberEntity child;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        entityManager.persist(tenant);
        entityManager.flush();

        ChurchEntity church = TestDataUtil.createTestChurchEntity(tenant);
        entityManager.persist(church);

        tenant.setChurch(church);
        entityManager.persist(tenant);
        entityManager.flush();

        child = TestDataUtil.createTestChild(church);
        child.setTenantId(tenant.getId());
        entityManager.persist(child);
        entityManager.flush();
    }

    @Test
    void existsByMembershipNumber_shouldReturnTrueForExistingChild() {
        boolean exists = childRepository.existsByMembershipNumber(child.getMembershipNumber());
        assertThat(exists).isTrue();
    }

    @Test
    void existsByMembershipNumber_shouldReturnFalseForUnknownNumber() {
        boolean exists = childRepository.existsByMembershipNumber("UNKNOWN");
        assertThat(exists).isFalse();
    }
}
