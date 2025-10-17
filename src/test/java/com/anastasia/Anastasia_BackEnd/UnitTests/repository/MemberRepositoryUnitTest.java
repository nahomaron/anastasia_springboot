package com.anastasia.Anastasia_BackEnd.UnitTests.repository;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.registration.MemberRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"test", "test-server"})
@DataJpaTest(
        excludeAutoConfiguration = {
                com.anastasia.Anastasia_BackEnd.config.ApplicationConfig.class
        }
)
@Import(TestAuditorAwareConfig.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class MemberRepositoryUnitTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private MemberEntity member;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        entityManager.persist(tenant);

        ChurchEntity church = TestDataUtil.createTestChurchEntity(tenant);
        entityManager.persist(church);

        tenant.setChurch(church);
        entityManager.persist(tenant);

        member = TestDataUtil.createTestMember(church);
        entityManager.persist(member);
        entityManager.flush();
    }

    @Test
    void existsByMembershipNumber_shouldReturnTrue() {
        boolean exists = memberRepository.existsByMembershipNumber(member.getMembershipNumber());
        assertThat(exists).isTrue();
    }

    @Test
    void existsByMembershipNumber_shouldReturnFalseForUnknown() {
        boolean exists = memberRepository.existsByMembershipNumber("NOT-THERE");
        assertThat(exists).isFalse();
    }
}
