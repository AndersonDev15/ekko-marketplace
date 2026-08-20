package com.ekko.seller_service.service;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.consume.ProductDeactivatedEvent;
import com.ekko.seller_service.messaging.dto.consume.ProductPublishedEvent;
import com.ekko.seller_service.repository.ProductEventRepository;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStatusMetricsServiceTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SELLER_KEYCLOAK_ID = UUID.randomUUID();

    @Mock
    private ProductEventRepository productEventRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerMetricsRepository metricsRepository;

    @InjectMocks
    private ProductStatusMetricsService metricsService;

    @Test
    void applyPublished_incrementaProductosActivos() {
        Seller seller = seller();
        SellerMetrics metrics = SellerMetrics.builder().activeProducts(2L).build();
        when(productEventRepository.insertIfAbsent(PRODUCT_ID, ProductStatusMetricsService.EVENT_PUBLISHED)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.of(seller));
        when(metricsRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(metrics));

        metricsService.applyPublished(publishedEvent());

        assertEquals(3L, metrics.getActiveProducts());
        verify(metricsRepository).save(metrics);
    }

    @Test
    void applyDeactivated_decrementaProductosActivos() {
        Seller seller = seller();
        SellerMetrics metrics = SellerMetrics.builder().activeProducts(5L).build();
        when(productEventRepository.insertIfAbsent(PRODUCT_ID, ProductStatusMetricsService.EVENT_DEACTIVATED)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.of(seller));
        when(metricsRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(metrics));

        metricsService.applyDeactivated(deactivatedEvent());

        assertEquals(4L, metrics.getActiveProducts());
        verify(metricsRepository).save(metrics);
    }

    @Test
    void applyDeactivated_nuncaBajaDeCero() {
        Seller seller = seller();
        SellerMetrics metrics = SellerMetrics.builder().activeProducts(0L).build();
        when(productEventRepository.insertIfAbsent(PRODUCT_ID, ProductStatusMetricsService.EVENT_DEACTIVATED)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.of(seller));
        when(metricsRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(metrics));

        metricsService.applyDeactivated(deactivatedEvent());

        assertEquals(0L, metrics.getActiveProducts());
    }

    @Test
    void applyPublished_eventoYaProcesado_noVuelveAContar() {
        when(productEventRepository.insertIfAbsent(PRODUCT_ID, ProductStatusMetricsService.EVENT_PUBLISHED)).thenReturn(0);

        metricsService.applyPublished(publishedEvent());

        verifyNoInteractions(sellerRepository, metricsRepository);
    }

    @Test
    void applyDeactivated_vendedorInexistente_ignora() {
        when(productEventRepository.insertIfAbsent(PRODUCT_ID, ProductStatusMetricsService.EVENT_DEACTIVATED)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.empty());

        metricsService.applyDeactivated(deactivatedEvent());

        verify(metricsRepository, never()).save(any());
    }

    @Test
    void applyPublished_sinMetricasPrevias_creaConUnProductoActivo() {
        Seller seller = seller();
        when(productEventRepository.insertIfAbsent(PRODUCT_ID, ProductStatusMetricsService.EVENT_PUBLISHED)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.of(seller));
        when(metricsRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        metricsService.applyPublished(publishedEvent());

        verify(metricsRepository).save(argThat(m -> m.getActiveProducts() == 1L));
    }

    @Test
    void applyDeactivated_sinMetricasPrevias_creaEnCero() {
        Seller seller = seller();
        when(productEventRepository.insertIfAbsent(PRODUCT_ID, ProductStatusMetricsService.EVENT_DEACTIVATED)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.of(seller));
        when(metricsRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        metricsService.applyDeactivated(deactivatedEvent());

        verify(metricsRepository).save(argThat(m -> m.getActiveProducts() == 0L));
    }

    private static Seller seller() {
        return Seller.builder()
                .id(UUID.randomUUID())
                .keycloakId(SELLER_KEYCLOAK_ID.toString())
                .build();
    }

    private static ProductPublishedEvent publishedEvent() {
        return new ProductPublishedEvent(PRODUCT_ID, SELLER_KEYCLOAK_ID, "Producto", "Electronica", null);
    }

    private static ProductDeactivatedEvent deactivatedEvent() {
        return new ProductDeactivatedEvent(PRODUCT_ID, SELLER_KEYCLOAK_ID, "ACTIVE", null);
    }
}