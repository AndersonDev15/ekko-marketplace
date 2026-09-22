package com.ekko.order_service.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest
@ContextConfiguration(classes = TestContainersConfig.class)
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("order_db")
                    .withUsername("ekko")
                    .withPassword("ekko123");

    static final RabbitMQContainer rabbitmq =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        postgres.start();
        rabbitmq.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);

        registry.add(
                "spring.rabbitmq.listener.simple.auto-startup",
                () -> "false"
        );

        registry.add("eureka.client.enabled", () -> "false");

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add(
                "spring.flyway.locations",
                () -> "classpath:db/migration"
        );
    }
}