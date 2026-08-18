package com.ekko.seller_service.service;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.ReviewCreatedEvent;
import com.ekko.seller_service.repository.ReviewConfirmationRepository;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewCreatedMetricsServiceTest {

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID SELLER_KEYCLOAK_ID = UUID.randomUUID();

    @Mock
    private ReviewConfirmationRepository confirmationRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerMetricsRepository metricsRepository;

    @InjectMocks
    private ReviewCreatedMetricsService metricsService;

    @Test
    void applyMetrics_incrementaReseniasYRecalculaPromedio() {
        Seller seller = seller();
        SellerMetrics metrics = SellerMetrics.builder()
                .totalReviews(4L)
                .averageRating(new BigDecimal("4.00"))
                .build();
        when(confirmationRepository.insertIfAbsent(REVIEW_ID)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.of(seller));
        when(metricsRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(metrics));

        metricsService.applyMetrics(event(5));

        assertEquals(5L, metrics.getTotalReviews());
        assertEquals(new BigDecimal("4.20"), metrics.getAverageRating());
        verify(metricsRepository).save(metrics);
    }

    @Test
    void applyMetrics_reviewYaProcesada_noVuelveAContar() {
        when(confirmationRepository.insertIfAbsent(REVIEW_ID)).thenReturn(0);

        metricsService.applyMetrics(event(5));

        verifyNoInteractions(sellerRepository, metricsRepository);
    }

    @Test
    void applyMetrics_vendedorInexistente_ignora() {
        when(confirmationRepository.insertIfAbsent(REVIEW_ID)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.empty());

        metricsService.applyMetrics(event(5));

        verify(metricsRepository, never()).save(any());
    }

    @Test
    void applyMetrics_sinMetricasPrevias_creaConPromedioDelRating() {
        Seller seller = seller();
        when(confirmationRepository.insertIfAbsent(REVIEW_ID)).thenReturn(1);
        when(sellerRepository.findByKeycloakId(SELLER_KEYCLOAK_ID.toString())).thenReturn(Optional.of(seller));
        when(metricsRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());
        when(metricsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        metricsService.applyMetrics(event(4));

        ArgumentCaptor<SellerMetrics> captor = ArgumentCaptor.forClass(SellerMetrics.class);
        verify(metricsRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getTotalReviews());
        assertEquals(new BigDecimal("4.00"), captor.getValue().getAverageRating());
    }

    private static Seller seller() {
        return Seller.builder()
                .id(UUID.randomUUID())
                .keycloakId(SELLER_KEYCLOAK_ID.toString())
                .build();
    }

    private static ReviewCreatedEvent event(int rating) {
        return new ReviewCreatedEvent(
                REVIEW_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SELLER_KEYCLOAK_ID, "customer-1", rating, "Titulo", "Comentario", null);
    }
}