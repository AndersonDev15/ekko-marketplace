package com.ekko.identity_service.messaging;

import com.ekko.identity_service.config.RabbitMQConfig;
import com.ekko.identity_service.entity.OutboxEvent;
import com.ekko.identity_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private OutboxPoller outboxPoller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        outboxPoller = new OutboxPoller(outboxEventRepository, rabbitTemplate, objectMapper);
    }

    @Test
    void publishOutboxEvents_shouldPublishAndMarkEvents() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event1 = OutboxEvent.create(aggregateId, "UserRegisteredEvent", "{}", "user.registered.customer");
        OutboxEvent event2 = OutboxEvent.create(aggregateId, "EmailVerifiedEvent", "{}", "user.email-verified");

        when(outboxEventRepository.findUnpublishedEvents()).thenReturn(List.of(event1, event2));

        outboxPoller.publishOutboxEvents();

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.IDENTITY_EXCHANGE), eq("user.registered.customer"), eq("{}"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.IDENTITY_EXCHANGE), eq("user.email-verified"), eq("{}"));
        verify(outboxEventRepository).save(event1);
        verify(outboxEventRepository).save(event2);
        assert event1.isPublished();
        assert event2.isPublished();
    }

    @Test
    void publishOutboxEvents_whenNoEvents_shouldDoNothing() {
        when(outboxEventRepository.findUnpublishedEvents()).thenReturn(List.of());

        outboxPoller.publishOutboxEvents();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void publishOutboxEvents_whenPublishFails_shouldNotMarkAsPublished() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create(aggregateId, "UserRegisteredEvent", "{}", "user.registered.customer");

        when(outboxEventRepository.findUnpublishedEvents()).thenReturn(List.of(event));
        doThrow(new RuntimeException("RabbitMQ connection failed")).when(rabbitTemplate)
                .convertAndSend(eq(RabbitMQConfig.IDENTITY_EXCHANGE), eq("user.registered.customer"), eq("{}"));

        outboxPoller.publishOutboxEvents();

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.IDENTITY_EXCHANGE), eq("user.registered.customer"), eq("{}"));
        verify(outboxEventRepository, never()).save(event);
        assert !event.isPublished();
    }
}