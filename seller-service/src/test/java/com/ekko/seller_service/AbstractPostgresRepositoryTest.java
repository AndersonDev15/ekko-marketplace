package com.ekko.seller_service;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ContextConfiguration(classes = {SellerServiceApplication.class, TestContainersConfig.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
public abstract class AbstractPostgresRepositoryTest {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> postgres = TestContainersConfig.postgresContainer();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE order_confirmations,
                           product_events,
                           review_confirmations,
                           seller_documents,
                           seller_bank_accounts,
                           seller_addresses,
                           seller_metrics,
                           sellers
                RESTART IDENTITY CASCADE
                """);
    }
}
