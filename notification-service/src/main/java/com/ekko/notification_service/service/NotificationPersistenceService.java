package com.ekko.notification_service.service;

import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPersistenceService {

    private final NotificationRepository repository;

    @Transactional
    public Notification persistPending(
            UUID recipientId,
            String recipientEmail,
            NotificationType type,
            UUID templateId,
            String subject,
            String body,
            Map<String, Object> renderedVariables) {
        // Guest checkouts have no account, so the notification is delivered only by EMAIL and
        // recipientId has no functional meaning (it is never consulted via GET /notifications/me,
        // there is no guest JWT). The DB column is NOT NULL though, so a synthetic UUID is generated
        // solely to satisfy the constraint.
        UUID persistedRecipientId = recipientId != null ? recipientId : UUID.randomUUID();
        Notification notification = Notification.builder()
                .recipientId(persistedRecipientId)
                .recipientEmail(recipientEmail)
                .type(type)
                .templateId(templateId)
                .subject(subject)
                .body(body)
                .renderedVariables(renderedVariables != null ? renderedVariables : Map.of())
                .status(NotificationStatus.PENDING)
                .build();
        return repository.save(notification);
    }

    @Transactional
    public void persistFinalState(UUID notificationId, NotificationStatus status, String errorMessage) {
        Notification notification = repository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.error("Notification not found for final state update, id={}. Status {} will not be applied",
                    notificationId, status);
            return;
        }
        notification.setStatus(status);
        if (status == NotificationStatus.SENT) {
            notification.setSentAt(LocalDateTime.now());
        } else if (status == NotificationStatus.FAILED) {
            notification.setErrorMessage(errorMessage);
        }
        repository.save(notification);
    }
}