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
import com.ekko.product_service.messaging.dto.consume.OrderCancelledEvent;
import com.ekko.product_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.InventoryRepository;
import com.ekko.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.VARIANT_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class OrderInventoryServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderInventoryService orderInventoryService;

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

    @Test
    void confirmStock_restaDisponibleYReservado() {
        UUID variantId = persistActiveVariantWithStock(10);
        inventoryService.reserveStock(variantId, 3);

        orderInventoryService.confirmStock(confirmedEvent(UUID.randomUUID(), variantId, 3));

        assertStock(variantId, 7L, 0L);
    }

    @Test
    void confirmStock_mismaOrdenRedelivered_noAjustaDosVeces() {
        UUID variantId = persistActiveVariantWithStock(10);
        inventoryService.reserveStock(variantId, 3);
        OrderConfirmedEvent event = confirmedEvent(UUID.randomUUID(), variantId, 3);

        orderInventoryService.confirmStock(event);
        orderInventoryService.confirmStock(event);

        assertStock(variantId, 7L, 0L);
    }

    @Test
    void releaseStock_liberaReserva() {
        UUID variantId = persistActiveVariantWithStock(10);
        inventoryService.reserveStock(variantId, 4);

        orderInventoryService.releaseStock(cancelledEvent(UUID.randomUUID(), variantId, 4));

        assertStock(variantId, 10L, 0L);
    }

    @Test
    void mismaOrdenConfirmadaYCancelada_ambosEventosSeProcesan() {
        UUID orderId = UUID.randomUUID();
        UUID variantA = persistActiveVariantWithStock(10);
        UUID variantB = persistActiveVariantWithStock(10);
        inventoryService.reserveStock(variantA, 3);
        inventoryService.reserveStock(variantB, 4);

        orderInventoryService.confirmStock(confirmedEvent(orderId, variantA, 3));
        orderInventoryService.releaseStock(cancelledEvent(orderId, variantB, 4));

        assertStock(variantA, 7L, 0L);
        assertStock(variantB, 10L, 0L);
    }

    private void assertStock(UUID variantId, long available, long reserved) {
        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId).orElseThrow();
        assertEquals(available, inventory.getStockAvailable());
        assertEquals(reserved, inventory.getStockReserved());
    }

    private static OrderConfirmedEvent confirmedEvent(UUID orderId, UUID variantId, int quantity) {
        return new OrderConfirmedEvent(
                orderId, "EKK-001", UUID.randomUUID(), "guest@ekko.test",
                List.of(new OrderConfirmedEvent.OrderItemConfirmed(
                        UUID.randomUUID(), UUID.randomUUID(), variantId, quantity,
                        SELLER_KEYCLOAK_ID, new BigDecimal("100.00"))),
                null);
    }

    private static OrderCancelledEvent cancelledEvent(UUID orderId, UUID variantId, int quantity) {
        return new OrderCancelledEvent(
                orderId, "EKK-001", UUID.randomUUID(), null, "CONFIRMED", true,
                List.of(new OrderCancelledEvent.OrderItemCancelled(
                        UUID.randomUUID(), UUID.randomUUID(), variantId, quantity, UUID.randomUUID())),
                null);
    }

    private UUID persistActiveVariantWithStock(long stock) {
        UUID variantId = persistActiveVariant();
        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId).orElseThrow();
        inventory.setStockAvailable(stock);
        inventoryRepository.save(inventory);
        return variantId;
    }

    private UUID persistActiveVariant() {
        Product product = persistProduct(ProductStatus.ACTIVE);
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