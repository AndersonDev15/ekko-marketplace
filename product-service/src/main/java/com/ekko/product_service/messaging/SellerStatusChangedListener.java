package com.ekko.product_service.messaging;

import com.ekko.product_service.config.RabbitMQConfig;
import com.ekko.product_service.entity.SellerStatusView;
import com.ekko.product_service.enums.SellerStatus;
import com.ekko.product_service.messaging.dto.consume.SellerStatusChangedEvent;
import com.ekko.product_service.repository.SellerStatusViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SellerStatusChangedListener {

    private final SellerStatusViewRepository repository;

    @RabbitListener(queues = RabbitMQConfig.SELLER_STATUS_CHANGED_QUEUE)
    public void handle(SellerStatusChangedEvent event) {
        SellerStatusView view = repository.findById(UUID.fromString(event.keycloakId()))
                .orElse(new SellerStatusView());

        view.setSellerKeycloakId(UUID.fromString(event.keycloakId()));
        view.setStatus(event.newStatus());
        view.setUpdatedAt(event.changedAt());

        repository.save(view);
    }


}