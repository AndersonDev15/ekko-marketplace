package com.ekko.order_service.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(
        basePackages = "com.ekko.order_service",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                com.ekko.order_service.OrderServiceApplication.class,
                                com.ekko.order_service.ProductServiceFeignTestController.class
                        }
                )
        }
)
@EntityScan(basePackages = {
        "com.ekko.order_service.domain.model",
        "com.ekko.order_service.infrastructure.persistence.entity"
})
@EnableJpaRepositories(
        basePackages = "com.ekko.order_service.infrastructure.persistence.repository"
)
@EnableFeignClients(
        basePackages = "com.ekko.order_service.infrastructure.persistence.adapter.out.product"
)
@EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
        "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration",
        "org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration",
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@Import({
        com.ekko.order_service.infrastructure.config.SecurityConfig.class,
        com.ekko.order_service.infrastructure.config.RabbitMQConfig.class
})
public class TestContainersConfig {
}