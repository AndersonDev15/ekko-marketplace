package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductImageTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.request.CreateVariantRequest;
import com.ekko.product_service.dto.request.ProductFiltersRequest;
import com.ekko.product_service.dto.response.ProductSummaryResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductSortOption;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductImageRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CatalogServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private VariantService variantService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository imageRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private EntityManager entityManager;

    private final UUID sellerId = SELLER_KEYCLOAK_ID;

    // ---------------------------------------------------------------- searchProducts

    @Test
    void searchProducts_filtraSoloActivosYConStockVendible() {
        UUID withStock = createActiveProductWithStock(new BigDecimal("100.00"));
        UUID noStock = createActiveProductWithoutStock();

        Page<ProductSummaryResponse> result =
                searchProducts(emptyFilters(), ProductSortOption.RECENT);

        assertTrue(result.getContent().stream().anyMatch(s -> s.id().equals(withStock)));
        assertTrue(result.getContent().stream().noneMatch(s -> s.id().equals(noStock)));
    }

    @Test
    void searchProducts_filtraPorPrecioMinimo() {
        UUID cheap = createActiveProductWithStock(new BigDecimal("200.00"));
        createActiveProductWithStock(new BigDecimal("500.00"));

        Page<ProductSummaryResponse> result = searchProducts(
                new ProductFiltersRequest(null, null, null, new BigDecimal("300.00"), null, null),
                ProductSortOption.RECENT);

        assertTrue(result.getContent().stream().noneMatch(s -> s.id().equals(cheap)));
    }

    @Test
    void searchProducts_precioMinimoSoloConsideraVariantesActivas() {
        UUID productId = createProductWithInactiveCheaperVariant(
                new BigDecimal("500.00"), new BigDecimal("20.00"));

        Page<ProductSummaryResponse> result = searchProducts(emptyFilters(), ProductSortOption.RECENT);

        ProductSummaryResponse summary = result.getContent().stream()
                .filter(s -> s.id().equals(productId))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("500.00"), summary.price());
    }

    @Test
    void searchProducts_filtroPorPrecioIgnoraVariantesInactivas() {
        UUID inactiveCheap = createProductWithInactiveCheaperVariant(
                new BigDecimal("200.00"), new BigDecimal("20.00"));
        UUID cheap = createActiveProductWithStock(new BigDecimal("200.00"));

        Page<ProductSummaryResponse> result = searchProducts(
                new ProductFiltersRequest(null, null, null, new BigDecimal("10.00"), new BigDecimal("50.00"), null),
                ProductSortOption.RECENT);

        assertTrue(result.getContent().stream().noneMatch(s -> s.id().equals(inactiveCheap)));
        assertTrue(result.getContent().stream().noneMatch(s -> s.id().equals(cheap)));
    }

    @Test
    void searchProducts_ordenaPorPrecioMinimoIgnoraVariantesInactivas() {
        UUID first = createProductWithInactiveCheaperVariant(
                new BigDecimal("400.00"), new BigDecimal("10.00"));
        createActiveProductWithStock(new BigDecimal("800.00"));

        Page<ProductSummaryResponse> result = searchProducts(emptyFilters(), ProductSortOption.PRICE_ASC);

        List<ProductSummaryResponse> content = result.getContent();
        assertEquals(first, content.get(0).id());
    }

    @Test
    void searchProducts_filtraPorNombre() {
        Product product = persistActiveProduct();
        product.setName("iPhone 16 Pro");
        productRepository.save(product);
        persistVariantWithStock(product, new BigDecimal("150.00"));

        Page<ProductSummaryResponse> result = searchProducts(
                new ProductFiltersRequest(null, null, null, null, null, "iphone"),
                ProductSortOption.RECENT);

        assertTrue(result.getContent().stream().anyMatch(s -> s.id().equals(product.getId())));
    }

    @Test
    void searchProducts_ordenaPorPrecioAscendente() {
        UUID cheap = createActiveProductWithStock(new BigDecimal("100.00"));
        UUID expensive = createActiveProductWithStock(new BigDecimal("900.00"));

        Page<ProductSummaryResponse> result = searchProducts(emptyFilters(), ProductSortOption.PRICE_ASC);

        List<ProductSummaryResponse> content = result.getContent();
        assertTrue(content.size() >= 2);
        assertEquals(cheap, content.get(0).id());
        assertEquals(expensive, content.get(1).id());
    }

    @Test
    void searchProducts_devuelvePrecioMinimoImagenMarcaYStock() {
        UUID productId = createActiveProductWithStock(new BigDecimal("150.00"));

        Page<ProductSummaryResponse> result = searchProducts(emptyFilters(), ProductSortOption.RECENT);

        ProductSummaryResponse summary = result.getContent().stream()
                .filter(s -> s.id().equals(productId))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("150.00"), summary.price());
        assertTrue(summary.mainImageUrl().startsWith("https://cdn.example.com/"));
        assertTrue(summary.hasStock());
    }

    @Test
    void searchProducts_filtraPorCategoriaConDescendientes() {
        Category root = persistCategory();
        Category child = persistCategory();
        child.setParent(root);
        categoryRepository.save(child);

        Product product = persistActiveProduct();
        product.setCategory(child);
        productRepository.save(product);
        persistVariantWithStock(product, new BigDecimal("10.00"));
        attachPrimaryImage(product);

        Page<ProductSummaryResponse> byRoot = searchProducts(
                new ProductFiltersRequest(root.getId(), null, null, null, null, null),
                ProductSortOption.RECENT);
        Page<ProductSummaryResponse> byChild = searchProducts(
                new ProductFiltersRequest(child.getId(), null, null, null, null, null),
                ProductSortOption.RECENT);

        assertTrue(byRoot.getContent().stream().anyMatch(s -> s.id().equals(product.getId())));
        assertTrue(byChild.getContent().stream().anyMatch(s -> s.id().equals(product.getId())));
    }

    // ---------------------------------------------------------------- helpers

    private Page<ProductSummaryResponse> searchProducts(ProductFiltersRequest filters, ProductSortOption sort) {
        entityManager.flush();
        entityManager.clear();
        return catalogService.searchProducts(filters, 0, 20, sort);
    }

    private ProductFiltersRequest emptyFilters() {
        return new ProductFiltersRequest(null, null, null, null, null, null);
    }

    private UUID createActiveProductWithStock(BigDecimal price) {
        Product product = persistActiveProduct();
        persistVariantWithStock(product, price);
        attachPrimaryImage(product);
        return product.getId();
    }

    private UUID createActiveProductWithoutStock() {
        Product product = persistActiveProduct();
        persistVariantWithoutStock(product);
        return product.getId();
    }

    private UUID createProductWithInactiveCheaperVariant(BigDecimal activePrice, BigDecimal inactivePrice) {
        Product product = persistActiveProduct();
        UUID activeId = variantService.createVariant(product.getId(),
                new CreateVariantRequest(uniqueSku(), activePrice, null, null, null), sellerId).id();
        inventoryService.adjustStock(activeId, 10, sellerId);
        UUID inactiveId = variantService.createVariant(product.getId(),
                new CreateVariantRequest(uniqueSku(), inactivePrice, null, null, null), sellerId).id();
        ProductVariant inactive = variantRepository.findById(inactiveId).orElseThrow();
        inactive.setIsActive(false);
        variantRepository.save(inactive);
        product.getVariants().add(variantRepository.findById(activeId).orElseThrow());
        product.getVariants().add(inactive);
        attachPrimaryImage(product);
        return product.getId();
    }

    private void persistVariantWithStock(Product product, BigDecimal price) {
        UUID variantId = variantService.createVariant(product.getId(),
                new CreateVariantRequest(uniqueSku(), price, null, null, null), sellerId).id();
        inventoryService.adjustStock(variantId, 10, sellerId);
        product.getVariants().add(variantRepository.findById(variantId).orElseThrow());
    }

    private void persistVariantWithoutStock(Product product) {
        com.ekko.product_service.dto.response.VariantResponse response = variantService.createVariant(
                product.getId(),
                new CreateVariantRequest(uniqueSku(), new BigDecimal("50.00"), null, null, null),
                sellerId);
        product.getVariants().add(variantRepository.findById(response.id()).orElseThrow());
    }

    private Product persistActiveProduct() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Brand brand = brandRepository.save(BrandTestDataBuilder.aBrand()
                .withId(null)
                .withName("Brand-" + suffix)
                .withSlug("brand-" + suffix)
                .build());
        Category category = persistCategory();
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withName("Phone-" + suffix)
                .withSlug("phone-" + suffix)
                .withBrand(brand)
                .withCategory(category)
                .withStatus(ProductStatus.ACTIVE)
                .withSellerKeycloakId(sellerId)
                .build();
        return productRepository.save(product);
    }

    private void attachPrimaryImage(Product product) {
        ProductImage image = ProductImageTestDataBuilder.anImage()
                .withId(null)
                .withProduct(product)
                .withUrl("https://cdn.example.com/main-" + UUID.randomUUID() + ".jpg")
                .withIsPrimary(true)
                .withSortOrder(0)
                .build();
        product.getImages().add(image);
        imageRepository.save(image);
    }

    private Category persistCategory() {
        return categoryRepository.save(CategoryTestDataBuilder.aCategory()
                .withId(null)
                .withSlug("cat-" + UUID.randomUUID())
                .build());
    }

    private String uniqueSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 14);
    }
}