package com.ekko.seller_service.service;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.ProductDeactivatedEvent;
import com.ekko.seller_service.messaging.dto.ProductPublishedEvent;
import com.ekko.seller_service.repository.ProductEventRepository;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStatusMetricsService {

    public static final String EVENT_PUBLISHED = "PUBLISHED";
    public static final String EVENT_DEACTIVATED = "DEACTIVATED";

    private final ProductEventRepository productEventRepository;
    private final SellerRepository sellerRepository;
    private final SellerMetricsRepository metricsRepository;

    @Transactional
    public void applyPublished(ProductPublishedEvent event) {
        if (productEventRepository.insertIfAbsent(event.productId(), EVENT_PUBLISHED) == 0) {
            return;
        }
        adjustActiveProducts(event.sellerId(), 1, event.productId());
    }

    @Transactional
    public void applyDeactivated(ProductDeactivatedEvent event) {
        if (productEventRepository.insertIfAbsent(event.productId(), EVENT_DEACTIVATED) == 0) {
            return;
        }
        adjustActiveProducts(event.sellerId(), -1, event.productId());
    }

    private void adjustActiveProducts(UUID sellerKeycloakId, long delta, UUID productId) {
        sellerRepository.findByKeycloakId(sellerKeycloakId.toString())
                .ifPresentOrElse(
                        seller -> {
                            SellerMetrics metrics = metricsRepository.findBySellerId(seller.getId())
                                    .orElseGet(() -> SellerMetrics.builder().seller(seller).build());
                            metrics.setActiveProducts(Math.max(0, metrics.getActiveProducts() + delta));
                            metricsRepository.save(metrics);
                        },
                        () -> log.warn("product event for product {} references unknown seller {}; metrics skipped",
                                productId, sellerKeycloakId));
    }
}