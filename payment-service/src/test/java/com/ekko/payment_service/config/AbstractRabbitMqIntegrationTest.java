package com.ekko.payment_service.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

public abstract class AbstractRabbitMqIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final RabbitMQContainer RABBITMQ;

    static {
        RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
                .withUser("ekko", "ekko123");
        RABBITMQ.start();
    }

    @DynamicPropertySource
    static void rabbitMqProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }
}
