package com.ekko.payment_service.infrastructure.messaging.rabbit;

import com.ekko.payment_service.domain.model.PaymentCancelledEvent;
import com.ekko.payment_service.domain.model.PaymentCompletedEvent;
import com.ekko.payment_service.domain.model.PaymentFailedEvent;
import com.ekko.payment_service.domain.model.PaymentInitiatedEvent;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQPaymentEventPublisher implements PaymentEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_INITIATED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishPaymentCancelled(PaymentCancelledEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_CANCELLED_ROUTING_KEY,
                event
        );
    }
}
