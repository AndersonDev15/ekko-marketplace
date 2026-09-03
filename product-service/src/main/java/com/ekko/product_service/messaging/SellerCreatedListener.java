package com.ekko.product_service.messaging;

import com.ekko.product_service.config.RabbitMQConfig;
import com.ekko.product_service.entity.SellerSnapshot;
import com.ekko.product_service.entity.SellerStatusView;
import com.ekko.product_service.messaging.dto.consume.SellerCreatedEvent;
import com.ekko.product_service.repository.SellerSnapshotRepository;
import com.ekko.product_service.repository.SellerStatusViewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SellerCreatedListener {

    private final SellerSnapshotRepository sellerSnapshotRepository;
    private final SellerStatusViewRepository sellerStatusViewRepository;

    @RabbitListener(queues = RabbitMQConfig.SELLER_CREATED_QUEUE)
    @Transactional
    public void handle(SellerCreatedEvent event) {

        UUID sellerKeycloakId = UUID.fromString(event.keycloakId());

        SellerSnapshot snapshot = SellerSnapshot.builder()
                .sellerKeycloakId(sellerKeycloakId)
                .storeName(event.storeName())
                .email(event.email())
                .updatedAt(event.createdAt())
                .build();

        sellerSnapshotRepository.save(snapshot);

        SellerStatusView statusView = SellerStatusView.builder()
                .sellerKeycloakId(sellerKeycloakId)
                .status(event.status())
                .sellerSlug(event.slug())
                .updatedAt(event.createdAt())
                .build();

        sellerStatusViewRepository.save(statusView);
    }
}
