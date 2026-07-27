package com.adwitiya.feedbackportal.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for integration tests.
 *
 * <p>Runs against a real MySQL 8.4 container rather than an in-memory
 * substitute. That matters here specifically: the schema uses
 * {@code FULLTEXT} indexes, {@code CHECK} constraints and
 * {@code TIMESTAMPDIFF}, and every Flyway migration is exercised on the
 * engine that actually runs in production. H2 in MySQL-compatibility mode
 * would silently accept things MySQL rejects.</p>
 *
 * <p>The container is {@code static}, so one MySQL instance is reused across
 * every test class in the run.</p>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("feedback_portal_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-time-zone=+00:00")
            .withReuse(true);

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/demo");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
