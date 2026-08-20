package com.ekko.notification_service.repository;

import com.ekko.notification_service.config.AbstractPostgresIntegrationTest;
import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.entity.NotificationTemplate;
import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private NotificationRepository repository;
    @Autowired
    private NotificationTemplateRepository templateRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("findByRecipientIdAndTypeOrderByCreatedAtDesc retorna solo IN_APP del recipient ordenadas descendente")
    void findByRecipient_filtersAndOrders() {
        UUID recipientId = UUID.randomUUID();
        UUID otherRecipient = UUID.randomUUID();
        UUID templateId = createTemplate();

        insertNotification(templateId, recipientId, "IN_APP", "s1", null, "2025-01-01 10:00:00");
        insertNotification(templateId, recipientId, "IN_APP", "s2", null, "2025-01-02 10:00:00");
        insertNotification(templateId, recipientId, "IN_APP", "s3", null, "2025-01-03 10:00:00");
        insertNotification(templateId, recipientId, "EMAIL", "s4", null, "2025-01-04 10:00:00");
        insertNotification(templateId, otherRecipient, "IN_APP", "s5", null, "2025-01-05 10:00:00");

        Page<Notification> result = repository.findByRecipientIdAndTypeOrderByCreatedAtDesc(
                recipientId, NotificationType.IN_APP, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Notification::getSubject)
                .containsExactly("s3", "s2", "s1");
    }

    @Test
    @DisplayName("countByRecipientIdAndTypeAndReadAtIsNull cuenta solo las no leídas de tipo IN_APP")
    void countUnread_countsOnlyUnreadInApp() {
        UUID recipientId = UUID.randomUUID();
        UUID templateId = createTemplate();

        insertNotification(templateId, recipientId, "IN_APP", "s1", null, "2025-01-01 10:00:00");
        insertNotification(templateId, recipientId, "IN_APP", "s2", null, "2025-01-02 10:00:00");
        insertNotification(templateId, recipientId, "IN_APP", "s3", Timestamp.valueOf("2025-01-01 09:00:00"),
                "2025-01-03 10:00:00");
        insertNotification(templateId, recipientId, "EMAIL", "s4", null, "2025-01-04 10:00:00");

        long count = repository.countByRecipientIdAndTypeAndReadAtIsNull(recipientId, NotificationType.IN_APP);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("markAllAsRead actualiza solo las IN_APP no leídas del recipient y retorna el count")
    void markAllAsRead_updatesOnlyUnreadInAppOfRecipient() {
        UUID recipientId = UUID.randomUUID();
        UUID otherRecipient = UUID.randomUUID();
        UUID templateId = createTemplate();
        Timestamp alreadyRead = Timestamp.valueOf("2025-01-01 09:00:00");

        insertNotification(templateId, recipientId, "IN_APP", "s1", null, "2025-01-01 10:00:00");
        insertNotification(templateId, recipientId, "IN_APP", "s2", null, "2025-01-02 10:00:00");
        insertNotification(templateId, recipientId, "IN_APP", "s3", alreadyRead, "2025-01-03 10:00:00");
        insertNotification(templateId, recipientId, "EMAIL", "s4", null, "2025-01-04 10:00:00");
        insertNotification(templateId, otherRecipient, "IN_APP", "s5", null, "2025-01-05 10:00:00");

        Integer updated = transactionTemplate.execute(
                status -> repository.markAllAsRead(recipientId, LocalDateTime.now()));

        assertThat(updated).isEqualTo(2);
        assertThat(unreadInAppCount(recipientId)).isZero();
        assertThat(unreadInAppCount(otherRecipient)).isEqualTo(1);
        assertThat(unreadEmailCount(recipientId)).isEqualTo(1);
        assertThat(countByRecipientAndStatus(recipientId, "IN_APP", "s3", alreadyRead)).isEqualTo(1);
    }

    @Test
    @DisplayName("renderedVariables jsonb persiste y recupera un Map con valores anidados")
    void jsonbRoundTrip_persistsAndReadsNestedVariables() {
        UUID templateId = createTemplate();
        UUID recipientId = UUID.randomUUID();

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nombre", "Ekko");
        variables.put("cantidad", 5);
        variables.put("extra", Map.of("inner", Map.of("deep", "value")));

        Notification saved = repository.save(Notification.builder()
                .templateId(templateId)
                .recipientId(recipientId)
                .recipientEmail("customer@ekko.test")
                .type(NotificationType.IN_APP)
                .subject("Asunto")
                .body("Cuerpo")
                .status(NotificationStatus.PENDING)
                .renderedVariables(variables)
                .build());

        Notification reloaded = repository.findById(saved.getId()).orElseThrow();

        JsonNode storedNode = objectMapper.valueToTree(reloaded.getRenderedVariables());
        assertThat(storedNode).isEqualTo(objectMapper.valueToTree(variables));
        assertThat(storedNode.get("nombre").asText()).isEqualTo("Ekko");
        assertThat(storedNode.get("cantidad").asInt()).isEqualTo(5);
        assertThat(storedNode.get("extra").get("inner").get("deep").asText()).isEqualTo("value");
    }

    private UUID createTemplate() {
        return templateRepository.save(NotificationTemplate.builder()
                .name("order.confirmed-" + UUID.randomUUID())
                .type(NotificationType.EMAIL)
                .subject("Subject")
                .body("Body")
                .variables("{}")
                .build()).getId();
    }

    private void insertNotification(UUID templateId, UUID recipientId, String type, String subject,
                                    Timestamp readAt, String createdAt) {
        jdbcTemplate.update("""
                        INSERT INTO notifications
                            (template_id, recipient_id, recipient_email, type, subject, body, status,
                             rendered_variables, read_at, created_at, updated_at)
                        VALUES (?, ?, ?, '%s', ?, ?, 'PENDING', '{}', ?, ?, NOW())
                        """.formatted(type),
                templateId, recipientId, "customer@ekko.test", subject, "Body " + subject,
                readAt, Timestamp.valueOf(createdAt));
    }

    private int unreadInAppCount(UUID recipientId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'IN_APP' AND read_at IS NULL",
                Integer.class, recipientId);
    }

    private int unreadEmailCount(UUID recipientId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'EMAIL' AND read_at IS NULL",
                Integer.class, recipientId);
    }

    private int countByRecipientAndStatus(UUID recipientId, String type, String subject, Timestamp readAt) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type::text = ? AND subject = ? AND read_at = ?",
                Integer.class, recipientId, type, subject, readAt);
    }
}