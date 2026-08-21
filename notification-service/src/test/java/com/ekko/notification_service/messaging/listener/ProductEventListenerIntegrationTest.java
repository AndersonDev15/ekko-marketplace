package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.AbstractRabbitMqIntegrationTest;
import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.InventoryLowStockEvent;
import com.ekko.notification_service.messaging.event.ProductDeactivatedEvent;
import com.ekko.notification_service.messaging.event.ProductPublishedEvent;
import com.ekko.notification_service.messaging.event.ProductRejectedEvent;
import com.ekko.notification_service.messaging.event.ProductStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductEventListenerIntegrationTest extends AbstractRabbitMqIntegrationTest {

    private static final String SELLER_EMAIL = "seller@ekko.test";
    private static final String PRODUCT_SERVICE_TYPE_ID =
            "com.ekko.product_service.messaging.dto.publish.ProductPublishedEvent";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private NotificationOrchestratorService orchestrator;

    @MockitoBean
    private UserLookupService userLookupService;

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
    void receivesProductPublishedEventAndBuildsNotificationEvent() {
        UUID productId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ProductPublishedEvent event = new ProductPublishedEvent(
                productId,
                sellerKeycloakId,
                "Zapatillas deportivas",
                "Calzado",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_PUBLISHED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("PRODUCT_PUBLISHED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "productId", productId,
                "name", "Zapatillas deportivas",
                "category", "Calzado"), notification.variables());
    }

    @Test
    void receivesProductRejectedEventAndBuildsNotificationEvent() {
        UUID productId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ProductRejectedEvent event = new ProductRejectedEvent(
                productId,
                sellerKeycloakId,
                "Zapatillas deportivas",
                "Foto de mala calidad",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_REJECTED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("PRODUCT_REJECTED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "productId", productId,
                "name", "Zapatillas deportivas",
                "reason", "Foto de mala calidad"), notification.variables());
    }

    @Test
    void receivesProductDeactivatedEventAndBuildsNotificationEvent() {
        UUID productId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ProductDeactivatedEvent event = new ProductDeactivatedEvent(
                productId,
                sellerKeycloakId,
                ProductStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_DEACTIVATED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("PRODUCT_DEACTIVATED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "productId", productId,
                "previousStatus", ProductStatus.ACTIVE), notification.variables());
    }

    @Test
    void receivesInventoryLowStockEventAndBuildsNotificationEvent() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        InventoryLowStockEvent event = new InventoryLowStockEvent(
                productId,
                variantId,
                sellerKeycloakId,
                3,
                10,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.INVENTORY_LOW_STOCK_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("INVENTORY_LOW_STOCK", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "productId", productId,
                "variantId", variantId,
                "currentStock", 3L,
                "minimumStock", 10L), notification.variables());
    }

    @Test
    void deserializesMessageProducedWithProductServiceTypeId() {
        // A message sent by product-service carries __TypeId__ with the product-service class FQN,
        // which is not present on notification-service's classpath. The listener still deserializes
        // it into its own ProductPublishedEvent record because @RabbitListener infers the argument type.
        UUID productId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        String json = """
                {"productId":"%s","sellerKeycloakId":"%s","name":"Zapatillas deportivas",
                 "category":"Calzado","publishedAt":"2026-01-01T10:00:00"}""".formatted(productId, sellerKeycloakId);

        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setHeader("__TypeId__", PRODUCT_SERVICE_TYPE_ID)
                .build();

        rabbitTemplate.send(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_PUBLISHED_ROUTING_KEY,
                message);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("PRODUCT_PUBLISHED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(productId, notification.variables().get("productId"));
    }

    private NotificationEvent awaitProcessedEvent() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(orchestrator).process(captor.capture()));
        return captor.getValue();
    }
}