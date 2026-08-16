package com.ekko.seller_service.messaging;

import com.ekko.seller_service.config.RabbitMQConfig;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.messaging.dto.SellerCreatedEvent;
import com.ekko.seller_service.messaging.dto.SellerDocumentReviewEvent;
import com.ekko.seller_service.messaging.dto.SellerStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMQSellerEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMQSellerEventPublisher publisher =
            new RabbitMQSellerEventPublisher(rabbitTemplate);

    @Test
    void routesSellerCreatedEventToConfiguredExchangeAndKey() {
        SellerCreatedEvent event = new SellerCreatedEvent(
                UUID.randomUUID(),
                "kc-test-0001",
                "Mi tienda",
                "seller@ekko.test",
                SellerStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        publisher.publishSellerCreated(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_CREATED_ROUTING_KEY,
                event);
    }

    @Test
    void routesSellerStatusChangedEventToConfiguredExchangeAndKey() {
        SellerStatusChangedEvent event = new SellerStatusChangedEvent(
                UUID.randomUUID(),
                "kc-test-0001",
                SellerStatus.PENDING_REVIEW,
                SellerStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        publisher.publishSellerStatusChanged(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_STATUS_CHANGED_ROUTING_KEY,
                event);
    }

    @Test
    void routesSellerDocumentReviewEventToConfiguredExchangeAndKey() {
        SellerDocumentReviewEvent event = new SellerDocumentReviewEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.ID_CARD,
                DocumentStatus.APPROVED,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                "Verificado");

        publisher.publishSellerDocumentReview(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_DOCUMENT_REVIEW_ROUTING_KEY,
                event);
    }
}