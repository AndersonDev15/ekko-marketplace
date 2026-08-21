package com.ekko.notification_service.service;

import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPersistenceServiceTest {

    @Mock
    private NotificationRepository repository;

    private NotificationPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new NotificationPersistenceService(repository);
    }

    @Test
    @DisplayName("persistPending construye la entidad con status PENDING y readAt/sentAt/metadata null")
    void persistPending_buildsPendingNotification() {
        UUID recipientId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.persistPending(recipientId, "customer@ekko.test", NotificationType.EMAIL,
                templateId, "Subject", "Body", Map.of("nombre", "Ekko"));

        Notification captured = captureSaved();
        assertThat(captured.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(captured.getReadAt()).isNull();
        assertThat(captured.getSentAt()).isNull();
        assertThat(captured.getMetadata()).isNull();
        assertThat(captured.getRecipientId()).isEqualTo(recipientId);
        assertThat(captured.getTemplateId()).isEqualTo(templateId);
    }

    @Test
    @DisplayName("persistPending genera un UUID sintético cuando recipientId es null (guest checkout)")
    void persistPending_generatesSyntheticRecipientIdWhenNull() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.persistPending(null, "guest@ekko.test", NotificationType.EMAIL,
                UUID.randomUUID(), "Subject", "Body", Map.of());

        Notification captured = captureSaved();
        assertThat(captured.getRecipientId()).isNotNull();
        assertThat(captured.getRecipientEmail()).isEqualTo("guest@ekko.test");
    }

    @Test
    @DisplayName("persistPending usa Map.of() cuando renderedVariables es null")
    void persistPending_usesEmptyMapWhenVariablesNull() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.persistPending(UUID.randomUUID(), null, NotificationType.IN_APP,
                UUID.randomUUID(), null, "Body", null);

        Notification captured = captureSaved();
        assertThat(captured.getRenderedVariables()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("persistPending preserva el Map recibido sin transformación")
    void persistPending_preservesVariablesMapIdentity() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", "Ekko");
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.persistPending(UUID.randomUUID(), null, NotificationType.IN_APP,
                UUID.randomUUID(), null, "Body", variables);

        assertThat(captureSaved().getRenderedVariables()).isSameAs(variables);
    }

    @Test
    @DisplayName("persistFinalState con SENT setea sentAt y deja errorMessage null")
    void persistFinalState_sent_setsSentAt() {
        UUID id = UUID.randomUUID();
        Notification existing = Notification.builder().id(id).build();
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.persistFinalState(id, NotificationStatus.SENT, null);

        assertThat(existing.getSentAt()).isNotNull();
        assertThat(existing.getErrorMessage()).isNull();
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("persistFinalState con FAILED setea errorMessage y deja sentAt null")
    void persistFinalState_failed_setsErrorMessage() {
        UUID id = UUID.randomUUID();
        Notification existing = Notification.builder().id(id).build();
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.persistFinalState(id, NotificationStatus.FAILED, "smtp down");

        assertThat(existing.getErrorMessage()).isEqualTo("smtp down");
        assertThat(existing.getSentAt()).isNull();
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("persistFinalState no persiste cuando el id no existe en el repository")
    void persistFinalState_doesNotSaveWhenIdMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        service.persistFinalState(id, NotificationStatus.SENT, null);

        verify(repository, never()).save(any());
    }

    private Notification captureSaved() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}