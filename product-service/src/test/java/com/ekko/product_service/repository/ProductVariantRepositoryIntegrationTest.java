package com.ekko.product_service.repository;

import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.builder.ProductVariantTestDataBuilder;
import com.ekko.product_service.config.AbstractDataJpaIntegrationTest;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductVariantRepositoryIntegrationTest extends AbstractDataJpaIntegrationTest {

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // ---------------------------------------------------------------- existsBySku

    @Test
    void existsBySku_retornaTrueCuandoElSkuExiste() {
        Product product = persistProduct();
        persistVariant(product, "SKU-EXISTE", true, null);

        assertTrue(variantRepository.existsBySku("SKU-EXISTE"));
    }

    @Test
    void existsBySku_retornaFalseCuandoElSkuNoExiste() {
        persistProduct();

        assertFalse(variantRepository.existsBySku("SKU-INEXISTENTE"));
    }

    // ------------------------------------------------------------ existsBySkuAndIdNot

    @Test
    void existsBySkuAndIdNot_retornaTrueCuandoOtroSkuUsaElMismoSku() {
        Product product = persistProduct();
        ProductVariant variantA = persistVariant(product, "SKU-A", true, null);
        persistVariant(product, "SKU-B", true, null);

        assertTrue(variantRepository.existsBySkuAndIdNot("SKU-B", variantA.getId()));
    }

    @Test
    void existsBySkuAndIdNot_retornaFalseCuandoElMismoSkuPerteneceSoloAlIdDado() {
        Product product = persistProduct();
        ProductVariant variant = persistVariant(product, "SKU-A", true, null);

        assertFalse(variantRepository.existsBySkuAndIdNot("SKU-A", variant.getId()));
    }

    @Test
    void existsBySkuAndIdNot_retornaFalseCuandoElSkuNoExisteEnOtraVariante() {
        Product product = persistProduct();
        ProductVariant variant = persistVariant(product, "SKU-A", true, null);

        assertFalse(variantRepository.existsBySkuAndIdNot("SKU-INEXISTENTE", variant.getId()));
    }

    // ----------------------------- countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot

    @Test
    void countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot_cuentaVariantesActivasNoEliminadas() {
        Product product = persistProduct();
        persistVariant(product, "SKU-1", true, null);
        persistVariant(product, "SKU-2", true, null);
        ProductVariant variantC = persistVariant(product, "SKU-3", true, null);

        long count = variantRepository
                .countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(product.getId(), variantC.getId());

        assertEquals(2, count);
    }

    @Test
    void countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot_excluyeEliminadasIncluyendoLaDada() {
        Product product = persistProduct();
        persistVariant(product, "SKU-1", true, null);
        persistVariant(product, "SKU-2", true, LocalDateTime.now());
        persistVariant(product, "SKU-3", true, null);

        long count = variantRepository
                .countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(product.getId(), UUID.randomUUID());

        assertEquals(2, count);
    }

    @Test
    void countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot_noCuentaInactivas() {
        Product product = persistProduct();
        persistVariant(product, "SKU-1", true, null);
        persistVariant(product, "SKU-2", false, null);
        persistVariant(product, "SKU-3", false, null);

        long count = variantRepository
                .countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(product.getId(), UUID.randomUUID());

        assertEquals(1, count);
    }

    @Test
    void countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot_noCuentaLasInactivasNiEliminadas() {
        Product product = persistProduct();
        persistVariant(product, "SKU-1", true, null);
        persistVariant(product, "SKU-2", false, null);
        persistVariant(product, "SKU-3", true, LocalDateTime.now());
        persistVariant(product, "SKU-4", false, LocalDateTime.now());

        long count = variantRepository
                .countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(product.getId(), UUID.randomUUID());

        assertEquals(1, count);
    }

    @Test
    void countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot_retornaCeroCuandoNoHayActivas() {
        Product product = persistProduct();
        persistVariant(product, "SKU-1", false, null);

        long count = variantRepository
                .countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(product.getId(), UUID.randomUUID());

        assertEquals(0, count);
    }

    @Test
    void countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot_noCuentaVariantesDeOtroProducto() {
        Product productA = persistProduct();
        Product productB = persistProduct();
        persistVariant(productA, "SKU-A1", true, null);
        persistVariant(productB, "SKU-B1", true, null);
        persistVariant(productB, "SKU-B2", true, null);

        long count = variantRepository
                .countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(productA.getId(), UUID.randomUUID());

        assertEquals(1, count);
    }

    // ------------------------------------------------------------------- helpers

    private Product persistProduct() {
        Category category = CategoryTestDataBuilder.aCategory()
                .withId(null)
                .withSlug("category-" + UUID.randomUUID())
                .build();
        category = categoryRepository.save(category);

        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(null)
                .withCategory(category)
                .withSlug("product-" + UUID.randomUUID())
                .withStatus(ProductStatus.DRAFT)
                .build();
        return productRepository.save(product);
    }

    private ProductVariant persistVariant(Product product, String sku, Boolean isActive,
                                          LocalDateTime deletedAt) {
        ProductVariant variant = ProductVariantTestDataBuilder.aVariant()
                .withId(null)
                .withProduct(product)
                .withSku(sku)
                .withIsActive(isActive)
                .build();
        variant.setDeletedAt(deletedAt);
        return variantRepository.save(variant);
    }
}