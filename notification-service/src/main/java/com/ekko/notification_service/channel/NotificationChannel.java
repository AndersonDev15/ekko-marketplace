package com.ekko.notification_service.channel;

import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationDispatchException;

public interface NotificationChannel {

    void send(Notification notification) throws NotificationDispatchException;

    NotificationType getType();
}