package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.AbstractRabbitMqIntegrationTest;
import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.OrderCancelledEvent;
import com.ekko.notification_service.messaging.event.OrderConfirmedEvent;
import com.ekko.notification_service.messaging.event.OrderCreatedEvent;
import com.ekko.notification_service.messaging.event.OrderStatus;
import com.ekko.notification_service.messaging.event.OrderStatusChangedEvent;
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

import java.math.BigDecimal;
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

class OrderEventListenerIntegrationTest extends AbstractRabbitMqIntegrationTest {

    private static final String CUSTOMER_EMAIL = "customer@ekko.test";
    private static final String SELLER_EMAIL = "seller@ekko.test";
    private static final String ORDER_SERVICE_TYPE_ID =
            "com.ekko.order_service.domain.event.OrderCreatedEvent";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private NotificationOrchestratorService orchestrator;

    @MockitoBean
    private UserLookupService userLookupService;

    @TestConfiguration
    static class ProducerExchangesConfig {
        // notification-service does not declare seller.exchange, product.exchange nor order.exchange
        // (consumer only, bound by name in RabbitMQConfig). The test declares them to simulate the
        // producer services, so RabbitAdmin can establish every binding before messages are published.
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
    }

    @Test
    void receivesOrderCreatedEventAndBuildsCustomerAndSellerNotifications() {
        UUID customerId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                new BigDecimal("200.00"),
                OrderStatus.PENDING,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                List.of(new OrderCreatedEvent.OrderItemPayload(
                        UUID.randomUUID(), UUID.randomUUID(), 2, sellerKeycloakId,
                        new BigDecimal("100.00"), new BigDecimal("200.00"))));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event);

        List<NotificationEvent> notifications = awaitProcessedEvents(2);

        NotificationEvent customer = notifications.get(0);
        assertEquals(customerId, customer.recipientId());
        assertEquals(CUSTOMER_EMAIL, customer.recipientEmail());
        assertEquals("ORDER_CREATED", customer.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), customer.channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "total", new BigDecimal("200.00"),
                "itemCount", 1), customer.variables());

        NotificationEvent seller = notifications.get(1);
        assertEquals(sellerKeycloakId, seller.recipientId());
        assertEquals(SELLER_EMAIL, seller.recipientEmail());
        assertEquals("ORDER_CREATED_SELLER", seller.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), seller.channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 1L), seller.variables());
    }

    @Test
    void receivesOrderConfirmedEventAndBuildsCustomerAndSellerNotifications() {
        UUID customerId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                List.of(new OrderConfirmedEvent.OrderItemConfirmed(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, sellerKeycloakId,
                        new BigDecimal("200.00"))),
                LocalDateTime.of(2026, 8, 20, 11, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CONFIRMED_ROUTING_KEY,
                event);

        List<NotificationEvent> notifications = awaitProcessedEvents(2);

        assertEquals("ORDER_CONFIRMED", notifications.get(0).eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notifications.get(0).channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 1), notifications.get(0).variables());
        assertEquals("ORDER_CONFIRMED_SELLER", notifications.get(1).eventType());
        assertEquals(sellerKeycloakId, notifications.get(1).recipientId());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 1L), notifications.get(1).variables());
    }

    @Test
    void receivesOrderCancelledEventAndBuildsCustomerAndSellerNotifications() {
        UUID customerId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                OrderStatus.CONFIRMED,
                true,
                List.of(new OrderCancelledEvent.OrderItemCancelled(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, sellerKeycloakId)),
                LocalDateTime.of(2026, 8, 20, 12, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY,
                event);

        List<NotificationEvent> notifications = awaitProcessedEvents(2);

        assertEquals("ORDER_CANCELLED", notifications.get(0).eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notifications.get(0).channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "refundRequired", true,
                "previousStatus", OrderStatus.CONFIRMED), notifications.get(0).variables());
        assertEquals("ORDER_CANCELLED_SELLER", notifications.get(1).eventType());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "refundRequired", true), notifications.get(1).variables());
    }

    @Test
    void receivesOrderStatusChangedEventAndBuildsCustomerAndSellerNotifications() {
        UUID customerId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                OrderStatus.CONFIRMED,
                OrderStatus.SHIPPED,
                List.of(sellerKeycloakId),
                LocalDateTime.of(2026, 8, 20, 13, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_CHANGED_ROUTING_KEY,
                event);

        List<NotificationEvent> notifications = awaitProcessedEvents(2);

        assertEquals("ORDER_STATUS_CHANGED", notifications.get(0).eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notifications.get(0).channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "previousStatus", OrderStatus.CONFIRMED,
                "newStatus", OrderStatus.SHIPPED), notifications.get(0).variables());
        assertEquals("ORDER_STATUS_CHANGED_SELLER", notifications.get(1).eventType());
        assertEquals(sellerKeycloakId, notifications.get(1).recipientId());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "previousStatus", OrderStatus.CONFIRMED,
                "newStatus", OrderStatus.SHIPPED), notifications.get(1).variables());
    }

    @Test
    void deserializesMessageProducedWithOrderServiceTypeId() {
        // A message sent by order-service carries __TypeId__ with the order-service class FQN,
        // which is not present on notification-service's classpath. The listener still deserializes
        // it into its own OrderCreatedEvent record because @RabbitListener infers the argument type.
        UUID customerId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        String json = """
                {"orderId":"%s","orderNumber":"EKK-20260820-AB12","customerId":"%s",
                 "customerEmail":"%s","total":200.00,"status":"PENDING",
                 "createdAt":"2026-08-20T10:00:00",
                 "items":[{"variantId":"%s","productId":"%s","quantity":2,
                            "sellerKeycloakId":"%s","priceSnapshot":100.00,"subtotal":200.00}]}""".formatted(
                UUID.randomUUID(), customerId, CUSTOMER_EMAIL, UUID.randomUUID(), UUID.randomUUID(), sellerKeycloakId);

        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setHeader("__TypeId__", ORDER_SERVICE_TYPE_ID)
                .build();

        rabbitTemplate.send(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                message);

        List<NotificationEvent> notifications = awaitProcessedEvents(2);

        assertEquals("ORDER_CREATED", notifications.get(0).eventType());
        assertEquals(customerId, notifications.get(0).recipientId());
        assertEquals(CUSTOMER_EMAIL, notifications.get(0).recipientEmail());
        assertEquals("ORDER_CREATED_SELLER", notifications.get(1).eventType());
        assertEquals(sellerKeycloakId, notifications.get(1).recipientId());
        assertEquals(SELLER_EMAIL, notifications.get(1).recipientEmail());
    }

    private List<NotificationEvent> awaitProcessedEvents(int count) {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(orchestrator, org.mockito.Mockito.times(count))
                        .process(captor.capture()));
        return captor.getAllValues();
    }
}