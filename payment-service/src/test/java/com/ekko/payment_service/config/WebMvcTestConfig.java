package com.ekko.payment_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ekko.payment_service.infrastructure.config.SecurityConfig;

@Configuration
@Import({SecurityConfig.class})
public class WebMvcTestConfig {
}