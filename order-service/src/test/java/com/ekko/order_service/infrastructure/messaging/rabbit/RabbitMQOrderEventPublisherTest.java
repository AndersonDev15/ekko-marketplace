package com.ekko.order_service.infrastructure.messaging.rabbit;

import com.ekko.order_service.domain.event.OrderCancelledEvent;
import com.ekko.order_service.domain.event.OrderConfirmedEvent;
import com.ekko.order_service.domain.event.OrderCreatedEvent;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.event.OrderStatusChangedEvent;
import com.ekko.order_service.infrastructure.config.RabbitMQConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMQOrderEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMQOrderEventPublisher publisher =
            new RabbitMQOrderEventPublisher(rabbitTemplate);

    private static final UUID VARIANT_ID = UUID.randomUUID();
    private static final UUID OTHER_VARIANT_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SELLER_KEYCLOAK_ID = UUID.randomUUID();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void routesOrderCreatedEventToConfiguredExchangeAndKey() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "EKK-20250809-AB12",
                UUID.randomUUID(),
                "customer@example.com",
                new BigDecimal("199.00"),
                OrderStatus.CONFIRMED,
                LocalDateTime.of(2025, 8, 9, 12, 0),
                List.of(
                        new OrderCreatedEvent.OrderItemPayload(
                                VARIANT_ID,
                                PRODUCT_ID,
                                2,
                                SELLER_KEYCLOAK_ID,
                                new BigDecimal("100.00"),
                                new BigDecimal("200.00")),
                        new OrderCreatedEvent.OrderItemPayload(
                                OTHER_VARIANT_ID,
                                PRODUCT_ID,
                                1,
                                SELLER_KEYCLOAK_ID,
                                new BigDecimal("50.00"),
                                new BigDecimal("50.00"))));

        publisher.publishOrderCreated(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event);
    }

    @Test
    void serializesOrderCreatedEventItemsWithPriceSnapshotAndSubtotal() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "EKK-20250809-AB12",
                null,
                "customer@example.com",
                new BigDecimal("250.00"),
                OrderStatus.CONFIRMED,
                LocalDateTime.of(2025, 8, 9, 12, 0),
                List.of(
                        new OrderCreatedEvent.OrderItemPayload(
                                VARIANT_ID,
                                PRODUCT_ID,
                                2,
                                SELLER_KEYCLOAK_ID,
                                new BigDecimal("100.00"),
                                new BigDecimal("200.00")),
                        new OrderCreatedEvent.OrderItemPayload(
                                OTHER_VARIANT_ID,
                                PRODUCT_ID,
                                1,
                                SELLER_KEYCLOAK_ID,
                                new BigDecimal("50.00"),
                                new BigDecimal("50.00"))));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(event));

        assertEquals(event.orderId().toString(), json.get("orderId").asText());
        assertEquals(event.orderNumber(), json.get("orderNumber").asText());
        assertEquals(event.customerEmail(), json.get("customerEmail").asText());
        assertEquals(0, event.total().compareTo(new BigDecimal(json.get("total").asText())));
        assertEquals(event.status().name(), json.get("status").asText());
        assertEquals(2, json.get("items").size());
        assertEquals(event.customerId() == null, json.get("customerId").isNull());

        JsonNode item0 = json.get("items").get(0);
        assertEquals(VARIANT_ID.toString(), item0.get("variantId").asText());
        assertEquals(PRODUCT_ID.toString(), item0.get("productId").asText());
        assertEquals(2, item0.get("quantity").asInt());
        assertEquals(SELLER_KEYCLOAK_ID.toString(), item0.get("sellerKeycloakId").asText());
        assertEquals(0, new BigDecimal("100.00").compareTo(new BigDecimal(item0.get("priceSnapshot").asText())));
        assertEquals(0, new BigDecimal("200.00").compareTo(new BigDecimal(item0.get("subtotal").asText())));

        JsonNode item1 = json.get("items").get(1);
        assertEquals(OTHER_VARIANT_ID.toString(), item1.get("variantId").asText());
        assertEquals(PRODUCT_ID.toString(), item1.get("productId").asText());
        assertEquals(1, item1.get("quantity").asInt());
        assertEquals(SELLER_KEYCLOAK_ID.toString(), item1.get("sellerKeycloakId").asText());
        assertEquals(0, new BigDecimal("50.00").compareTo(new BigDecimal(item1.get("priceSnapshot").asText())));
        assertEquals(0, new BigDecimal("50.00").compareTo(new BigDecimal(item1.get("subtotal").asText())));
    }

    @Test
    void routesOrderCancelledEventToConfiguredExchangeAndKey() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                "EKK-20250809-AB12",
                UUID.randomUUID(),
                null,
                OrderStatus.CONFIRMED,
                true,
                LocalDateTime.of(2025, 8, 9, 14, 30));

        publisher.publishOrderCancelled(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY,
                event);
    }

    @Test
    void routesOrderConfirmedEventToConfiguredExchangeAndKey() {
        UUID itemId = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(),
                "EKK-20250809-AB12",
                UUID.randomUUID().toString(),
                List.of(new OrderConfirmedEvent.OrderItemConfirmed(itemId, PRODUCT_ID, SELLER_KEYCLOAK_ID,
                        new BigDecimal("200.00"))),
                LocalDateTime.of(2025, 8, 9, 13, 0));

        publisher.publishOrderConfirmed(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CONFIRMED_ROUTING_KEY,
                event);
    }

    @Test
    void routesOrderStatusChangedEventToConfiguredExchangeAndKey() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                UUID.randomUUID(),
                "EKK-20250809-AB12",
                OrderStatus.CONFIRMED,
                OrderStatus.SHIPPED,
                LocalDateTime.of(2025, 8, 9, 14, 30));

        publisher.publishOrderStatusChanged(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_CHANGED_ROUTING_KEY,
                event);
    }
}