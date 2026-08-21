package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.PaymentFailedEvent;
import com.ekko.notification_service.messaging.event.PaymentFailureReason;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    private static final String CUSTOMER_EMAIL = "customer@ekko.test";
    private static final String GUEST_EMAIL = "guest@ekko.test";

    @Mock
    private NotificationOrchestratorService orchestrator;

    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(orchestrator);
    }

    @Test
    void handlePaymentFailedWithCustomerIdBuildsNotificationWithBothChannels() {
        UUID customerId = UUID.randomUUID();
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                customerId,
                CUSTOMER_EMAIL,
                PaymentFailureReason.DECLINED,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        listener.handlePaymentFailed(event);

        NotificationEvent notification = captureProcessedEvent();

        assertEquals(customerId, notification.recipientId());
        assertEquals(CUSTOMER_EMAIL, notification.recipientEmail());
        assertEquals("PAYMENT_FAILED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "orderId", event.orderId(),
                "reason", PaymentFailureReason.DECLINED,
                "failedAt", event.failedAt()), notification.variables());
    }

    @Test
    void handlePaymentFailedWithNullCustomerIdBuildsNotificationWithEmailOnly() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                GUEST_EMAIL,
                PaymentFailureReason.FRAUD,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        listener.handlePaymentFailed(event);

        NotificationEvent notification = captureProcessedEvent();

        assertEquals(null, notification.recipientId());
        assertEquals(GUEST_EMAIL, notification.recipientEmail());
        assertEquals("PAYMENT_FAILED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL), notification.channels());
        assertEquals(Map.of(
                "orderId", event.orderId(),
                "reason", PaymentFailureReason.FRAUD,
                "failedAt", event.failedAt()), notification.variables());
    }

    @Test
    void handlePaymentFailedDoesNotInvokeUserLookupService() {
        // PaymentEventListener does not inject UserLookupService, so verify it's not used
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                CUSTOMER_EMAIL,
                PaymentFailureReason.TIMEOUT,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        listener.handlePaymentFailed(event);

        // Just verify orchestrator was called once - no UserLookupService exists in this listener
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator).process(captor.capture());
    }

    private NotificationEvent captureProcessedEvent() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator).process(captor.capture());
        return captor.getValue();
    }
}