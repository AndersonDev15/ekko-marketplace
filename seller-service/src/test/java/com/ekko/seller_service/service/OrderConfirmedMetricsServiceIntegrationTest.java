package com.ekko.seller_service.service;

import com.ekko.seller_service.AbstractPostgresIntegrationTest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class OrderConfirmedMetricsServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderConfirmedMetricsService metricsService;

    @Autowired
    private SellerMetricsRepository metricsRepository;

    @Test
    void applyMetrics_agrupaPorVendedor_persisteVentasYRevenue() {
        UUID sellerAKeycloakId = UUID.randomUUID();
        UUID sellerBKeycloakId = UUID.randomUUID();
        Seller sellerA = insertActiveSeller(sellerAKeycloakId.toString());
        Seller sellerB = insertActiveSeller(sellerBKeycloakId.toString());
        insertSellerMetrics(sellerA.getId());
        insertSellerMetrics(sellerB.getId());

        metricsService.applyMetrics(event(
                item(sellerAKeycloakId, "100.00"),
                item(sellerAKeycloakId, "50.00"),
                item(sellerBKeycloakId, "25.00")));

        assertMetrics(sellerA.getId(), 1L, "150.00");
        assertMetrics(sellerB.getId(), 1L, "25.00");
    }

    @Test
    void applyMetrics_mismaOrdenRedelivered_noVuelveAContar() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());

        OrderConfirmedEvent event = event(item(sellerKeycloakId, "100.00"));
        metricsService.applyMetrics(event);
        metricsService.applyMetrics(event);

        assertMetrics(seller.getId(), 1L, "100.00");
    }

    @Test
    void applyMetrics_sinFilaDeMetricas_creaMetricasConValores() {
        UUID sellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(sellerKeycloakId.toString());

        metricsService.applyMetrics(event(item(sellerKeycloakId, "40.00")));

        assertMetrics(seller.getId(), 1L, "40.00");
    }

    @Test
    void applyMetrics_vendedorInexistente_ignoraEseVendedor() {
        UUID knownSellerKeycloakId = UUID.randomUUID();
        Seller seller = insertActiveSeller(knownSellerKeycloakId.toString());
        insertSellerMetrics(seller.getId());

        metricsService.applyMetrics(event(
                item(knownSellerKeycloakId, "30.00"),
                item(UUID.randomUUID(), "999.00")));

        assertMetrics(seller.getId(), 1L, "30.00");
    }

    private void assertMetrics(UUID sellerId, long expectedSales, String expectedRevenue) {
        SellerMetrics metrics = metricsRepository.findBySellerId(sellerId).orElseThrow();
        assertThat(metrics.getTotalSales()).isEqualTo(expectedSales);
        assertThat(metrics.getTotalRevenue()).isEqualByComparingTo(new BigDecimal(expectedRevenue));
    }

    private static OrderConfirmedEvent event(OrderConfirmedEvent.OrderItemConfirmed... items) {
        return new OrderConfirmedEvent(UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "guest@ekko.test", List.of(items), null);
    }

    private static OrderConfirmedEvent.OrderItemConfirmed item(UUID sellerKeycloakId, String subtotal) {
        return new OrderConfirmedEvent.OrderItemConfirmed(
                UUID.randomUUID(), UUID.randomUUID(), sellerKeycloakId, new BigDecimal(subtotal));
    }
}