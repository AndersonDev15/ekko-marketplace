package com.ekko.notification_service.channel;

import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InAppNotificationChannelTest {

    private final InAppNotificationChannel channel = new InAppNotificationChannel();

    @Test
    @DisplayName("send es un no-op: no lanza excepción y no necesita colaboradores")
    void send_isNoOpAndDoesNotThrow() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .body("Cuerpo")
                .build();

        assertThatCode(() -> channel.send(notification)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getType retorna NotificationType.IN_APP")
    void getType_returnsInApp() {
        assertThat(channel.getType()).isEqualTo(NotificationType.IN_APP);
    }
}