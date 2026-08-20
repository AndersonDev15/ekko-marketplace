package com.ekko.notification_service.channel;

import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationDispatchException;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationChannel implements NotificationChannel {

    @Override
    public NotificationType getType() {
        return NotificationType.IN_APP;
    }

    /**
     * Las notificaciones in-app no requieren ningun despacho externo: el orquestador
     * ya persiste la notificacion como PENDING y el destinatario la consume desde el
     * propio servicio (listado, conteo de no leidas y marcado como leida). Por eso el
     * metodo no hace nada: no existe un canal de salida que invocar.
     */
    @Override
    public void send(Notification notification) throws NotificationDispatchException {
        // No-op: ver Javadoc.
    }
}