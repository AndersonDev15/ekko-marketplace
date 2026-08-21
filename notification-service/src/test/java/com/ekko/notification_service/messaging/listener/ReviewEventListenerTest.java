package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.ReviewCreatedEvent;
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
class ReviewEventListenerTest {

    private static final String SELLER_EMAIL = "seller@ekko.test";

    @Mock
    private NotificationOrchestratorService orchestrator;

    @Mock
    private UserLookupService userLookupService;

    private ReviewEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ReviewEventListener(orchestrator, userLookupService);
    }

    @Test
    void handleReviewCreatedResolvesSellerEmailAndBuildsNotificationEvent() {
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

        listener.handleReviewCreated(event);

        NotificationEvent notification = captureProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
        assertEquals("REVIEW_CREATED", notification.eventType());
        assertEquals(List.of(NotificationType.EMAIL, NotificationType.IN_APP), notification.channels());
        assertEquals(Map.of(
                "productId", productId,
                "rating", 5,
                "title", "Excelente producto",
                "comment", "Muy buena calidad"), notification.variables());
    }

    @Test
    void handleReviewCreatedFallsBackToEmptyStringsWhenTitleAndCommentAreNull() {
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

        listener.handleReviewCreated(event);

        NotificationEvent notification = captureProcessedEvent();

        assertEquals("", notification.variables().get("title"));
        assertEquals("", notification.variables().get("comment"));
    }

    @Test
    void handleReviewCreatedUsesSellerKeycloakIdAsRecipientIdNotCustomerId() {
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

        listener.handleReviewCreated(event);

        NotificationEvent notification = captureProcessedEvent();

        assertEquals(sellerKeycloakId, notification.recipientId());
        assertEquals(SELLER_EMAIL, notification.recipientEmail());
    }

    @Test
    void propagatesUserLookupExceptionWithoutCatching() {
        UUID sellerKeycloakId = UUID.randomUUID();
        when(userLookupService.resolveEmail(sellerKeycloakId))
                .thenThrow(new IllegalStateException("user not found"));
        ReviewCreatedEvent event = new ReviewCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                sellerKeycloakId,
                "customer-123",
                5,
                "Test",
                "Test",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        try {
            listener.handleReviewCreated(event);
        } catch (IllegalStateException expected) {
            verify(orchestrator, org.mockito.Mockito.never()).process(org.mockito.ArgumentMatchers.any());
            return;
        }
        throw new AssertionError("expected IllegalStateException to propagate");
    }

    private NotificationEvent captureProcessedEvent() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(orchestrator).process(captor.capture());
        return captor.getValue();
    }
}