package com.ekko.notification_service.repository;

import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(
            UUID recipientId,
            NotificationType type,
            Pageable pageable
    );

    long countByRecipientIdAndTypeAndReadAtIsNull(
            UUID recipientId,
            NotificationType type
    );

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt " +
            "WHERE n.recipientId = :recipientId AND n.type = 'IN_APP' AND n.readAt IS NULL")
    int markAllAsRead(
            @Param("recipientId") UUID recipientId,
            @Param("readAt") LocalDateTime readAt
    );
}