package com.ekko.payment_service.infrastructure.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_INITIATED_ROUTING_KEY = "payment.initiated";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_CANCELLED_ROUTING_KEY = "payment.cancelled";

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }
}
