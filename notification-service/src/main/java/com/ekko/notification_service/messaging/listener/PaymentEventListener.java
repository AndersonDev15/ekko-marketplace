package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.PaymentFailedEvent;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final NotificationOrchestratorService orchestratorService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        List<NotificationType> channels = event.customerId() != null
                ? List.of(NotificationType.EMAIL, NotificationType.IN_APP)
                : List.of(NotificationType.EMAIL);

        NotificationEvent notificationEvent = new NotificationEvent(
                event.customerId(),
                event.customerEmail(),
                "PAYMENT_FAILED",
                channels,
                Map.of(
                        "orderId", event.orderId(),
                        "reason", event.reason(),
                        "failedAt", event.failedAt()
                )
        );
        orchestratorService.process(notificationEvent);
    }
}