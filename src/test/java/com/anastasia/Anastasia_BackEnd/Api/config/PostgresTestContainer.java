package com.anastasia.Anastasia_BackEnd.Api.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresTestContainer {

    private static final String POSTGRES_MEMBERS_COLUMNS_SQL = """
            ALTER TABLE members ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE members ADD COLUMN IF NOT EXISTS inactive_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE members ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE members ADD COLUMN IF NOT EXISTS status_reason VARCHAR(512);
            ALTER TABLE members ADD COLUMN IF NOT EXISTS consent_version VARCHAR(64);
            ALTER TABLE members ADD COLUMN IF NOT EXISTS consent_accepted_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE members ADD COLUMN IF NOT EXISTS external_id VARCHAR(128);
            ALTER TABLE members ADD COLUMN IF NOT EXISTS source_system VARCHAR(64);
            ALTER TABLE members ADD COLUMN IF NOT EXISTS preferred_name VARCHAR(120);
            ALTER TABLE members ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
            """;

    private static final String POSTGRES_CHILDREN_COLUMNS_SQL = """
            ALTER TABLE children ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE children ADD COLUMN IF NOT EXISTS inactive_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE children ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE children ADD COLUMN IF NOT EXISTS status_reason VARCHAR(512);
            ALTER TABLE children ADD COLUMN IF NOT EXISTS consent_version VARCHAR(64);
            ALTER TABLE children ADD COLUMN IF NOT EXISTS consent_accepted_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE children ADD COLUMN IF NOT EXISTS external_id VARCHAR(128);
            ALTER TABLE children ADD COLUMN IF NOT EXISTS source_system VARCHAR(64);
            ALTER TABLE children ADD COLUMN IF NOT EXISTS preferred_name VARCHAR(120);
            ALTER TABLE children ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
            """;

    private static final String POSTGRES_TENANT_DEMO_TEMPLATE_SQL = """
            ALTER TABLE tenants ADD COLUMN IF NOT EXISTS is_demo_template BOOLEAN NOT NULL DEFAULT FALSE;
            CREATE INDEX IF NOT EXISTS idx_tenants_demo_template ON tenants(is_demo_template) WHERE is_demo_template = TRUE;
            """;

    private static final String POSTGRES_TENANT_WORKSPACE_LIFECYCLE_SQL = """
            ALTER TABLE tenants ADD COLUMN IF NOT EXISTS workspace_initialization_mode VARCHAR(24);
            ALTER TABLE tenants ADD COLUMN IF NOT EXISTS is_demo_workspace BOOLEAN NOT NULL DEFAULT FALSE;
            ALTER TABLE tenants ADD COLUMN IF NOT EXISTS scheduled_purge_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE tenants ADD COLUMN IF NOT EXISTS purged_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE tenants ADD COLUMN IF NOT EXISTS archive_scheduled_at TIMESTAMP WITH TIME ZONE;
            ALTER TABLE tenants ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP WITH TIME ZONE;
            CREATE INDEX IF NOT EXISTS idx_tenants_scheduled_purge_at ON tenants(scheduled_purge_at);
            CREATE INDEX IF NOT EXISTS idx_tenants_archive_scheduled_at ON tenants(archive_scheduled_at);
            """;

    private static final String H2_TENANT_DEMO_TEMPLATE_SQL = "CALL ensure_tenant_demo_template_columns();";
    private static final String H2_TENANT_WORKSPACE_LIFECYCLE_SQL = "CALL ensure_tenant_workspace_columns();";

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(false);

    private static final boolean DOCKER_AVAILABLE;

    static {
        boolean dockerAvailable;
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable throwable) {
            dockerAvailable = false;
        }
        DOCKER_AVAILABLE = dockerAvailable;

        if (DOCKER_AVAILABLE) {
            POSTGRES_CONTAINER.start();
        }
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.flyway.locations", () -> "classpath:db/migration");
            registry.add("spring.flyway.placeholders.members_columns_sql", () -> POSTGRES_MEMBERS_COLUMNS_SQL);
            registry.add("spring.flyway.placeholders.children_columns_sql", () -> POSTGRES_CHILDREN_COLUMNS_SQL);
            registry.add("spring.flyway.placeholders.tenant_demo_template_sql", () -> POSTGRES_TENANT_DEMO_TEMPLATE_SQL);
            registry.add("spring.flyway.placeholders.tenant_workspace_lifecycle_sql", () -> POSTGRES_TENANT_WORKSPACE_LIFECYCLE_SQL);
            registry.add("spring.flyway.placeholders.normalize_priest_languages_sql", () -> "");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "password");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.flyway.locations", () -> "classpath:db/migration-h2,classpath:db/migration");
            registry.add("spring.flyway.placeholders.members_columns_sql", () -> "");
            registry.add("spring.flyway.placeholders.children_columns_sql", () -> "");
            registry.add("spring.flyway.placeholders.tenant_demo_template_sql", () -> H2_TENANT_DEMO_TEMPLATE_SQL);
            registry.add("spring.flyway.placeholders.tenant_workspace_lifecycle_sql", () -> H2_TENANT_WORKSPACE_LIFECYCLE_SQL);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        }
    }
}
