package com.ekko.product_service.messaging;

import com.ekko.product_service.config.RabbitMQConfig;
import com.ekko.product_service.entity.SellerStatusView;
import com.ekko.product_service.messaging.dto.consume.SellerSlugChangedEvent;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.SellerStatusViewRepository;
import com.ekko.product_service.service.SellerSlugChangedService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SellerSlugChangedListener {

    private final SellerSlugChangedService sellerSlugChangedService;

    @RabbitListener(queues = RabbitMQConfig.SELLER_SLUG_CHANGED_QUEUE)
    public void handle(SellerSlugChangedEvent event) {
        sellerSlugChangedService.updateSellerSlug(event);
    }
}
