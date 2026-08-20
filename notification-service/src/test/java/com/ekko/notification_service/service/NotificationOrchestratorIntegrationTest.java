package com.ekko.notification_service.service;

import com.ekko.notification_service.config.AbstractPostgresIntegrationTest;
import com.ekko.notification_service.entity.NotificationTemplate;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = "management.health.mail.enabled=false")
class NotificationOrchestratorIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private NotificationOrchestratorService orchestrator;
    @Autowired
    private com.ekko.notification_service.repository.NotificationTemplateRepository templateRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void stubMailSender() {
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    @DisplayName("flujo completo: con template activo EMAIL, la notificación queda SENT con sentAt no-null")
    void process_endToEndMarksSentWithRenderedTemplate() {
        UUID templateId = templateRepository.save(NotificationTemplate.builder()
                .name("order.confirmed")
                .type(NotificationType.EMAIL)
                .subject("Hola {{nombre}}")
                .body("Tu pedido {{numero}}")
                .variables("{}")
                .build()).getId();
        UUID recipientId = UUID.randomUUID();
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", "Ekko");
        variables.put("numero", "EKK-1");
        NotificationEvent event = new NotificationEvent(
                recipientId, "customer@ekko.test", "order.confirmed",
                List.of(NotificationType.EMAIL), variables);

        orchestrator.process(event);

        assertThat(notificationCount(recipientId)).isEqualTo(1);
        assertThat(queryString("SELECT status FROM notifications WHERE recipient_id = ?", recipientId))
                .isEqualTo("SENT");
        assertThat(queryBoolean("SELECT sent_at IS NOT NULL FROM notifications WHERE recipient_id = ?", recipientId))
                .isTrue();
        assertThat(queryString("SELECT subject FROM notifications WHERE recipient_id = ?", recipientId))
                .isEqualTo("Hola Ekko");
        assertThat(queryString("SELECT body FROM notifications WHERE recipient_id = ?", recipientId))
                .isEqualTo("Tu pedido EKK-1");
        assertThat(queryString("SELECT template_id::text FROM notifications WHERE recipient_id = ?", recipientId))
                .isEqualTo(templateId.toString());
    }

    @Test
    @DisplayName("flujo sin template: process no persiste ninguna notificación")
    void process_doesNotPersistWhenNoActiveTemplate() {
        UUID recipientId = UUID.randomUUID();
        NotificationEvent event = new NotificationEvent(
                recipientId, "customer@ekko.test", "unknown.event",
                List.of(NotificationType.EMAIL), Map.of("nombre", "Ekko"));

        orchestrator.process(event);

        assertThat(notificationCount(recipientId)).isZero();
    }

    @Test
    @DisplayName("flujo con fallo de envío: la notificación queda FAILED con errorMessage")
    void process_marksFailedWhenMailSenderThrows() {
        templateRepository.save(NotificationTemplate.builder()
                .name("order.shipped")
                .type(NotificationType.EMAIL)
                .subject("Enviado {{nombre}}")
                .body("Body")
                .variables("{}")
                .build());
        doThrow(new MailSendException("smtp down"))
                .when(javaMailSender).send(any(MimeMessage.class));
        UUID recipientId = UUID.randomUUID();
        NotificationEvent event = new NotificationEvent(
                recipientId, "customer@ekko.test", "order.shipped",
                List.of(NotificationType.EMAIL), Map.of("nombre", "Ekko"));

        orchestrator.process(event);

        assertThat(notificationCount(recipientId)).isEqualTo(1);
        assertThat(queryString("SELECT status FROM notifications WHERE recipient_id = ?", recipientId))
                .isEqualTo("FAILED");
        assertThat(queryString("SELECT error_message FROM notifications WHERE recipient_id = ?", recipientId))
                .isEqualTo("Failed to send email notification");
        assertThat(queryBoolean("SELECT sent_at IS NULL FROM notifications WHERE recipient_id = ?", recipientId))
                .isTrue();
    }

    @Test
    @DisplayName("renderedVariables persistido coincide con las variables del evento (comparado como JsonNode)")
    void process_persistsRenderedVariablesMatchingEvent() throws Exception {
        templateRepository.save(NotificationTemplate.builder()
                .name("order.confirmed")
                .type(NotificationType.EMAIL)
                .subject("Hola {{nombre}}")
                .body("Body")
                .variables("{}")
                .build());
        UUID recipientId = UUID.randomUUID();
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", "Ekko");
        variables.put("cantidad", 5);
        variables.put("extra", Map.of("inner", "value"));
        NotificationEvent event = new NotificationEvent(
                recipientId, "customer@ekko.test", "order.confirmed",
                List.of(NotificationType.EMAIL), variables);

        orchestrator.process(event);

        String storedJson = jdbcTemplate.queryForObject(
                "SELECT rendered_variables::text FROM notifications WHERE recipient_id = ?",
                String.class, recipientId);
        JsonNode storedNode = objectMapper.readTree(storedJson);
        assertThat(storedNode).isEqualTo(objectMapper.valueToTree(variables));
    }

    private int notificationCount(UUID recipientId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE recipient_id = ?", Integer.class, recipientId);
    }

    private String queryString(String sql, UUID recipientId) {
        return jdbcTemplate.queryForObject(sql, String.class, recipientId);
    }

    private Boolean queryBoolean(String sql, UUID recipientId) {
        return jdbcTemplate.queryForObject(sql, Boolean.class, recipientId);
    }
}