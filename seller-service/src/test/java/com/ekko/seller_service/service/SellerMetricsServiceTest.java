package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.response.SellerMetricsResponse;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.exception.SellerMetricsNotFoundException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerMetricsServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();

    @Mock
    private SellerMetricsRepository metricsRepository;

    @Mock
    private SellerMapper sellerMapper;

    @InjectMocks
    private SellerMetricsService sellerMetricsService;

    @Test
    void getMyMetrics_conMetricas_devuelveResponse() {
        SellerMetrics metrics = SellerMetrics.builder()
                .totalSales(5L)
                .totalRevenue(BigDecimal.valueOf(120.50))
                .averageRating(BigDecimal.valueOf(4.5))
                .totalReviews(10L)
                .activeProducts(3L)
                .build();
        SellerMetricsResponse expected = new SellerMetricsResponse(
                5L, BigDecimal.valueOf(120.50), BigDecimal.valueOf(4.5), 10L, 3L, null);
        when(metricsRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.of(metrics));
        when(sellerMapper.toMetricsResponse(metrics)).thenReturn(expected);

        SellerMetricsResponse result = sellerMetricsService.getMyMetrics(SELLER_ID);

        assertEquals(expected, result);
    }

    @Test
    void getMyMetrics_sinMetricas_lanzaMetricsNotFound() {
        when(metricsRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.empty());

        assertThrows(SellerMetricsNotFoundException.class,
                () -> sellerMetricsService.getMyMetrics(SELLER_ID));
    }
}
