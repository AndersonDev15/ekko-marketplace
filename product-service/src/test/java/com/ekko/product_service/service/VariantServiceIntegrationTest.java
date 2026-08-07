package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.request.CreateVariantAttributeRequest;
import com.ekko.product_service.dto.request.CreateVariantRequest;
import com.ekko.product_service.dto.request.UpdateVariantRequest;
import com.ekko.product_service.dto.response.VariantResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.entity.ProductVariantAttribute;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.DuplicateSkuException;
import com.ekko.product_service.exception.InvalidDiscountPriceException;
import com.ekko.product_service.exception.LastActiveVariantException;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.InventoryRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_NAME;
import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_VALUE;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.VARIANT_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class VariantServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private VariantService variantService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private final UUID sellerId = SELLER_KEYCLOAK_ID;

    // ---------------------------------------------------------------- createVariant

    @Test
    void createVariant_creaLaVarianteCorrectamente() {
        Product product = persistProduct();
        CreateVariantRequest request = createRequest(uniqueSku(), null, null, null);

        VariantResponse response = variantService.createVariant(product.getId(), request, sellerId);

        ProductVariant saved = variantRepository.findById(response.id()).orElseThrow();
        assertEquals(request.sku(), saved.getSku());
        assertEquals(VARIANT_PRICE, saved.getPrice());
        assertEquals("USD", saved.getCurrency());
        assertEquals(true, saved.getIsActive());
        assertEquals(product.getId(), saved.getProduct().getId());
    }

    @Test
    void createVariant_persisteLosAtributos() {
        Product product = persistProduct();
        CreateVariantAttributeRequest attributeRequest =
                new CreateVariantAttributeRequest(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        CreateVariantRequest request = createRequest(uniqueSku(), null, null,
                List.of(attributeRequest));

        VariantResponse response = variantService.createVariant(product.getId(), request, sellerId);

        ProductVariant saved = variantRepository.findById(response.id()).orElseThrow();
        assertEquals(1, saved.getAttributes().size());
        ProductVariantAttribute attribute = saved.getAttributes().get(0);
        assertEquals(ATTRIBUTE_NAME, attribute.getName());
        assertEquals(ATTRIBUTE_VALUE, attribute.getValue());
        assertEquals(response.id(), attribute.getVariant().getId());
    }

    @Test
    void createVariant_creaElInventario() {
        Product product = persistProduct();
        CreateVariantRequest request = createRequest(uniqueSku(), null, null, null);
        VariantResponse response = variantService.createVariant(product.getId(), request, sellerId);

        Inventory inventory = inventoryRepository.findByVariantId(response.id()).orElseThrow();
        assertNotNull(inventory);
        assertEquals(response.id(), inventory.getVariant().getId());
    }

    @Test
    void createVariant_creaInventarioConValoresPorDefecto() {
        Product product = persistProduct();
        CreateVariantRequest request = createRequest(uniqueSku(), null, null, null);
        VariantResponse response = variantService.createVariant(product.getId(), request, sellerId);

        Inventory inventory = inventoryRepository.findByVariantId(response.id()).orElseThrow();
        assertEquals(0L, inventory.getStockAvailable());
        assertEquals(0L, inventory.getStockReserved());
        assertEquals(5L, inventory.getStockMinimum());
    }

    @Test
    void createVariant_generaSkuAutomaticamenteCuandoNoSeEnvia() {
        Product product = persistProduct();
        CreateVariantRequest request = createRequest(null, null, null, null);

        VariantResponse response = variantService.createVariant(product.getId(), request, sellerId);

        ProductVariant saved = variantRepository.findById(response.id()).orElseThrow();
        assertNotNull(saved.getSku());
        assertTrue(saved.getSku().startsWith("EKKO-"));
        assertTrue(saved.getSku().matches("EKKO-[0-9A-F]{8}"));
    }

    @Test
    void createVariant_usaElSkuEnviado() {
        Product product = persistProduct();
        String sku = uniqueSku();
        CreateVariantRequest request = createRequest(sku, null, null, null);

        VariantResponse response = variantService.createVariant(product.getId(), request, sellerId);

        assertEquals(sku, variantRepository.findById(response.id()).orElseThrow().getSku());
    }

    @Test
    void createVariant_usaUSDPorDefecto() {
        Product product = persistProduct();
        CreateVariantRequest request = createRequest(uniqueSku(), null, null, null);

        VariantResponse response = variantService.createVariant(product.getId(), request, sellerId);

        assertEquals("USD", variantRepository.findById(response.id()).orElseThrow().getCurrency());
    }

    @Test
    void createVariant_lanzaDuplicateSkuExceptionParaSkuDuplicado() {
        Product product = persistProduct();
        String sku = uniqueSku();
        variantService.createVariant(product.getId(), createRequest(sku, null, null, null), sellerId);

        assertThrows(DuplicateSkuException.class,
                () -> variantService.createVariant(
                        product.getId(), createRequest(sku, null, null, null), sellerId));
    }

    @Test
    void createVariant_lanzaInvalidDiscountPriceExceptionParaDescuentoInvalido() {
        Product product = persistProduct();
        BigDecimal price = new BigDecimal("100.00");
        CreateVariantRequest request = createRequest(uniqueSku(), price,
                new BigDecimal("100.00"), null);

        assertThrows(InvalidDiscountPriceException.class,
                () -> variantService.createVariant(product.getId(), request, sellerId));
    }

    // ---------------------------------------------------------------- updateVariant

    @Test
    void updateVariant_actualizaElPrecio() {
        Product product = persistProduct();
        VariantResponse created = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);

        variantService.updateVariant(product.getId(), created.id(),
                new UpdateVariantRequest(null, new BigDecimal("1000.00"), null), sellerId);

        assertEquals(new BigDecimal("1000.00"),
                variantRepository.findById(created.id()).orElseThrow().getPrice());
    }

    @Test
    void updateVariant_actualizaElDescuento() {
        Product product = persistProduct();
        VariantResponse created = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);

        variantService.updateVariant(product.getId(), created.id(),
                new UpdateVariantRequest(null, null, new BigDecimal("150.00")), sellerId);

        assertEquals(new BigDecimal("150.00"),
                variantRepository.findById(created.id()).orElseThrow().getDiscountPrice());
    }

    @Test
    void updateVariant_actualizaElSku() {
        Product product = persistProduct();
        VariantResponse created = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);
        String newSku = uniqueSku();

        variantService.updateVariant(product.getId(), created.id(),
                new UpdateVariantRequest(newSku, null, null), sellerId);

        assertEquals(newSku, variantRepository.findById(created.id()).orElseThrow().getSku());
    }

    @Test
    void updateVariant_conservaLosCamposNoEnviados() {
        Product product = persistProduct();
        String originalSku = uniqueSku();
        CreateVariantRequest request = createRequest(originalSku, "EUR");
        VariantResponse created = variantService.createVariant(product.getId(), request, sellerId);

        variantService.updateVariant(product.getId(), created.id(),
                new UpdateVariantRequest(null, new BigDecimal("1000.00"), null), sellerId);

        ProductVariant saved = variantRepository.findById(created.id()).orElseThrow();
        assertEquals("EUR", saved.getCurrency());
        assertEquals(originalSku, saved.getSku());
        assertEquals(new BigDecimal("1000.00"), saved.getPrice());
    }

    @Test
    void updateVariant_lanzaDuplicateSkuException() {
        Product product = persistProduct();
        String skuA = uniqueSku();
        VariantResponse created = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);
        variantService.createVariant(product.getId(), createRequest(skuA, null, null, null), sellerId);

        assertThrows(DuplicateSkuException.class,
                () -> variantService.updateVariant(product.getId(), created.id(),
                        new UpdateVariantRequest(skuA, null, null), sellerId));
    }

    // ------------------------------------------------------------ deactivateVariant

    @Test
    void deactivateVariant_cambiaIsActiveAFalse() {
        Product product = persistProduct();
        VariantResponse v1 = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);
        variantService.createVariant(product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);

        variantService.deactivateVariant(product.getId(), v1.id(), sellerId);

        ProductVariant saved = variantRepository.findById(v1.id()).orElseThrow();
        assertEquals(false, saved.getIsActive());
    }

    @Test
    void deactivateVariant_persisteElUpdatedAt() {
        Product product = persistProduct();
        VariantResponse v1 = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);
        variantService.createVariant(product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);

        variantService.deactivateVariant(product.getId(), v1.id(), sellerId);

        ProductVariant saved = variantRepository.findById(v1.id()).orElseThrow();
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void deactivateVariant_lanzaLastActiveVariantExceptionCuandoEsLaUnicaActiva() {
        Product product = persistProduct();
        VariantResponse created = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);

        assertThrows(LastActiveVariantException.class,
                () -> variantService.deactivateVariant(product.getId(), created.id(), sellerId));
    }

    // ------------------------------------------------------------- softDeleteVariant

    @Test
    void softDeleteVariant_estableceDeletedAt() {
        Product product = persistProduct();
        VariantResponse v1 = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);
        variantService.createVariant(product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);

        variantService.softDeleteVariant(product.getId(), v1.id(), sellerId);

        ProductVariant saved = variantRepository.findById(v1.id()).orElseThrow();
        assertNotNull(saved.getDeletedAt());
    }

    @Test
    void softDeleteVariant_cambiaIsActiveAFalse() {
        Product product = persistProduct();
        VariantResponse v1 = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);
        variantService.createVariant(product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);

        variantService.softDeleteVariant(product.getId(), v1.id(), sellerId);

        assertEquals(false, variantRepository.findById(v1.id()).orElseThrow().getIsActive());
    }

    @Test
    void softDeleteVariant_lanzaLastActiveVariantExceptionCuandoEsLaUnicaActiva() {
        Product product = persistProduct();
        VariantResponse created = variantService.createVariant(
                product.getId(), createRequest(uniqueSku(), null, null, null), sellerId);

        assertThrows(LastActiveVariantException.class,
                () -> variantService.softDeleteVariant(product.getId(), created.id(), sellerId));
    }

    // ------------------------------------------------------------------- helpers

    private CreateVariantRequest createRequest(String sku, BigDecimal price,
                                               BigDecimal discountPrice, List<CreateVariantAttributeRequest> attributes) {
        return new CreateVariantRequest(sku, price != null ? price : VARIANT_PRICE, discountPrice, null, attributes);
    }

    private CreateVariantRequest createRequest(String sku, String currency) {
        return new CreateVariantRequest(sku, VARIANT_PRICE, null, currency, null);
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

    private Product persistProduct() {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(persistBrand())
                .withCategory(persistCategory())
                .withSellerKeycloakId(sellerId)
                .withName("iPhone 16")
                .withSlug("iphone-16-" + UUID.randomUUID().toString().substring(0, 8))
                .withStatus(ProductStatus.DRAFT)
                .build();
        return productRepository.save(product);
    }
}