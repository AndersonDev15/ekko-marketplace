package com.ekko.seller_service.service;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.seller_service.repository.OrderConfirmationRepository;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConfirmedMetricsService {

    private final OrderConfirmationRepository confirmationRepository;
    private final SellerRepository sellerRepository;
    private final SellerMetricsRepository metricsRepository;

    /**
     * Applies the metrics of a confirmed order: for every distinct seller present
     * in the order, totalSales is incremented by 1 and totalRevenue by the sum of
     * that seller's item subtotals. The order is marked as processed atomically,
     * so redeliveries do not double-count.
     */
    @Transactional
    public void applyMetrics(OrderConfirmedEvent event) {
        if (confirmationRepository.insertIfAbsent(event.orderId()) == 0) {
            return;
        }

        Map<UUID, BigDecimal> revenueBySeller = new HashMap<>();
        for (OrderConfirmedEvent.OrderItemConfirmed item : event.items()) {
            revenueBySeller.merge(item.sellerKeycloakId(), item.subtotal(), BigDecimal::add);
        }

        revenueBySeller.forEach((sellerKeycloakId, revenue) ->
                sellerRepository.findByKeycloakId(sellerKeycloakId.toString())
                        .ifPresentOrElse(
                                seller -> updateMetrics(seller, revenue),
                                () -> log.warn("order.confirmed {} references unknown seller {}; metrics skipped",
                                        event.orderId(), sellerKeycloakId)));
    }

    private void updateMetrics(Seller seller, BigDecimal revenue) {
        SellerMetrics metrics = metricsRepository.findBySellerId(seller.getId())
                .orElseGet(() -> SellerMetrics.builder().seller(seller).build());
        metrics.setTotalSales(metrics.getTotalSales() + 1);
        metrics.setTotalRevenue(metrics.getTotalRevenue().add(revenue));
        metricsRepository.save(metrics);
    }
}