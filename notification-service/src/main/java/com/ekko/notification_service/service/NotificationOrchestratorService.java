package com.ekko.notification_service.service;

import com.ekko.notification_service.channel.NotificationChannel;
import com.ekko.notification_service.entity.Notification;
import com.ekko.notification_service.entity.NotificationTemplate;
import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationDispatchException;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestratorService {

    private final NotificationTemplateService templateService;
    private final TemplateRenderer templateRenderer;
    private final NotificationPersistenceService persistenceService;
    private final List<NotificationChannel> channels;

    private Map<NotificationType, NotificationChannel> channelMap = new HashMap<>();

    @PostConstruct
    void initChannelMap() {
        channelMap = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getType,
                        Function.identity()));
    }

    public void process(NotificationEvent event) {
        for (NotificationType channel : event.channels()) {
            if (channel == NotificationType.IN_APP && event.recipientId() == null) {
                log.warn("Skipping IN_APP channel: recipientId is null (likely guest checkout), eventType={}",
                        event.eventType());
                continue;
            }

            Optional<NotificationTemplate> templateOpt =
                    templateService.findActiveByNameAndType(event.eventType(), channel);
            if (templateOpt.isEmpty()) {
                log.warn("No active template found for eventType={} channel={}", event.eventType(), channel);
                continue;
            }

            NotificationTemplate template = templateOpt.get();
            String renderedSubject = templateRenderer.render(template.getSubject(), event.variables());
            String renderedBody = templateRenderer.render(template.getBody(), event.variables());

            Notification notification = persistenceService.persistPending(
                    event.recipientId(),
                    event.recipientEmail(),
                    channel,
                    template.getId(),
                    renderedSubject,
                    renderedBody,
                    event.variables());

            NotificationChannel notificationChannel = channelMap.get(channel);
            if (notificationChannel == null) {
                log.warn("No channel implementation registered for type={}", channel);
                continue;
            }

            try {
                notificationChannel.send(notification);
                persistenceService.persistFinalState(notification.getId(), NotificationStatus.SENT, null);
            } catch (NotificationDispatchException e) {
                log.error("Failed to dispatch notification id={} channel={}: {}",
                        notification.getId(), channel, e.getMessage(), e);
                persistenceService.persistFinalState(notification.getId(), NotificationStatus.FAILED, e.getMessage());
            }
        }
    }
}