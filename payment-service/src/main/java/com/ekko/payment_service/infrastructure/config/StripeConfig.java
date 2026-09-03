package com.ekko.payment_service.infrastructure.config;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    @Bean
    public StripeClient stripeClient() {
        return new StripeClient(apiKey);
    }
}