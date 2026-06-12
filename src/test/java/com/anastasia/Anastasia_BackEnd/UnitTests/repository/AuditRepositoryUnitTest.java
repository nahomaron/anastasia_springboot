package com.anastasia.Anastasia_BackEnd.UnitTests.repository;

import com.anastasia.Anastasia_BackEnd.TestSupport.TestAuditorAwareConfig;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditEventType;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditLog;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditRepository;
import com.anastasia.Anastasia_BackEnd.common.config.ApplicationConfig;
import jakarta.persistence.EntityManager;
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
class AuditRepositoryUnitTest {

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void save_shouldGeneratePrimaryKeyFromAuditLogsSequence() {
        AuditLog saved = auditRepository.save(buildAuditLog(AuditEventType.AUTH_LOGIN_SUCCEEDED.name()));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();
    }

    @Test
    void save_shouldPersistAndReloadAuditLogFields() {
        AuditLog saved = auditRepository.save(buildAuditLog(AuditEventType.AUTH_LOGIN_FAILED.name()));
        entityManager.flush();
        entityManager.clear();

        AuditLog reloaded = auditRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getAction()).isEqualTo(AuditEventType.AUTH_LOGIN_FAILED.name());
        assertThat(reloaded.getActorIdentifier()).isEqualTo("platform-admin@example.com");
        assertThat(reloaded.getTenantId()).isEqualTo(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(reloaded.getTargetType()).isEqualTo("AUTH");
        assertThat(reloaded.getTargetId()).isEqualTo("user-123");
        assertThat(reloaded.getResult()).isEqualTo("FAILURE");
        assertThat(reloaded.getReason()).isEqualTo("Invalid credentials");
        assertThat(reloaded.getContext()).isEqualTo("{\"ip\":\"127.0.0.1\"}");
        assertThat(reloaded.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(reloaded.getUserAgent()).isEqualTo("JUnit");
    }

    private AuditLog buildAuditLog(String action) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setActorIdentifier("platform-admin@example.com");
        auditLog.setTenantId(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
        auditLog.setTargetType("AUTH");
        auditLog.setTargetId("user-123");
        auditLog.setResult("FAILURE");
        auditLog.setReason("Invalid credentials");
        auditLog.setContext("{\"ip\":\"127.0.0.1\"}");
        auditLog.setIpAddress("127.0.0.1");
        auditLog.setUserAgent("JUnit");
        return auditLog;
    }
}
