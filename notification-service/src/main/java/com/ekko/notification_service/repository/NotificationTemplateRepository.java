package com.ekko.notification_service.repository;

import com.ekko.notification_service.entity.NotificationTemplate;
import com.ekko.notification_service.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByNameAndTypeAndIsActiveTrue(String name, NotificationType type);

    boolean existsByNameAndType(String name, NotificationType type);
}