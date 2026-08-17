package com.ekko.payment_service.infrastructure.messaging.rabbit;

import com.ekko.payment_service.domain.event.PaymentCancelledEvent;
import com.ekko.payment_service.domain.event.PaymentCompletedEvent;
import com.ekko.payment_service.domain.event.PaymentFailedEvent;
import com.ekko.payment_service.domain.event.PaymentInitiatedEvent;
import com.ekko.payment_service.domain.event.PaymentRefundFailedEvent;
import com.ekko.payment_service.domain.event.PaymentRefundedEvent;
import com.ekko.payment_service.domain.event.TransferFailedEvent;
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

    @Override
    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_REFUNDED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishPaymentRefundFailed(PaymentRefundFailedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_REFUND_FAILED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishTransferFailed(TransferFailedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.TRANSFER_FAILED_ROUTING_KEY,
                event
        );
    }
}
