package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.InventoryLowStockEvent;
import com.ekko.notification_service.messaging.event.ProductDeactivatedEvent;
import com.ekko.notification_service.messaging.event.ProductPublishedEvent;
import com.ekko.notification_service.messaging.event.ProductRejectedEvent;
import com.ekko.notification_service.messaging.event.ProductStatus;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import com.ekko.notification_service.service.UserLookupService;
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
class ProductEventListenerTest {

    private static final String SELLER_EMAIL = "seller@ekko.test";

    @Mock
    private NotificationOrchestratorService orchestrator;

    @Mock
    private UserLookupService userLookupService;

    private ProductEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ProductEventListener(orchestrator, userLookupService);
    }

    @Test
    void onProductPublishedResolvesEmailAndBuildsNotificationEvent() {
        UUID productId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ProductPublishedEvent event = new ProductPublishedEvent(
                productId,
                sellerKeycloakId,
                "Zapatillas deportivas",
                "Calzado",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        listener.onProductPublished(event);

        NotificationEvent notification = captureProcessedEvent();

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
    void onProductRejectedResolvesEmailAndBuildsNotificationEvent() {
        UUID productId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ProductRejectedEvent event = new ProductRejectedEvent(
                productId,
                sellerKeycloakId,
                "Zapatillas deportivas",
                "Foto de mala calidad",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        listener.onProductRejected(event);

        NotificationEvent notification = captureProcessedEvent();

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
    void onProductDeactivatedResolvesEmailAndBuildsNotificationEvent() {
        UUID productId = UUID.randomUUID();
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId)).thenReturn(SELLER_EMAIL);
        ProductDeactivatedEvent event = new ProductDeactivatedEvent(
                productId,
                sellerKeycloakId,
                ProductStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        listener.onProductDeactivated(event);

        NotificationEvent notification = captureProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("PRODUCT_DEACTIVATED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "productId", productId,
                "previousStatus", ProductStatus.ACTIVE), notification.variables());
    }

    @Test
    void onInventoryLowStockResolvesEmailAndBuildsNotificationEvent() {
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

        listener.onInventoryLowStock(event);

        NotificationEvent notification = captureProcessedEvent();

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

    private NotificationEvent captureProcessedEvent() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator).process(captor.capture());
        return captor.getValue();
    }
}