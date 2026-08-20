package com.ekko.notification_service.channel;

import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationDispatchException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    @Override
    public void send(Notification notification) throws NotificationDispatchException {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            throw new NotificationDispatchException(
                    "Cannot send email notification: recipient email is missing for notification " + notification.getId());
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody(), true);
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            throw new NotificationDispatchException("Failed to send email notification", e);
        }
    }
}