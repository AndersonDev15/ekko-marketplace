package com.ekko.seller_service.service;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.seller_service.repository.OrderConfirmationRepository;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderConfirmedMetricsServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID SELLER_A_KEYCLOAK_ID = UUID.randomUUID();
    private static final UUID SELLER_B_KEYCLOAK_ID = UUID.randomUUID();

    @Mock
    private OrderConfirmationRepository confirmationRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerMetricsRepository metricsRepository;

    @InjectMocks
    private OrderConfirmedMetricsService metricsService;

    @Test
    void applyMetrics_agrupaPorVendedor_incrementaVentasYRevenue() {
        Seller sellerA = seller(SELLER_A_KEYCLOAK_ID);
        Seller sellerB = seller(SELLER_B_KEYCLOAK_ID);
        SellerMetrics metricsA = metrics(3L, new BigDecimal("10.00"));
        SellerMetrics metricsB = metrics(1L, new BigDecimal("5.00"));
        when(confirmationRepository.insertIfAbsent(ORDER_ID)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_A_KEYCLOAK_ID.toString())).thenReturn(Optional.of(sellerA));
        when(sellerRepository.findByKeycloakId(SELLER_B_KEYCLOAK_ID.toString())).thenReturn(Optional.of(sellerB));
        when(metricsRepository.findBySellerId(sellerA.getId())).thenReturn(Optional.of(metricsA));
        when(metricsRepository.findBySellerId(sellerB.getId())).thenReturn(Optional.of(metricsB));

        metricsService.applyMetrics(event(
                item(SELLER_A_KEYCLOAK_ID, "100.00"),
                item(SELLER_A_KEYCLOAK_ID, "50.00"),
                item(SELLER_B_KEYCLOAK_ID, "25.00")));

        assertEquals(4L, metricsA.getTotalSales());
        assertEquals(new BigDecimal("160.00"), metricsA.getTotalRevenue());
        assertEquals(2L, metricsB.getTotalSales());
        assertEquals(new BigDecimal("30.00"), metricsB.getTotalRevenue());
        verify(metricsRepository).save(metricsA);
        verify(metricsRepository).save(metricsB);
    }

    @Test
    void applyMetrics_ordenYaProcesada_noVuelveAContar() {
        when(confirmationRepository.insertIfAbsent(ORDER_ID)).thenReturn(0);

        metricsService.applyMetrics(event(item(SELLER_A_KEYCLOAK_ID, "100.00")));

        verifyNoInteractions(sellerRepository, metricsRepository);
    }

    @Test
    void applyMetrics_vendedorInexistente_ignoraEseVendedor() {
        when(confirmationRepository.insertIfAbsent(ORDER_ID)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_A_KEYCLOAK_ID.toString())).thenReturn(Optional.empty());

        metricsService.applyMetrics(event(item(SELLER_A_KEYCLOAK_ID, "100.00")));

        verify(metricsRepository, never()).save(any());
    }

    @Test
    void applyMetrics_sinMetricasPrevias_creaYPersiste() {
        Seller sellerA = seller(SELLER_A_KEYCLOAK_ID);
        when(confirmationRepository.insertIfAbsent(ORDER_ID)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_A_KEYCLOAK_ID.toString())).thenReturn(Optional.of(sellerA));
        when(metricsRepository.findBySellerId(sellerA.getId())).thenReturn(Optional.empty());
        when(metricsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        metricsService.applyMetrics(event(item(SELLER_A_KEYCLOAK_ID, "25.00")));

        ArgumentCaptor<SellerMetrics> captor = ArgumentCaptor.forClass(SellerMetrics.class);
        verify(metricsRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getTotalSales());
        assertEquals(new BigDecimal("25.00"), captor.getValue().getTotalRevenue());
        assertEquals(sellerA.getId(), captor.getValue().getSeller().getId());
    }

    private static Seller seller(UUID keycloakId) {
        return Seller.builder()
                .id(UUID.randomUUID())
                .keycloakId(keycloakId.toString())
                .build();
    }

    private static SellerMetrics metrics(Long sales, BigDecimal revenue) {
        return SellerMetrics.builder()
                .totalSales(sales)
                .totalRevenue(revenue)
                .build();
    }

    private static OrderConfirmedEvent event(OrderConfirmedEvent.OrderItemConfirmed... items) {
        return new OrderConfirmedEvent(ORDER_ID, "ORD-001", UUID.randomUUID(), "guest@ekko.test", List.of(items), null);
    }

    private static OrderConfirmedEvent.OrderItemConfirmed item(UUID sellerKeycloakId, String subtotal) {
        return new OrderConfirmedEvent.OrderItemConfirmed(
                UUID.randomUUID(), UUID.randomUUID(), sellerKeycloakId, new BigDecimal(subtotal));
    }
}