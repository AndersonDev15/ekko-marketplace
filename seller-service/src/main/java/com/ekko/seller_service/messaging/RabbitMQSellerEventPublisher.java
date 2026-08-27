package com.ekko.seller_service.messaging;

import com.ekko.seller_service.config.RabbitMQConfig;
import com.ekko.seller_service.messaging.dto.publish.SellerCreatedEvent;
import com.ekko.seller_service.messaging.dto.publish.SellerDocumentReviewEvent;
import com.ekko.seller_service.messaging.dto.publish.SellerSlugChangedEvent;
import com.ekko.seller_service.messaging.dto.publish.SellerStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQSellerEventPublisher implements SellerEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishSellerCreated(SellerCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_CREATED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishSellerStatusChanged(SellerStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_STATUS_CHANGED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishSellerDocumentReview(SellerDocumentReviewEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_DOCUMENT_REVIEW_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishSellerSlugChanged(SellerSlugChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SELLER_EXCHANGE,
                RabbitMQConfig.SELLER_SLUG_CHANGED_ROUTING_KEY,
                event);
    }
}