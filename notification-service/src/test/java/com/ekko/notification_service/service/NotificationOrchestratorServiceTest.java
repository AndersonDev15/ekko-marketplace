package com.ekko.notification_service.service;

import com.ekko.notification_service.channel.NotificationChannel;
import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.entity.NotificationTemplate;
import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationDispatchException;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOrchestratorServiceTest {

    private static final String EVENT_TYPE = "order.confirmed";

    @Mock
    private NotificationTemplateService templateService;
    @Mock
    private TemplateRenderer templateRenderer;
    @Mock
    private NotificationPersistenceService persistenceService;
    @Mock
    private NotificationChannel emailChannel;
    @Mock
    private NotificationChannel inAppChannel;

    private NotificationOrchestratorService orchestrator;

    @BeforeEach
    void setUp() {
        lenient().when(emailChannel.getType()).thenReturn(NotificationType.EMAIL);
        lenient().when(inAppChannel.getType()).thenReturn(NotificationType.IN_APP);
        orchestrator = new NotificationOrchestratorService(
                templateService, templateRenderer, persistenceService, List.of(emailChannel, inAppChannel));
        orchestrator.initChannelMap();
    }

    private NotificationTemplate template(UUID id) {
        return NotificationTemplate.builder()
                .id(id)
                .name(EVENT_TYPE)
                .type(NotificationType.EMAIL)
                .subject("Subject {{nombre}}")
                .body("Body {{nombre}}")
                .build();
    }

    private Notification notification(UUID id) {
        return Notification.builder().id(id).build();
    }

    @Test
    @DisplayName("process retorna sin llamar colaboradores cuando recipientId es null")
    void process_ignoresEventWithoutRecipient() {
        NotificationEvent event = new NotificationEvent(
                null, "customer@ekko.test", EVENT_TYPE, List.of(NotificationType.EMAIL), Map.of("nombre", "Ekko"));

        orchestrator.process(event);

        verifyNoInteractions(templateService, templateRenderer, persistenceService);
        verify(emailChannel, never()).send(any(Notification.class));
        verify(inAppChannel, never()).send(any(Notification.class));
    }

    @Test
    @DisplayName("process continúa al siguiente canal sin excepción cuando no hay template activo")
    void process_skipsChannelWithoutActiveTemplate() {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "customer@ekko.test", EVENT_TYPE, List.of(NotificationType.EMAIL), Map.of());
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.EMAIL))
                .thenReturn(Optional.empty());

        orchestrator.process(event);

        verify(persistenceService, never()).persistPending(any(), any(), any(), any(), any(), any(), any());
        verify(emailChannel, never()).send(any(Notification.class));
    }

    @Test
    @DisplayName("process persiste PENDING pero no envía cuando el canal no está registrado")
    void process_persistsButDoesNotSendWhenChannelNotRegistered() {
        NotificationOrchestratorService withoutEmail = new NotificationOrchestratorService(
                templateService, templateRenderer, persistenceService, List.of(inAppChannel));
        withoutEmail.initChannelMap();

        UUID templateId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationTemplate template = template(templateId);
        Notification notification = notification(notificationId);
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "customer@ekko.test", EVENT_TYPE, List.of(NotificationType.EMAIL), Map.of());
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.EMAIL))
                .thenReturn(Optional.of(template));
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("rendered");
        when(persistenceService.persistPending(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(notification);

        withoutEmail.process(event);

        verify(persistenceService).persistPending(
                event.recipientId(), event.recipientEmail(), NotificationType.EMAIL, templateId,
                "rendered", "rendered", event.variables());
        verify(emailChannel, never()).send(any(Notification.class));
        verify(persistenceService, never()).persistFinalState(any(), any(), any());
    }

    @Test
    @DisplayName("process marca la notificación como SENT cuando el envío no lanza excepción")
    void process_marksSentWhenDispatchSucceeds() {
        UUID templateId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationTemplate template = template(templateId);
        Notification notification = notification(notificationId);
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "customer@ekko.test", EVENT_TYPE, List.of(NotificationType.EMAIL), Map.of());
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.EMAIL))
                .thenReturn(Optional.of(template));
        when(templateRenderer.render(template.getSubject(), event.variables())).thenReturn("Subject rendered");
        when(templateRenderer.render(template.getBody(), event.variables())).thenReturn("Body rendered");
        when(persistenceService.persistPending(event.recipientId(), event.recipientEmail(),
                NotificationType.EMAIL, templateId, "Subject rendered", "Body rendered", event.variables()))
                .thenReturn(notification);

        orchestrator.process(event);

        verify(emailChannel).send(notification);
        verify(persistenceService).persistFinalState(notificationId, NotificationStatus.SENT, null);
    }

    @Test
    @DisplayName("process marca la notificación como FAILED cuando el envío lanza NotificationDispatchException")
    void process_marksFailedWhenDispatchThrows() {
        UUID templateId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationTemplate template = template(templateId);
        Notification notification = notification(notificationId);
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "customer@ekko.test", EVENT_TYPE, List.of(NotificationType.EMAIL), Map.of());
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.EMAIL))
                .thenReturn(Optional.of(template));
        when(templateRenderer.render(anyString(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistenceService.persistPending(eq(event.recipientId()), eq(event.recipientEmail()),
                eq(NotificationType.EMAIL), eq(templateId), anyString(), anyString(), anyMap()))
                .thenReturn(notification);
        doThrow(new NotificationDispatchException("smtp down")).when(emailChannel).send(notification);

        orchestrator.process(event);

        verify(persistenceService).persistFinalState(notificationId, NotificationStatus.FAILED, "smtp down");
    }

    @Test
    @DisplayName("process con múltiples canales: si el primero falla, el segundo igual se procesa")
    void process_continuesWithNextChannelWhenFirstFails() {
        UUID emailTemplateId = UUID.randomUUID();
        UUID inAppTemplateId = UUID.randomUUID();
        UUID emailNotificationId = UUID.randomUUID();
        UUID inAppNotificationId = UUID.randomUUID();
        NotificationTemplate emailTemplate = NotificationTemplate.builder()
                .id(emailTemplateId).name(EVENT_TYPE).type(NotificationType.EMAIL).subject("S").body("B").build();
        NotificationTemplate inAppTemplate = NotificationTemplate.builder()
                .id(inAppTemplateId).name(EVENT_TYPE).type(NotificationType.IN_APP).subject("S").body("B").build();
        Notification emailNotification = notification(emailNotificationId);
        Notification inAppNotification = notification(inAppNotificationId);
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", "Ekko");
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "customer@ekko.test", EVENT_TYPE,
                List.of(NotificationType.EMAIL, NotificationType.IN_APP), variables);
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.EMAIL))
                .thenReturn(Optional.of(emailTemplate));
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.IN_APP))
                .thenReturn(Optional.of(inAppTemplate));
        when(templateRenderer.render(anyString(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistenceService.persistPending(eq(event.recipientId()), eq(event.recipientEmail()),
                eq(NotificationType.EMAIL), eq(emailTemplateId), anyString(), anyString(), anyMap()))
                .thenReturn(emailNotification);
        when(persistenceService.persistPending(eq(event.recipientId()), eq(event.recipientEmail()),
                eq(NotificationType.IN_APP), eq(inAppTemplateId), anyString(), anyString(), anyMap()))
                .thenReturn(inAppNotification);
        doThrow(new NotificationDispatchException("smtp down")).when(emailChannel).send(emailNotification);

        orchestrator.process(event);

        verify(emailChannel).send(emailNotification);
        verify(inAppChannel).send(inAppNotification);
        verify(persistenceService).persistFinalState(emailNotificationId, NotificationStatus.FAILED, "smtp down");
        verify(persistenceService).persistFinalState(inAppNotificationId, NotificationStatus.SENT, null);
    }

    @Test
    @DisplayName("process con múltiples canales: un canal sin template no impide procesar el otro")
    void process_skipsChannelWithoutTemplateAndProcessesOther() {
        UUID templateId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationTemplate emailTemplate = NotificationTemplate.builder()
                .id(templateId).name(EVENT_TYPE).type(NotificationType.EMAIL).subject("S").body("B").build();
        Notification notification = notification(notificationId);
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", "Ekko");
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "customer@ekko.test", EVENT_TYPE,
                List.of(NotificationType.IN_APP, NotificationType.EMAIL), variables);
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.IN_APP))
                .thenReturn(Optional.empty());
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.EMAIL))
                .thenReturn(Optional.of(emailTemplate));
        when(templateRenderer.render(anyString(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistenceService.persistPending(eq(event.recipientId()), eq(event.recipientEmail()),
                eq(NotificationType.EMAIL), eq(templateId), anyString(), anyString(), anyMap()))
                .thenReturn(notification);

        orchestrator.process(event);

        verify(emailChannel).send(notification);
        verify(inAppChannel, never()).send(any(Notification.class));
        verify(persistenceService).persistFinalState(notificationId, NotificationStatus.SENT, null);
    }

    @Test
    @DisplayName("process pasa template.getSubject y template.getBody a templateRenderer.render")
    void process_passesTemplateSubjectAndBodyToRenderer() {
        UUID templateId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationTemplate template = NotificationTemplate.builder()
                .id(templateId).name(EVENT_TYPE).type(NotificationType.EMAIL)
                .subject("Subject {{nombre}}").body("Body {{nombre}}").build();
        Notification notification = notification(notificationId);
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "customer@ekko.test", EVENT_TYPE, List.of(NotificationType.EMAIL), Map.of());
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.EMAIL))
                .thenReturn(Optional.of(template));
        when(templateRenderer.render(anyString(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistenceService.persistPending(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(notification);

        orchestrator.process(event);

        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        verify(templateRenderer, times(2)).render(templateCaptor.capture(), anyMap());
        assertThat(templateCaptor.getAllValues())
                .containsExactly(template.getSubject(), template.getBody());
    }

    @Test
    @DisplayName("process pasa event.variables directamente a persistPending sin transformación")
    void process_passesEventVariablesIdentityToPersistPending() {
        UUID templateId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationTemplate template = template(templateId);
        Notification notification = notification(notificationId);
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", "Ekko");
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "customer@ekko.test", EVENT_TYPE, List.of(NotificationType.EMAIL), variables);
        when(templateService.findActiveByNameAndType(EVENT_TYPE, NotificationType.EMAIL))
                .thenReturn(Optional.of(template));
        when(templateRenderer.render(anyString(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistenceService.persistPending(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(notification);

        orchestrator.process(event);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(persistenceService).persistPending(eq(event.recipientId()), eq(event.recipientEmail()),
                eq(NotificationType.EMAIL), eq(templateId), anyString(), anyString(), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue()).isSameAs(variables);
    }
}