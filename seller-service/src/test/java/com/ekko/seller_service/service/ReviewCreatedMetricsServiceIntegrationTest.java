package com.ekko.seller_service.service;

import com.ekko.seller_service.AbstractPostgresIntegrationTest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.ReviewCreatedEvent;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewCreatedMetricsServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ReviewCreatedMetricsService metricsService;

    @Autowired
    private SellerMetricsRepository metricsRepository;

    @Test
    void applyMetrics_incrementaReseniasYRecalculaPromedio() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());
        setInitialReviews(seller.getId(), 4L, new BigDecimal("4.00"));

        metricsService.applyMetrics(event(sellerKeycloakId, 5));

        assertMetrics(seller.getId(), 5L, "4.20");
    }

    @Test
    void applyMetrics_mismaReviewRedelivered_noVuelveAContar() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());
        setInitialReviews(seller.getId(), 1L, new BigDecimal("5.00"));

        ReviewCreatedEvent event = event(sellerKeycloakId, 3);
        metricsService.applyMetrics(event);
        metricsService.applyMetrics(event);

        assertMetrics(seller.getId(), 2L, "4.00");
    }

    @Test
    void applyMetrics_sinFilaDeMetricas_creaConPromedioDelRating() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());

        metricsService.applyMetrics(event(sellerKeycloakId, 4));

        assertMetrics(seller.getId(), 1L, "4.00");
    }

    @Test
    void applyMetrics_vendedorInexistente_ignora() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());
        setInitialReviews(seller.getId(), 2L, new BigDecimal("3.00"));

        metricsService.applyMetrics(event(UUID.randomUUID(), 5));

        assertMetrics(seller.getId(), 2L, "3.00");
    }

    private void setInitialReviews(UUID sellerId, long reviews, BigDecimal average) {
        jdbcTemplate.update("""
                UPDATE seller_metrics
                SET total_reviews = ?, average_rating = ?
                WHERE seller_id = ?
                """, reviews, average, sellerId);
    }

    private void assertMetrics(UUID sellerId, long expectedReviews, String expectedAverage) {
        SellerMetrics metrics = metricsRepository.findBySellerId(sellerId).orElseThrow();
        assertThat(metrics.getTotalReviews()).isEqualTo(expectedReviews);
        assertThat(metrics.getAverageRating()).isEqualByComparingTo(new BigDecimal(expectedAverage));
    }

    private static ReviewCreatedEvent event(UUID sellerKeycloakId, int rating) {
        return new ReviewCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                sellerKeycloakId, "customer-1", rating, "Titulo", "Comentario", null);
    }
}