package com.ekko.product_service.service;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.messaging.dto.consume.ReviewCreatedEvent;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ReviewEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRatingServiceTest {

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private ReviewEventRepository reviewEventRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductRatingService productRatingService;

    @Test
    void applyRating_incrementaContadorYRecalculaPromedio() {
        Product product = product(new BigDecimal("4.0"), 4);
        when(reviewEventRepository.insertIfAbsent(REVIEW_ID)).thenReturn(1);
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));

        productRatingService.applyRating(event(5));

        assertEquals(5, product.getReviewCount());
        assertEquals(new BigDecimal("4.2"), product.getAverageRating());
        verify(productRepository).save(product);
    }

    @Test
    void applyRating_primeraResenha_promedioEsElRating() {
        Product product = product(BigDecimal.ZERO, 0);
        when(reviewEventRepository.insertIfAbsent(REVIEW_ID)).thenReturn(1);
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));

        productRatingService.applyRating(event(4));

        assertEquals(1, product.getReviewCount());
        assertEquals(new BigDecimal("4.0"), product.getAverageRating());
    }

    @Test
    void applyRating_reviewYaProcesada_noActualiza() {
        when(reviewEventRepository.insertIfAbsent(REVIEW_ID)).thenReturn(0);

        productRatingService.applyRating(event(5));

        verifyNoInteractions(productRepository);
    }

    @Test
    void applyRating_productoInexistente_ignora() {
        when(reviewEventRepository.insertIfAbsent(REVIEW_ID)).thenReturn(1);
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.empty());

        productRatingService.applyRating(event(5));

        verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static Product product(BigDecimal average, int count) {
        return Product.builder()
                .id(PRODUCT_ID)
                .averageRating(average)
                .reviewCount(count)
                .build();
    }

    private static ReviewCreatedEvent event(int rating) {
        return new ReviewCreatedEvent(
                REVIEW_ID, PRODUCT_ID, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "customer-1", rating, "Titulo", "Comentario", null);
    }
}