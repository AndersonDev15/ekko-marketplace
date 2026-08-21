package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.AbstractRabbitMqIntegrationTest;
import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.DocumentStatus;
import com.ekko.notification_service.messaging.event.DocumentType;
import com.ekko.notification_service.messaging.event.SellerCreatedEvent;
import com.ekko.notification_service.messaging.event.SellerDocumentReviewEvent;
import com.ekko.notification_service.messaging.event.SellerStatusChangedEvent;
import com.ekko.notification_service.messaging.event.SellerStatus;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class SellerEventListenerIntegrationTest extends AbstractRabbitMqIntegrationTest {

    private static final String SELLER_SERVICE_TYPE_ID =
            "com.ekko.seller_service.messaging.dto.publish.SellerCreatedEvent";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private NotificationOrchestratorService orchestrator;

    @TestConfiguration
    static class ProducerExchangesConfig {
        // notification-service does not declare seller.exchange nor product.exchange (consumer only,
        // bound by name in RabbitMQConfig). The test declares them to simulate the producer services,
        // so RabbitAdmin can establish every binding before messages are published.
        @Bean
        public DirectExchange sellerExchange() {
            return new DirectExchange(RabbitMQConfig.SELLER_EXCHANGE, true, false);
        }

        @Bean
        public DirectExchange productExchange() {
            return new DirectExchange(RabbitMQConfig.PRODUCT_EXCHANGE, true, false);
        }
    }

    @Test
    void receivesSellerCreatedEventAndBuildsNotificationEvent() {
        UUID sellerId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        SellerCreatedEvent event = new SellerCreatedEvent(
                sellerId,
                keycloakId.toString(),
                "Mi tienda",
                "seller@ekko.test",
                SellerStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_CREATED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(keycloakId, notification.recipientId());
        assertEquals("seller@ekko.test", notification.recipientEmail());
        assertEquals("SELLER_CREATED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "sellerId", sellerId,
                "storeName", "Mi tienda",
                "email", "seller@ekko.test"), notification.variables());
    }

    @Test
    void receivesSellerStatusChangedEventAndBuildsNotificationEvent() {
        UUID sellerId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        SellerStatusChangedEvent event = new SellerStatusChangedEvent(
                sellerId,
                keycloakId.toString(),
                "seller@ekko.test",
                SellerStatus.PENDING_REVIEW,
                SellerStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_STATUS_CHANGED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(keycloakId, notification.recipientId());
        assertEquals("seller@ekko.test", notification.recipientEmail());
        assertEquals("SELLER_STATUS_CHANGED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "sellerId", sellerId,
                "email", "seller@ekko.test",
                "previousStatus", SellerStatus.PENDING_REVIEW,
                "newStatus", SellerStatus.ACTIVE), notification.variables());
    }

    @Test
    void receivesSellerDocumentReviewEventAndBuildsNotificationEvent() {
        UUID sellerId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        SellerDocumentReviewEvent event = new SellerDocumentReviewEvent(
                sellerId,
                "seller@ekko.test",
                keycloakId.toString(),
                UUID.randomUUID(),
                DocumentType.BUSINESS_LICENSE,
                DocumentStatus.REJECTED,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                "Licencia inválida");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_DOCUMENT_REVIEW_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(keycloakId, notification.recipientId());
        assertEquals("seller@ekko.test", notification.recipientEmail());
        assertEquals("SELLER_DOCUMENT_REVIEW", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "sellerId", sellerId,
                "email", "seller@ekko.test",
                "documentType", DocumentType.BUSINESS_LICENSE,
                "reviewStatus", DocumentStatus.REJECTED,
                "notes", "Licencia inválida"), notification.variables());
    }

    @Test
    void deserializesMessageProducedWithSellerServiceTypeId() {
        // A message sent by seller-service carries __TypeId__ with the seller-service class FQN,
        // which is not present on notification-service's classpath. The listener still deserializes
        // it into its own SellerCreatedEvent record because @RabbitListener infers the argument type.
        UUID sellerId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        String json = """
                {"sellerId":"%s","keycloakId":"%s","storeName":"Mi tienda",
                 "email":"seller@ekko.test","status":"PENDING_REVIEW",
                 "createdAt":"2026-01-01T10:00:00"}""".formatted(sellerId, keycloakId);

        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setHeader("__TypeId__", SELLER_SERVICE_TYPE_ID)
                .build();

        rabbitTemplate.send(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_CREATED_ROUTING_KEY,
                message);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(keycloakId, notification.recipientId());
        assertEquals("seller@ekko.test", notification.recipientEmail());
        assertEquals("SELLER_CREATED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(sellerId, notification.variables().get("sellerId"));
    }

    private NotificationEvent awaitProcessedEvent() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(orchestrator).process(captor.capture()));
        return captor.getValue();
    }
}