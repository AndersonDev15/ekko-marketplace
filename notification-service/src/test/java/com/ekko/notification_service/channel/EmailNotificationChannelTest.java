package com.ekko.notification_service.channel;

import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationDispatchException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmailNotificationChannel(mailSender);
    }

    private Notification notification(String recipientEmail) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .recipientEmail(recipientEmail)
                .subject("Asunto")
                .body("Cuerpo")
                .build();
    }

    @Test
    @DisplayName("send lanza NotificationDispatchException cuando el recipientEmail es null")
    void send_throwsWhenRecipientEmailIsNull() {
        assertThatThrownBy(() -> channel.send(notification(null)))
                .isInstanceOf(NotificationDispatchException.class);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("send lanza NotificationDispatchException cuando el recipientEmail es blank")
    void send_throwsWhenRecipientEmailIsBlank() {
        assertThatThrownBy(() -> channel.send(notification("   ")))
                .isInstanceOf(NotificationDispatchException.class);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("send construye el MimeMessage y lo envía cuando el recipientEmail es válido")
    void send_sendsMimeMessageWhenRecipientEmailIsValid() {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        channel.send(notification("customer@ekko.test"));

        verify(mailSender).send(message);
    }

    @Test
    @DisplayName("send envuelve una MailException en NotificationDispatchException preservando la causa")
    void send_wrapsMailExceptionPreservingCause() {
        MimeMessage message = new MimeMessage((Session) null);
        MailSendException original = new MailSendException("smtp down");
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(original).when(mailSender).send(message);

        NotificationDispatchException exception = org.junit.jupiter.api.Assertions.assertThrows(
                NotificationDispatchException.class, () -> channel.send(notification("customer@ekko.test")));

        assertThat(exception.getCause()).isSameAs(original);
        assertThat(exception.getCause()).isInstanceOf(MailException.class);
    }

    @Test
    @DisplayName("getType retorna NotificationType.EMAIL")
    void getType_returnsEmail() {
        assertThat(channel.getType()).isEqualTo(NotificationType.EMAIL);
    }
}