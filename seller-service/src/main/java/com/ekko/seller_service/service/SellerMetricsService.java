package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.response.SellerMetricsResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.exception.SellerMetricsNotFoundException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerMetricsService {

    private final SellerMetricsRepository metricsRepository;
    private final SellerMapper sellerMapper;

    @Transactional(readOnly = true)
    public SellerMetricsResponse getMyMetrics(UUID sellerId) {
        SellerMetrics metrics = metricsRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new SellerMetricsNotFoundException(sellerId));

        return sellerMapper.toMetricsResponse(metrics);
    }
}