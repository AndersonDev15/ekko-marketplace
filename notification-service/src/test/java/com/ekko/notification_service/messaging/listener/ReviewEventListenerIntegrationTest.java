package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.AbstractRabbitMqIntegrationTest;
import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.ReviewCreatedEvent;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import com.ekko.notification_service.service.UserLookupService;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewEventListenerIntegrationTest extends AbstractRabbitMqIntegrationTest {

    private static final String SELLER_EMAIL = "seller@ekko.test";
    private static final String REVIEW_SERVICE_TYPE_ID =
            "com.ekko.review_service.messaging.dto.publish.ReviewCreatedEvent";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private NotificationOrchestratorService orchestrator;

    @MockitoBean
    private UserLookupService userLookupService;

    @TestConfiguration
    static class ProducerExchangesConfig {
        @Bean
        public DirectExchange sellerExchange() {
            return new DirectExchange(RabbitMQConfig.SELLER_EXCHANGE, true, false);
        }

        @Bean
        public DirectExchange productExchange() {
            return new DirectExchange(RabbitMQConfig.PRODUCT_EXCHANGE, true, false);
        }

        @Bean
        public DirectExchange orderExchange() {
            return new DirectExchange(RabbitMQConfig.ORDER_EXCHANGE, true, false);
        }

        @Bean
        public DirectExchange paymentExchange() {
            return new DirectExchange(RabbitMQConfig.PAYMENT_EXCHANGE, true, false);
        }

        @Bean
        public DirectExchange reviewExchange() {
            return new DirectExchange(RabbitMQConfig.REVIEW_EXCHANGE, true, false);
        }
    }

    @Test
    void receivesReviewCreatedEventResolvesSellerEmailAndBuildsNotificationEvent() {
        UUID sellerKeycloakId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ReviewCreatedEvent event = new ReviewCreatedEvent(
                UUID.randomUUID(),
                productId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                sellerKeycloakId,
                "customer-123",
                5,
                "Excelente producto",
                "Muy buena calidad",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REVIEW_EXCHANGE,
                RabbitMQConfig.REVIEW_CREATED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("REVIEW_CREATED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "productId", productId,
                "rating", 5,
                "title", "Excelente producto",
                "comment", "Muy buena calidad"), notification.variables());

        verify(userLookupService).resolveEmail(sellerKeycloakId);
    }

    @Test
    void receivesReviewCreatedEventWithNullTitleAndCommentFallsBackToEmptyStrings() {
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ReviewCreatedEvent event = new ReviewCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                sellerKeycloakId,
                "customer-123",
                3,
                null,
                null,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REVIEW_EXCHANGE,
                RabbitMQConfig.REVIEW_CREATED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals("", notification.variables().get("title"));
        assertEquals("", notification.variables().get("comment"));

        verify(userLookupService).resolveEmail(sellerKeycloakId);
    }

    @Test
    void reviewCreatedUsesSellerKeycloakIdAsRecipientIdNotCustomerId() {
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ReviewCreatedEvent event = new ReviewCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                sellerKeycloakId,
                "customer-123",
                4,
                "Buen producto",
                "Lo recomiendo",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REVIEW_EXCHANGE,
                RabbitMQConfig.REVIEW_CREATED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertNotEquals("customer-123", notification.recipientId());

        verify(userLookupService).resolveEmail(sellerKeycloakId);
    }

    @Test
    void deserializesMessageProducedWithReviewServiceTypeId() {
        UUID sellerKeycloakId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        String json = """
                {"reviewId":"%s","productId":"%s","orderId":"%s","orderItemId":"%s",
                 "sellerKeycloakId":"%s","customerId":"customer-123","rating":5,
                 "title":"Test","comment":"Test","createdAt":"2026-01-01T10:00:00"}""".formatted(
                UUID.randomUUID(), productId, UUID.randomUUID(), UUID.randomUUID(), sellerKeycloakId);

        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setHeader("__TypeId__", REVIEW_SERVICE_TYPE_ID)
                .build();

        rabbitTemplate.send(
                RabbitMQConfig.REVIEW_EXCHANGE,
                RabbitMQConfig.REVIEW_CREATED_ROUTING_KEY,
                message);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("REVIEW_CREATED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());

        verify(userLookupService).resolveEmail(sellerKeycloakId);
    }

    private NotificationEvent awaitProcessedEvent() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(orchestrator).process(captor.capture()));
        return captor.getValue();
    }
}