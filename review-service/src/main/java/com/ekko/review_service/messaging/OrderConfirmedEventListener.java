package com.ekko.review_service.messaging;

import com.ekko.review_service.config.RabbitMQConfig;
import com.ekko.review_service.messaging.dto.OrderConfirmedEvent;
import com.ekko.review_service.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConfirmedEventListener {

    private final EligibilityService eligibilityService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        eligibilityService.registerEligibility(event);
    }
}