package com.ekko.notification_service.service;

import com.ekko.notification_service.dto.response.NotificationResponse;
import com.ekko.notification_service.dto.response.UnreadCountResponse;
import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationAccessDeniedException;
import com.ekko.notification_service.exception.NotificationNotFoundException;
import com.ekko.notification_service.mapper.NotificationMapper;
import com.ekko.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, notificationMapper);
    }

    @Test
    @DisplayName("findInAppByRecipient consulta por tipo IN_APP y mapea a Page<NotificationResponse>")
    void findInAppByRecipient_queriesInAppAndMapsPage() {
        UUID recipientId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        @SuppressWarnings("unchecked")
        Page<Notification> page = org.mockito.Mockito.mock(Page.class);
        @SuppressWarnings("unchecked")
        Page<NotificationResponse> mappedPage = org.mockito.Mockito.mock(Page.class);
        when(notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(
                recipientId, NotificationType.IN_APP, pageable)).thenReturn(page);
        when(page.map(any(Function.class))).thenReturn(mappedPage);

        Page<NotificationResponse> result = service.findInAppByRecipient(recipientId, pageable);

        assertThat(result).isSameAs(mappedPage);
        verify(notificationRepository).findByRecipientIdAndTypeOrderByCreatedAtDesc(
                recipientId, NotificationType.IN_APP, pageable);
    }

    @Test
    @DisplayName("countUnread consulta por tipo IN_APP y envuelve el conteo en UnreadCountResponse")
    void countUnread_queriesInAppAndWrapsCount() {
        UUID recipientId = UUID.randomUUID();
        when(notificationRepository.countByRecipientIdAndTypeAndReadAtIsNull(recipientId, NotificationType.IN_APP))
                .thenReturn(3L);

        UnreadCountResponse result = service.countUnread(recipientId);

        assertThat(result.count()).isEqualTo(3);
        verify(notificationRepository).countByRecipientIdAndTypeAndReadAtIsNull(
                recipientId, NotificationType.IN_APP);
    }

    @Test
    @DisplayName("markAsRead lanza NotificationNotFoundException cuando el id no existe")
    void markAsRead_throwsNotFoundWhenIdMissing() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(id, UUID.randomUUID()))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    @DisplayName("markAsRead lanza NotificationAccessDeniedException cuando el recipient no coincide")
    void markAsRead_throwsAccessDeniedWhenRecipientMismatch() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(id).recipientId(UUID.randomUUID()).build();
        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markAsRead(id, UUID.randomUUID()))
                .isInstanceOf(NotificationAccessDeniedException.class);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead es idempotente: no persiste cuando readAt ya tiene valor")
    void markAsRead_doesNotSaveWhenAlreadyRead() {
        UUID id = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(id).recipientId(recipientId).readAt(LocalDateTime.now()).build();
        NotificationResponse response = new NotificationResponse(
                id, NotificationType.IN_APP, null, "body", NotificationStatus.PENDING,
                notification.getReadAt(), null, LocalDateTime.now());
        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = service.markAsRead(id, recipientId);

        assertThat(result).isSameAs(response);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead setea readAt y persiste cuando readAt es null")
    void markAsRead_setsReadAtAndPersistsWhenNull() {
        UUID id = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(id).recipientId(recipientId).build();
        NotificationResponse response = new NotificationResponse(
                id, NotificationType.IN_APP, null, "body", NotificationStatus.PENDING,
                null, null, LocalDateTime.now());
        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        service.markAsRead(id, recipientId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("markAllAsRead retorna el int exacto que devuelve el repository")
    void markAllAsRead_returnsRepositoryCount() {
        UUID recipientId = UUID.randomUUID();
        when(notificationRepository.markAllAsRead(eq(recipientId), any(LocalDateTime.class))).thenReturn(4);

        int result = service.markAllAsRead(recipientId);

        assertThat(result).isEqualTo(4);
        verify(notificationRepository).markAllAsRead(eq(recipientId), any(LocalDateTime.class));
    }
}