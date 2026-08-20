package com.ekko.notification_service.service;

import com.ekko.notification_service.dto.response.NotificationResponse;
import com.ekko.notification_service.dto.response.UnreadCountResponse;
import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationAccessDeniedException;
import com.ekko.notification_service.exception.NotificationNotFoundException;
import com.ekko.notification_service.mapper.NotificationMapper;
import com.ekko.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findInAppByRecipient(UUID recipientId, Pageable pageable) {
        return notificationRepository
                .findByRecipientIdAndTypeOrderByCreatedAtDesc(recipientId, NotificationType.IN_APP, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(UUID recipientId) {
        long count = notificationRepository
                .countByRecipientIdAndTypeAndReadAtIsNull(recipientId, NotificationType.IN_APP);
        return new UnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification not found with id: " + notificationId));

        if (!notification.getRecipientId().equals(recipientId)) {
            throw new NotificationAccessDeniedException(
                    "Notification " + notificationId + " does not belong to recipient " + recipientId);
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public int markAllAsRead(UUID recipientId) {
        int updated = notificationRepository.markAllAsRead(recipientId, LocalDateTime.now());
        log.info("Marked {} notifications as read for recipient {}", updated, recipientId);
        return updated;
    }
}