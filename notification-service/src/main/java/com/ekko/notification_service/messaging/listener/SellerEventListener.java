package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.SellerCreatedEvent;
import com.ekko.notification_service.messaging.event.SellerDocumentReviewEvent;
import com.ekko.notification_service.messaging.event.SellerStatusChangedEvent;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SellerEventListener {

    private static final List<NotificationType> SELLER_CHANNELS =
            List.of(NotificationType.EMAIL, NotificationType.IN_APP);

    private final NotificationOrchestratorService orchestrator;

    @RabbitListener(queues = RabbitMQConfig.SELLER_CREATED_QUEUE)
    public void onSellerCreated(SellerCreatedEvent event) {
        orchestrator.process(new NotificationEvent(
                UUID.fromString(event.keycloakId()),
                event.email(),
                "SELLER_CREATED",
                SELLER_CHANNELS,
                Map.of(
                        "sellerId", event.sellerId(),
                        "storeName", event.storeName(),
                        "email", event.email()
                )
        ));
    }

    @RabbitListener(queues = RabbitMQConfig.SELLER_STATUS_CHANGED_QUEUE)
    public void onSellerStatusChanged(SellerStatusChangedEvent event) {
        orchestrator.process(new NotificationEvent(
                UUID.fromString(event.keycloakId()),
                event.email(),
                "SELLER_STATUS_CHANGED",
                SELLER_CHANNELS,
                Map.of(
                        "sellerId", event.sellerId(),
                        "email", event.email(),
                        "previousStatus", event.previousStatus(),
                        "newStatus", event.newStatus()
                )
        ));
    }

    @RabbitListener(queues = RabbitMQConfig.SELLER_DOCUMENT_REVIEW_QUEUE)
    public void onSellerDocumentReview(SellerDocumentReviewEvent event) {
        orchestrator.process(new NotificationEvent(
                UUID.fromString(event.keycloakId()),
                event.email(),
                "SELLER_DOCUMENT_REVIEW",
                SELLER_CHANNELS,
                Map.of(
                        "sellerId", event.sellerId(),
                        "email", event.email(),
                        "documentType", event.documentType(),
                        "reviewStatus", event.reviewStatus(),
                        "notes", event.notes() == null ? "" : event.notes()
                )
        ));
    }
}