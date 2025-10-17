package com.anastasia.Anastasia_BackEnd.api.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresTestContainer {

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(false);

    static {
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
