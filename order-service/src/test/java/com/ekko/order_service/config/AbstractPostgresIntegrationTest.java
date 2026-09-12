package com.ekko.order_service.config;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ContextConfiguration(classes = {com.ekko.order_service.OrderServiceApplication.class, TestContainersConfig.class})
public abstract class AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> postgres = TestContainersConfig.postgresContainer();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }
}