package com.ekko.product_service.repository;

import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.SELLER_SLUG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ProductRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private final UUID otherSellerId = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    // ---------------------------------------------------- existsBySellerKeycloakIdAndSlug

    @Test
    void existsBySellerKeycloakIdAndSlug_retornaTrueCuandoElSlugExisteParaElVendedor() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG, ProductStatus.DRAFT, null);

        boolean exists = productRepository.existsBySellerKeycloakIdAndSlug(
                SELLER_KEYCLOAK_ID, PRODUCT_SLUG);

        assertTrue(exists);
    }

    @Test
    void existsBySellerKeycloakIdAndSlug_retornaFalseCuandoElSlugNoExiste() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG, ProductStatus.DRAFT, null);

        boolean exists = productRepository.existsBySellerKeycloakIdAndSlug(
                SELLER_KEYCLOAK_ID, "no-existe");

        assertFalse(exists);
    }

    @Test
    void existsBySellerKeycloakIdAndSlug_retornaFalseCuandoElSlugPerteneceAOtroVendedor() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG, ProductStatus.DRAFT, null);
        persistProduct(otherSellerId, "other-store", "galaxy-s25", ProductStatus.DRAFT, null);

        assertTrue(productRepository.existsBySellerKeycloakIdAndSlug(otherSellerId, "galaxy-s25"));
        assertFalse(productRepository.existsBySellerKeycloakIdAndSlug(SELLER_KEYCLOAK_ID, "galaxy-s25"));
    }

    // -------------------------------------------- findBySellerKeycloakIdAndDeletedAtIsNull

    @Test
    void findBySellerKeycloakIdAndDeletedAtIsNull_retornaSoloProductosNoEliminadosDelVendedor() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, "iphone-16", ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, "iphone-15", ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, "iphone-se", ProductStatus.DRAFT, LocalDateTime.now());
        persistProduct(otherSellerId, "other-store", "galaxy-s25", ProductStatus.DRAFT, null);

        Page<Product> result = productRepository.findBySellerKeycloakIdAndDeletedAtIsNull(
                SELLER_KEYCLOAK_ID, PageRequest.of(0, 20));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .noneMatch(product -> product.getDeletedAt() != null));
        assertTrue(result.getContent().stream()
                .allMatch(product -> SELLER_KEYCLOAK_ID.equals(product.getSellerKeycloakId())));
    }

    @Test
    void findBySellerKeycloakIdAndDeletedAtIsNull_noRetornaProductosEliminados() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG, ProductStatus.DRAFT,
                LocalDateTime.now());

        Page<Product> result = productRepository.findBySellerKeycloakIdAndDeletedAtIsNull(
                SELLER_KEYCLOAK_ID, PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findBySellerKeycloakIdAndDeletedAtIsNull_respetaLaPaginacion() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, "iphone-16", ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, "iphone-15", ProductStatus.DRAFT, null);

        Page<Product> firstPage = productRepository.findBySellerKeycloakIdAndDeletedAtIsNull(
                SELLER_KEYCLOAK_ID, PageRequest.of(0, 1));
        Page<Product> secondPage = productRepository.findBySellerKeycloakIdAndDeletedAtIsNull(
                SELLER_KEYCLOAK_ID, PageRequest.of(1, 1));

        assertEquals(1, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
        assertEquals(2, firstPage.getTotalElements());

        Set<UUID> ids = Stream.concat(
                        firstPage.getContent().stream(),
                        secondPage.getContent().stream())
                .map(Product::getId)
                .collect(Collectors.toSet());
        assertEquals(2, ids.size());
    }

    @Test
    void findBySellerKeycloakIdAndDeletedAtIsNull_retornaPaginaVaciaCuandoNoHayProductos() {
        Page<Product> result = productRepository.findBySellerKeycloakIdAndDeletedAtIsNull(
                UUID.randomUUID(), PageRequest.of(0, 20));

        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
    }

    // --------------------------------- findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull

    @Test
    void findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull_retornaElProductoCuandoExiste() {
        Product saved = persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG,
                ProductStatus.ACTIVE, null);

        Optional<Product> result = productRepository
                .findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                        SELLER_SLUG, PRODUCT_SLUG, ProductStatus.ACTIVE);

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull_noRetornaProductosEliminados() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG, ProductStatus.ACTIVE,
                LocalDateTime.now());

        Optional<Product> result = productRepository
                .findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                        SELLER_SLUG, PRODUCT_SLUG, ProductStatus.ACTIVE);

        assertTrue(result.isEmpty());
    }

    @Test
    void findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull_noRetornaProductosConOtroEstado() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG, ProductStatus.DRAFT, null);

        Optional<Product> result = productRepository
                .findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                        SELLER_SLUG, PRODUCT_SLUG, ProductStatus.ACTIVE);

        assertTrue(result.isEmpty());
    }

    @Test
    void findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull_retornaVacioCuandoNoExisteElSlug() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG, ProductStatus.ACTIVE, null);

        Optional<Product> result = productRepository
                .findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                        SELLER_SLUG, "no-existe", ProductStatus.ACTIVE);

        assertTrue(result.isEmpty());
    }

    @Test
    void findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull_retornaVacioCuandoElSellerNoCoincide() {
        persistProduct(SELLER_KEYCLOAK_ID, SELLER_SLUG, PRODUCT_SLUG, ProductStatus.ACTIVE, null);

        Optional<Product> result = productRepository
                .findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                        "otra-tienda", PRODUCT_SLUG, ProductStatus.ACTIVE);

        assertTrue(result.isEmpty());
    }

    // ------------------------------------------------------------------- helpers

    private Product persistProduct(UUID sellerKeycloakId, String sellerSlug, String slug,
                                   ProductStatus status, LocalDateTime deletedAt) {
        Category category = CategoryTestDataBuilder.aCategory()
                .withId(null)
                .withSlug("category-" + UUID.randomUUID())
                .build();
        category = categoryRepository.save(category);

        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(null)
                .withCategory(category)
                .withSellerKeycloakId(sellerKeycloakId)
                .withSellerSlug(sellerSlug)
                .withSlug(slug)
                .withStatus(status)
                .withDeletedAt(deletedAt)
                .build();
        return productRepository.save(product);
    }
}