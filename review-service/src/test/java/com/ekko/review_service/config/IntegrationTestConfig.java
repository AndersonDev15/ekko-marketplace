package com.ekko.review_service.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(
        basePackages = "com.ekko.review_service",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = com.ekko.review_service.ReviewServiceApplication.class
                )
        }
)
@EntityScan(basePackages = "com.ekko.review_service.entity")
@EnableJpaRepositories(basePackages = "com.ekko.review_service.repository")
@EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
        "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration",
        "org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration"
})
@Import({
        com.ekko.review_service.config.SecurityConfig.class,
        com.ekko.review_service.config.RabbitMQConfig.class
})
public class IntegrationTestConfig {
}