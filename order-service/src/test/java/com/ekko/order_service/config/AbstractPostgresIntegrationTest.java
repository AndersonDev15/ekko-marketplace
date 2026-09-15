package com.ekko.order_service.config;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {com.ekko.order_service.OrderServiceApplication.class, TestContainersConfig.class})
public abstract class AbstractPostgresIntegrationTest {
}