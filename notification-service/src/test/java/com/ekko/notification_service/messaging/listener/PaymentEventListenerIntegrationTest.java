package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.AbstractRabbitMqIntegrationTest;
import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.PaymentFailedEvent;
import com.ekko.notification_service.messaging.event.PaymentFailureReason;
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

class PaymentEventListenerIntegrationTest extends AbstractRabbitMqIntegrationTest {

    private static final String PAYMENT_SERVICE_TYPE_ID =
            "com.ekko.payment_service.domain.event.PaymentFailedEvent";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private NotificationOrchestratorService orchestrator;

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
    void receivesPaymentFailedEventWithCustomerIdBuildsNotificationWithBothChannels() {
        UUID customerId = UUID.randomUUID();
        String customerEmail = "customer@ekko.test";
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                customerId,
                customerEmail,
                PaymentFailureReason.DECLINED,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(customerId, notification.recipientId());
        assertEquals(customerEmail, notification.recipientEmail());
        assertEquals("PAYMENT_FAILED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "orderId", event.orderId(),
                "reason", PaymentFailureReason.DECLINED,
                "failedAt", event.failedAt()), notification.variables());
    }

    @Test
    void receivesPaymentFailedEventWithNullCustomerIdBuildsNotificationWithEmailOnly() {
        String guestEmail = "guest@ekko.test";
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                guestEmail,
                PaymentFailureReason.FRAUD,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                event);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(null, notification.recipientId());
        assertEquals(guestEmail, notification.recipientEmail());
        assertEquals("PAYMENT_FAILED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL), notification.channels());
        assertEquals(Map.of(
                "orderId", event.orderId(),
                "reason", PaymentFailureReason.FRAUD,
                "failedAt", event.failedAt()), notification.variables());
    }

    @Test
    void deserializesMessageProducedWithPaymentServiceTypeId() {
        UUID customerId = UUID.randomUUID();
        String customerEmail = "customer@ekko.test";
        String json = """
                {"paymentId":"%s","orderId":"%s","customerId":"%s",
                 "customerEmail":"%s","reason":"DECLINED",
                 "failedAt":"2026-01-01T10:00:00"}""".formatted(
                UUID.randomUUID(), UUID.randomUUID(), customerId, customerEmail);

        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setHeader("__TypeId__", PAYMENT_SERVICE_TYPE_ID)
                .build();

        rabbitTemplate.send(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                message);

        NotificationEvent notification = awaitProcessedEvent();

        assertEquals(customerId, notification.recipientId());
        assertEquals(customerEmail, notification.recipientEmail());
        assertEquals("PAYMENT_FAILED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
    }

    private NotificationEvent awaitProcessedEvent() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(orchestrator).process(captor.capture()));
        return captor.getValue();
    }
}