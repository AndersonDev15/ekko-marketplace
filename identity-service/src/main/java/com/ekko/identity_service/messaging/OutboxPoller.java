package com.ekko.identity_service.messaging;

import com.ekko.identity_service.config.RabbitMQConfig;
import com.ekko.identity_service.entity.OutboxEvent;
import com.ekko.identity_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnpublishedEvents();
        
        for (OutboxEvent event : events) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.IDENTITY_EXCHANGE,
                        event.getRoutingKey(),
                        event.getPayload()
                );
                event.markPublished();
                outboxEventRepository.save(event);
                log.debug("Published outbox event {} with routing key {}", event.getId(), event.getRoutingKey());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}", event.getId(), e);
            }
        }
    }
}