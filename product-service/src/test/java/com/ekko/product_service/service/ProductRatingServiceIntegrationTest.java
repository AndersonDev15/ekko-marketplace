package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.messaging.dto.ReviewCreatedEvent;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class ProductRatingServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProductRatingService productRatingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void applyRating_incrementaContadorYRecalculaPromedio() {
        Product product = persistProduct(new BigDecimal("4.0"), 4);

        productRatingService.applyRating(event(product.getId(), 5));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(5, updated.getReviewCount());
        assertEquals(new BigDecimal("4.2"), updated.getAverageRating());
    }

    @Test
    void applyRating_primeraResenha_promedioEsElRating() {
        Product product = persistProduct(BigDecimal.ZERO, 0);

        productRatingService.applyRating(event(product.getId(), 4));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(1, updated.getReviewCount());
        assertEquals(new BigDecimal("4.0"), updated.getAverageRating());
    }

    @Test
    void applyRating_mismaReviewRedelivered_noCuentaDosVeces() {
        Product product = persistProduct(BigDecimal.ZERO, 0);
        ReviewCreatedEvent event = event(product.getId(), 5);

        productRatingService.applyRating(event);
        productRatingService.applyRating(event);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(1, updated.getReviewCount());
        assertEquals(new BigDecimal("5.0"), updated.getAverageRating());
    }

    @Test
    void applyRating_productoInexistente_ignora() {
        productRatingService.applyRating(event(UUID.randomUUID(), 5));

        assertEquals(0, productRepository.count());
    }

    private Product persistProduct(BigDecimal average, int count) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Brand brand = brandRepository.save(BrandTestDataBuilder.aBrand()
                .withId(null)
                .withName("Brand-" + suffix)
                .withSlug("brand-" + suffix)
                .build());
        Category category = categoryRepository.save(CategoryTestDataBuilder.aCategory()
                .withId(null)
                .withSlug("category-" + suffix)
                .build());

        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(brand)
                .withCategory(category)
                .withSlug("product-" + suffix)
                .withAverageRating(average)
                .withReviewCount(count)
                .build();
        return productRepository.save(product);
    }

    private static ReviewCreatedEvent event(UUID productId, int rating) {
        return new ReviewCreatedEvent(
                UUID.randomUUID(), productId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "customer-1", rating, "Titulo", "Comentario", null);
    }
}