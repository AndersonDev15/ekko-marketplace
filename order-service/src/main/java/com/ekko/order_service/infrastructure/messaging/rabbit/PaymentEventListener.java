package com.ekko.order_service.infrastructure.messaging.rabbit;

import com.ekko.order_service.domain.model.PaymentData;
import com.ekko.order_service.domain.port.in.PaymentCallbackUseCase;
import com.ekko.order_service.infrastructure.config.RabbitMQConfig;
import com.ekko.order_service.infrastructure.messaging.dto.PaymentCompletedEventPayload;
import com.ekko.order_service.infrastructure.messaging.dto.PaymentFailedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentCallbackUseCase paymentCallbackUseCase;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void onPaymentCompleted(PaymentCompletedEventPayload payload) {
        paymentCallbackUseCase.onPaymentCompleted(
                payload.orderId(),
                new PaymentData(
                        payload.paymentId(),
                        payload.amount(),
                        payload.currency(),
                        payload.completedAt()));
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEventPayload payload) {
        paymentCallbackUseCase.onPaymentFailed(payload.orderId());
    }
}