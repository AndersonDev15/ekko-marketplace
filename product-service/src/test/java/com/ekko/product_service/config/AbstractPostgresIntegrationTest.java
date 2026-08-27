package com.ekko.product_service.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
public abstract class AbstractPostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES;

    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("product_db")
                .withUsername("ekko")
                .withPassword("ekko123");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabaseAndSeedSeller() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE product_attributes,
                           product_images,
                           product_variants,
                           products,
                           brands,
                           categories,
                           seller_status_view
                RESTART IDENTITY CASCADE
                """);
        // Seed seller_status_view for SellerStatusValidator
        jdbcTemplate.update("""
                INSERT INTO seller_status_view (seller_keycloak_id, status, updated_at)
                VALUES (CAST('00000000-0000-0000-0000-000000000001' AS uuid),
                        'ACTIVE',
                        now())
                ON CONFLICT (seller_keycloak_id) DO UPDATE SET status = 'ACTIVE', updated_at = now()
                """);
    }
}