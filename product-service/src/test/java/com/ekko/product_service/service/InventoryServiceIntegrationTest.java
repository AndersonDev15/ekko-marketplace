package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.request.CreateVariantRequest;
import com.ekko.product_service.dto.response.VariantResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.InsufficientStockException;
import com.ekko.product_service.exception.InvalidStockOperationException;
import com.ekko.product_service.exception.ProductNotAvailableException;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.InventoryRepository;
import com.ekko.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.VARIANT_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class InventoryServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private VariantService variantService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private final UUID sellerId = SELLER_KEYCLOAK_ID;

    // ------------------------------------------------------------- reserveStock

    @Test
    void reserveStock_incrementaStockReservado() {
        UUID variantId = persistActiveVariantWithStock(10);

        inventoryService.reserveStock(variantId, 2);

        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId).orElseThrow();
        assertEquals(2L, inventory.getStockReserved());
    }

    @Test
    void reserveStock_productoDraftLanzaNotAvailable() {
        UUID variantId = persistVariantWithStatus(ProductStatus.DRAFT);

        assertThrows(ProductNotAvailableException.class,
                () -> inventoryService.reserveStock(variantId, 1));
    }

    @Test
    void reserveStock_stockInsuficienteLanzaInsufficient() {
        UUID variantId = persistActiveVariantWithStock(3);

        assertThrows(InsufficientStockException.class,
                () -> inventoryService.reserveStock(variantId, 10));
    }

    // ------------------------------------------------------------- confirmStock

    @Test
    void confirmStock_restaDisponibleYReservado() {
        UUID variantId = persistActiveVariantWithStock(10);
        inventoryService.reserveStock(variantId, 3);

        inventoryService.confirmStock(variantId, 3);

        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId).orElseThrow();
        assertEquals(7L, inventory.getStockAvailable());
        assertEquals(0L, inventory.getStockReserved());
    }

    @Test
    void confirmStock_cantidadMayorQueReservadaLanza() {
        UUID variantId = persistActiveVariantWithStock(10);

        assertThrows(InvalidStockOperationException.class,
                () -> inventoryService.confirmStock(variantId, 5));
    }

    // ------------------------------------------------------------- releaseStock

    @Test
    void releaseStock_liberaReserva() {
        UUID variantId = persistActiveVariantWithStock(10);
        inventoryService.reserveStock(variantId, 4);

        inventoryService.releaseStock(variantId, 4);

        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId).orElseThrow();
        assertEquals(0L, inventory.getStockReserved());
        assertEquals(10L, inventory.getStockAvailable());
    }

    @Test
    void releaseStock_cantidadMayorQueReservadaLanza() {
        UUID variantId = persistActiveVariantWithStock(10);

        assertThrows(InvalidStockOperationException.class,
                () -> inventoryService.releaseStock(variantId, 3));
    }

    // ------------------------------------------------------------- adjustStock

    @Test
    void adjustStock_actualizaStockDisponible() {
        UUID variantId = persistActiveVariantWithStock(10);

        inventoryService.adjustStock(variantId, 15, sellerId);

        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId).orElseThrow();
        assertEquals(15L, inventory.getStockAvailable());
    }

    // ------------------------------------------------------------------- helpers

    private UUID persistActiveVariantWithStock(long stock) {
        UUID variantId = persistActiveVariant();
        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId).orElseThrow();
        inventory.setStockAvailable(stock);
        inventoryRepository.save(inventory);
        return variantId;
    }

    private UUID persistActiveVariant() {
        return persistVariantWithStatus(ProductStatus.ACTIVE);
    }

    private UUID persistVariantWithStatus(ProductStatus status) {
        Product product = persistProduct(status);
        VariantResponse variant = variantService.createVariant(
                product.getId(), createRequest(uniqueSku()), sellerId);
        return variant.id();
    }

    private CreateVariantRequest createRequest(String sku) {
        return new CreateVariantRequest(sku, VARIANT_PRICE, null, null, null);
    }

    private String uniqueSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private Brand persistBrand() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Brand brand = BrandTestDataBuilder.aBrand()
                .withId(null)
                .withName("Brand-" + suffix)
                .withSlug("brand-" + suffix)
                .build();
        return brandRepository.save(brand);
    }

    private Category persistCategory() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Category category = CategoryTestDataBuilder.aCategory()
                .withId(null)
                .withSlug("category-" + suffix)
                .build();
        return categoryRepository.save(category);
    }

    private Product persistProduct(ProductStatus status) {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(persistBrand())
                .withCategory(persistCategory())
                .withSellerKeycloakId(sellerId)
                .withName("iPhone 16")
                .withSlug("iphone-16-" + UUID.randomUUID().toString().substring(0, 8))
                .withStatus(status)
                .build();
        return productRepository.save(product);
    }
}