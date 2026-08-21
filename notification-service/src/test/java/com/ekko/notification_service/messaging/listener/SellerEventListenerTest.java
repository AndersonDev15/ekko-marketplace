package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.DocumentStatus;
import com.ekko.notification_service.messaging.event.DocumentType;
import com.ekko.notification_service.messaging.event.SellerCreatedEvent;
import com.ekko.notification_service.messaging.event.SellerDocumentReviewEvent;
import com.ekko.notification_service.messaging.event.SellerStatusChangedEvent;
import com.ekko.notification_service.messaging.event.SellerStatus;
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

@ExtendWith(MockitoExtension.class)
class SellerEventListenerTest {

    @Mock
    private NotificationOrchestratorService orchestrator;

    private SellerEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SellerEventListener(orchestrator);
    }

    @Test
    void onSellerCreatedBuildsNotificationEventWithEmailAndInAppChannels() {
        UUID sellerId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        SellerCreatedEvent event = new SellerCreatedEvent(
                sellerId,
                keycloakId.toString(),
                "Mi tienda",
                "seller@ekko.test",
                SellerStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        listener.onSellerCreated(event);

        NotificationEvent notification = captureProcessedEvent();

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
    void onSellerStatusChangedBuildsNotificationEventWithBothStatuses() {
        UUID sellerId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        SellerStatusChangedEvent event = new SellerStatusChangedEvent(
                sellerId,
                keycloakId.toString(),
                "seller@ekko.test",
                SellerStatus.PENDING_REVIEW,
                SellerStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        listener.onSellerStatusChanged(event);

        NotificationEvent notification = captureProcessedEvent();

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
    void onSellerDocumentReviewBuildsNotificationEventWithNotes() {
        UUID sellerId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        SellerDocumentReviewEvent event = new SellerDocumentReviewEvent(
                sellerId,
                "seller@ekko.test",
                keycloakId.toString(),
                documentId,
                DocumentType.ID_CARD,
                DocumentStatus.APPROVED,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                "Documento verificado");

        listener.onSellerDocumentReview(event);

        NotificationEvent notification = captureProcessedEvent();

        assertEquals(keycloakId, notification.recipientId());
        assertEquals("seller@ekko.test", notification.recipientEmail());
        assertEquals("SELLER_DOCUMENT_REVIEW", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "sellerId", sellerId,
                "email", "seller@ekko.test",
                "documentType", DocumentType.ID_CARD,
                "reviewStatus", DocumentStatus.APPROVED,
                "notes", "Documento verificado"), notification.variables());
    }

    @Test
    void onSellerDocumentReviewFallsBackToEmptyNotesWhenNull() {
        UUID sellerId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        SellerDocumentReviewEvent event = new SellerDocumentReviewEvent(
                sellerId,
                "seller@ekko.test",
                keycloakId.toString(),
                UUID.randomUUID(),
                DocumentType.RUT,
                DocumentStatus.REJECTED,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                null);

        listener.onSellerDocumentReview(event);

        NotificationEvent notification = captureProcessedEvent();

        assertEquals("", notification.variables().get("notes"));
    }

    private NotificationEvent captureProcessedEvent() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator).process(captor.capture());
        return captor.getValue();
    }
}