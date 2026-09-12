package com.ekko.seller_service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:16.14"))
                .withDatabaseName("seller_db")
                .withUsername("ekko")
                .withPassword("ekko123");
        POSTGRES.start();
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
    }

    public static PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES;
    }

    @Bean
    public PostgreSQLContainer<?> postgresContainerBean() {
        return POSTGRES;
    }
}