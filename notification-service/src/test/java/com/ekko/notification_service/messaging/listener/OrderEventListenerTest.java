package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.OrderCancelledEvent;
import com.ekko.notification_service.messaging.event.OrderConfirmedEvent;
import com.ekko.notification_service.messaging.event.OrderCreatedEvent;
import com.ekko.notification_service.messaging.event.OrderStatus;
import com.ekko.notification_service.messaging.event.OrderStatusChangedEvent;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import com.ekko.notification_service.service.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    private static final String CUSTOMER_EMAIL = "customer@ekko.test";
    private static final String SELLER_1_EMAIL = "seller1@ekko.test";
    private static final String SELLER_2_EMAIL = "seller2@ekko.test";

    @Mock
    private NotificationOrchestratorService orchestrator;

    @Mock
    private UserLookupService userLookupService;

    private OrderEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderEventListener(orchestrator, userLookupService);
    }

    @Test
    void onOrderCreatedBuildsCustomerAndPerSellerNotifications() {
        UUID customerId = UUID.randomUUID();
        UUID seller1 = UUID.randomUUID();
        UUID seller2 = UUID.randomUUID();
        when(userLookupService.resolveEmail(seller1)).thenReturn(SELLER_1_EMAIL);
        when(userLookupService.resolveEmail(seller2)).thenReturn(SELLER_2_EMAIL);
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                new BigDecimal("300.00"),
                OrderStatus.CONFIRMED,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                List.of(
                        new OrderCreatedEvent.OrderItemPayload(
                                UUID.randomUUID(), UUID.randomUUID(), 2, seller1,
                                new BigDecimal("100.00"), new BigDecimal("200.00")),
                        new OrderCreatedEvent.OrderItemPayload(
                                UUID.randomUUID(), UUID.randomUUID(), 1, seller2,
                                new BigDecimal("100.00"), new BigDecimal("100.00"))));

        listener.onOrderCreated(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator, times(3)).process(captor.capture());
        List<NotificationEvent> notifications = captor.getAllValues();

        NotificationEvent customer = notifications.get(0);
        assertEquals(customerId, customer.recipientId());
        assertEquals(CUSTOMER_EMAIL, customer.recipientEmail());
        assertEquals("ORDER_CREATED", customer.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), customer.channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "total", new BigDecimal("300.00"),
                "itemCount", 2), customer.variables());

        NotificationEvent seller1Notification = notifications.get(1);
        assertEquals(seller1, seller1Notification.recipientId());
        assertEquals(SELLER_1_EMAIL, seller1Notification.recipientEmail());
        assertEquals("ORDER_CREATED_SELLER", seller1Notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), seller1Notification.channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 1L), seller1Notification.variables());

        NotificationEvent seller2Notification = notifications.get(2);
        assertEquals(seller2, seller2Notification.recipientId());
        assertEquals(SELLER_2_EMAIL, seller2Notification.recipientEmail());
        assertEquals("ORDER_CREATED_SELLER", seller2Notification.eventType());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 1L), seller2Notification.variables());
    }

    @Test
    void onOrderCreatedForGuestUsesEmailChannelOnlyAndNullRecipientId() {
        UUID seller1 = UUID.randomUUID();
        when(userLookupService.resolveEmail(seller1)).thenReturn(SELLER_1_EMAIL);
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                null,
                "guest@ekko.test",
                new BigDecimal("200.00"),
                OrderStatus.PENDING,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                List.of(new OrderCreatedEvent.OrderItemPayload(
                        UUID.randomUUID(), UUID.randomUUID(), 1, seller1,
                        new BigDecimal("200.00"), new BigDecimal("200.00"))));

        listener.onOrderCreated(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator, times(2)).process(captor.capture());
        List<NotificationEvent> notifications = captor.getAllValues();

        NotificationEvent customer = notifications.get(0);
        assertEquals(null, customer.recipientId());
        assertEquals("guest@ekko.test", customer.recipientEmail());
        assertEquals("ORDER_CREATED", customer.eventType());
        assertEquals(List.of(NotificationType.EMAIL), customer.channels());
    }

    @Test
    void onOrderConfirmedBuildsCustomerAndPerSellerNotifications() {
        UUID customerId = UUID.randomUUID();
        UUID seller1 = UUID.randomUUID();
        UUID seller2 = UUID.randomUUID();
        when(userLookupService.resolveEmail(seller1)).thenReturn(SELLER_1_EMAIL);
        when(userLookupService.resolveEmail(seller2)).thenReturn(SELLER_2_EMAIL);
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                List.of(
                        new OrderConfirmedEvent.OrderItemConfirmed(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, seller1,
                                new BigDecimal("200.00")),
                        new OrderConfirmedEvent.OrderItemConfirmed(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, seller2,
                                new BigDecimal("100.00"))),
                LocalDateTime.of(2026, 8, 20, 11, 0));

        listener.onOrderConfirmed(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator, times(3)).process(captor.capture());
        List<NotificationEvent> notifications = captor.getAllValues();

        NotificationEvent customer = notifications.get(0);
        assertEquals(customerId, customer.recipientId());
        assertEquals(CUSTOMER_EMAIL, customer.recipientEmail());
        assertEquals("ORDER_CONFIRMED", customer.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), customer.channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 2), customer.variables());

        assertEquals("ORDER_CONFIRMED_SELLER", notifications.get(1).eventType());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 1L), notifications.get(1).variables());
        assertEquals("ORDER_CONFIRMED_SELLER", notifications.get(2).eventType());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 1L), notifications.get(2).variables());
    }

    @Test
    void onOrderCancelledBuildsCustomerAndPerSellerNotifications() {
        UUID customerId = UUID.randomUUID();
        UUID seller1 = UUID.randomUUID();
        UUID seller2 = UUID.randomUUID();
        when(userLookupService.resolveEmail(seller1)).thenReturn(SELLER_1_EMAIL);
        when(userLookupService.resolveEmail(seller2)).thenReturn(SELLER_2_EMAIL);
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                OrderStatus.CONFIRMED,
                true,
                List.of(
                        new OrderCancelledEvent.OrderItemCancelled(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, seller1),
                        new OrderCancelledEvent.OrderItemCancelled(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, seller2)),
                LocalDateTime.of(2026, 8, 20, 12, 0));

        listener.onOrderCancelled(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator, times(3)).process(captor.capture());
        List<NotificationEvent> notifications = captor.getAllValues();

        NotificationEvent customer = notifications.get(0);
        assertEquals(customerId, customer.recipientId());
        assertEquals(CUSTOMER_EMAIL, customer.recipientEmail());
        assertEquals("ORDER_CANCELLED", customer.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), customer.channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "refundRequired", true,
                "previousStatus", OrderStatus.CONFIRMED), customer.variables());

        assertEquals("ORDER_CANCELLED_SELLER", notifications.get(1).eventType());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "refundRequired", true), notifications.get(1).variables());
        assertEquals("ORDER_CANCELLED_SELLER", notifications.get(2).eventType());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "refundRequired", true), notifications.get(2).variables());
    }

    @Test
    void onOrderStatusChangedBuildsCustomerAndPerSellerNotifications() {
        UUID customerId = UUID.randomUUID();
        UUID seller1 = UUID.randomUUID();
        UUID seller2 = UUID.randomUUID();
        when(userLookupService.resolveEmail(seller1)).thenReturn(SELLER_1_EMAIL);
        when(userLookupService.resolveEmail(seller2)).thenReturn(SELLER_2_EMAIL);
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                OrderStatus.CONFIRMED,
                OrderStatus.SHIPPED,
                List.of(seller1, seller2),
                LocalDateTime.of(2026, 8, 20, 13, 0));

        listener.onOrderStatusChanged(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator, times(3)).process(captor.capture());
        List<NotificationEvent> notifications = captor.getAllValues();

        NotificationEvent customer = notifications.get(0);
        assertEquals(customerId, customer.recipientId());
        assertEquals(CUSTOMER_EMAIL, customer.recipientEmail());
        assertEquals("ORDER_STATUS_CHANGED", customer.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), customer.channels());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "previousStatus", OrderStatus.CONFIRMED,
                "newStatus", OrderStatus.SHIPPED), customer.variables());

        assertEquals("ORDER_STATUS_CHANGED_SELLER", notifications.get(1).eventType());
        assertEquals(seller1, notifications.get(1).recipientId());
        assertEquals(SELLER_1_EMAIL, notifications.get(1).recipientEmail());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "previousStatus", OrderStatus.CONFIRMED,
                "newStatus", OrderStatus.SHIPPED), notifications.get(1).variables());

        assertEquals("ORDER_STATUS_CHANGED_SELLER", notifications.get(2).eventType());
        assertEquals(seller2, notifications.get(2).recipientId());
        assertEquals(SELLER_2_EMAIL, notifications.get(2).recipientEmail());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "previousStatus", OrderStatus.CONFIRMED,
                "newStatus", OrderStatus.SHIPPED), notifications.get(2).variables());
    }

    @Test
    void sellerNotificationsAreDeduplicatedBySellerId() {
        UUID customerId = UUID.randomUUID();
        UUID seller1 = UUID.randomUUID();
        when(userLookupService.resolveEmail(seller1)).thenReturn(SELLER_1_EMAIL);
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                customerId,
                CUSTOMER_EMAIL,
                new BigDecimal("300.00"),
                OrderStatus.PENDING,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                List.of(
                        new OrderCreatedEvent.OrderItemPayload(
                                UUID.randomUUID(), UUID.randomUUID(), 2, seller1,
                                new BigDecimal("100.00"), new BigDecimal("200.00")),
                        new OrderCreatedEvent.OrderItemPayload(
                                UUID.randomUUID(), UUID.randomUUID(), 1, seller1,
                                new BigDecimal("100.00"), new BigDecimal("100.00"))));

        listener.onOrderCreated(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator, times(2)).process(captor.capture());
        List<NotificationEvent> notifications = captor.getAllValues();

        assertEquals("ORDER_CREATED", notifications.get(0).eventType());
        assertEquals("ORDER_CREATED_SELLER", notifications.get(1).eventType());
        assertEquals(Map.of(
                "orderNumber", "EKK-20260820-AB12",
                "itemCount", 2L), notifications.get(1).variables());
    }

    @Test
    void propagatesUserLookupExceptionWithoutCatching() {
        UUID seller1 = UUID.randomUUID();
        when(userLookupService.resolveEmail(seller1))
                .thenThrow(new IllegalStateException("user not found"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "EKK-20260820-AB12",
                UUID.randomUUID(),
                CUSTOMER_EMAIL,
                new BigDecimal("200.00"),
                OrderStatus.PENDING,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                List.of(new OrderCreatedEvent.OrderItemPayload(
                        UUID.randomUUID(), UUID.randomUUID(), 1, seller1,
                        new BigDecimal("200.00"), new BigDecimal("200.00"))));

        try {
            listener.onOrderCreated(event);
        } catch (IllegalStateException expected) {
            verify(orchestrator, times(1)).process(any(NotificationEvent.class));
            verify(orchestrator, never()).process(org.mockito.ArgumentMatchers.argThat(
                    n -> "ORDER_CREATED_SELLER".equals(n.eventType())));
            return;
        }
        throw new AssertionError("expected IllegalStateException to propagate");
    }
}