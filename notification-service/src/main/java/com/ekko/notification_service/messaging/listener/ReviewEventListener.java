package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.ReviewCreatedEvent;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import com.ekko.notification_service.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final NotificationOrchestratorService orchestratorService;
    private final UserLookupService userLookupService;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_CREATED_QUEUE)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        String sellerEmail = userLookupService.resolveEmail(event.sellerKeycloakId());

        NotificationEvent notificationEvent = new NotificationEvent(
                event.sellerKeycloakId(),
                sellerEmail,
                "REVIEW_CREATED",
                List.of(NotificationType.EMAIL, NotificationType.IN_APP),
                Map.of(
                        "productId", event.productId(),
                        "rating", event.rating(),
                        "title", event.title() != null ? event.title() : "",
                        "comment", event.comment() != null ? event.comment() : ""
                )
        );
        orchestratorService.process(notificationEvent);
    }
}