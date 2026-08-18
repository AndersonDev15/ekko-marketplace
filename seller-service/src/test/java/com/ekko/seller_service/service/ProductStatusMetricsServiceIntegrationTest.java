package com.ekko.seller_service.service;

import com.ekko.seller_service.AbstractPostgresIntegrationTest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.ProductDeactivatedEvent;
import com.ekko.seller_service.messaging.dto.ProductPublishedEvent;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductStatusMetricsServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProductStatusMetricsService metricsService;

    @Autowired
    private SellerMetricsRepository metricsRepository;

    @Test
    void applyPublished_incrementaProductosActivos() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());

        metricsService.applyPublished(publishedEvent(sellerKeycloakId));
        metricsService.applyPublished(publishedEvent(sellerKeycloakId));

        assertActiveProducts(seller.getId(), 2L);
    }

    @Test
    void applyDeactivated_decrementaProductosActivos() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());
        setActiveProducts(seller.getId(), 5L);

        metricsService.applyDeactivated(deactivatedEvent(sellerKeycloakId));

        assertActiveProducts(seller.getId(), 4L);
    }

    @Test
    void applyDeactivated_nuncaBajaDeCero() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());
        setActiveProducts(seller.getId(), 0L);

        metricsService.applyDeactivated(deactivatedEvent(sellerKeycloakId));

        assertActiveProducts(seller.getId(), 0L);
    }

    @Test
    void mismoProductoPublicadoYDesactivado_ambosCuentan() {
        UUID sellerKeycloakId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());

        metricsService.applyPublished(publishedEvent(productId, sellerKeycloakId));
        metricsService.applyPublished(publishedEvent(productId, sellerKeycloakId));
        metricsService.applyDeactivated(deactivatedEvent(productId, sellerKeycloakId));
        metricsService.applyDeactivated(deactivatedEvent(productId, sellerKeycloakId));

        assertActiveProducts(seller.getId(), 0L);
    }

    @Test
    void applyPublished_sinFilaDeMetricas_creaConUnProductoActivo() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());

        metricsService.applyPublished(publishedEvent(sellerKeycloakId));

        assertActiveProducts(seller.getId(), 1L);
    }

    @Test
    void applyPublished_vendedorInexistente_ignora() {
        metricsService.applyPublished(publishedEvent(UUID.randomUUID()));

        assertThat(metricsRepository.count()).isZero();
    }

    private void setActiveProducts(UUID sellerId, long activeProducts) {
        jdbcTemplate.update("UPDATE seller_metrics SET active_products = ? WHERE seller_id = ?",
                activeProducts, sellerId);
    }

    private void assertActiveProducts(UUID sellerId, long expected) {
        SellerMetrics metrics = metricsRepository.findBySellerId(sellerId).orElseThrow();
        assertThat(metrics.getActiveProducts()).isEqualTo(expected);
    }

    private static ProductPublishedEvent publishedEvent(UUID sellerKeycloakId) {
        return publishedEvent(UUID.randomUUID(), sellerKeycloakId);
    }

    private static ProductPublishedEvent publishedEvent(UUID productId, UUID sellerKeycloakId) {
        return new ProductPublishedEvent(productId, sellerKeycloakId, "Producto", "Electronica", null);
    }

    private static ProductDeactivatedEvent deactivatedEvent(UUID sellerKeycloakId) {
        return deactivatedEvent(UUID.randomUUID(), sellerKeycloakId);
    }

    private static ProductDeactivatedEvent deactivatedEvent(UUID productId, UUID sellerKeycloakId) {
        return new ProductDeactivatedEvent(productId, sellerKeycloakId, "ACTIVE", null);
    }
}