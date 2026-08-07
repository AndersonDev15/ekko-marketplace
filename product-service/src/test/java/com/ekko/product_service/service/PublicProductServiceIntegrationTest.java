package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductImageTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.builder.ProductVariantTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.response.ProductCatalogResponse;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.dto.response.ProductVariantResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductAttribute;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductAttributeRepository;
import com.ekko.product_service.repository.ProductImageRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_NAME;
import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_VALUE;
import static com.ekko.product_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.SELLER_SLUG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class PublicProductServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PublicProductService publicProductService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductAttributeRepository productAttributeRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    // ------------------------------------------------------------- getPublicCatalog

    @Test
    void getPublicCatalog_retornaSoloProductosConEstadoActive() {
        persistProduct(SELLER_SLUG, "iPhone 16", uniqueSlug("iphone-16"),
                ProductStatus.ACTIVE, null);
        persistProduct(SELLER_SLUG, "iPhone 15", uniqueSlug("iphone-15"),
                ProductStatus.ACTIVE, null);
        persistProduct(SELLER_SLUG, "iPhone SE", uniqueSlug("iphone-se"),
                ProductStatus.DRAFT, null);
        persistProduct(SELLER_SLUG, "iPhone 14", uniqueSlug("iphone-14"),
                ProductStatus.PENDING_REVIEW, null);
        persistProduct(SELLER_SLUG, "iPhone 13", uniqueSlug("iphone-13"),
                ProductStatus.REJECTED, null);

        Page<ProductCatalogResponse> result = publicProductService.getPublicCatalog(0, 20);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .anyMatch(product -> product.name().equals("iPhone 16")));
        assertTrue(result.getContent().stream()
                .anyMatch(product -> product.name().equals("iPhone 15")));
        assertTrue(result.getContent().stream()
                .noneMatch(product -> product.name().equals("iPhone SE")));
        assertTrue(result.getContent().stream()
                .noneMatch(product -> product.name().equals("iPhone 14")));
        assertTrue(result.getContent().stream()
                .noneMatch(product -> product.name().equals("iPhone 13")));
    }

    @Test
    void getPublicCatalog_noRetornaProductosEliminados() {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE, null);
        persistProduct(SELLER_SLUG, "iPhone 15", uniqueSlug("iphone-15"),
                ProductStatus.ACTIVE, LocalDateTime.now());

        Page<ProductCatalogResponse> result = publicProductService.getPublicCatalog(0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(PRODUCT_NAME, result.getContent().get(0).name());
    }

    @Test
    void getPublicCatalog_respetaLaPaginacion() {
        for (int i = 0; i < 5; i++) {
            persistProduct(SELLER_SLUG, "iPhone " + i, uniqueSlug("iphone-" + i),
                    ProductStatus.ACTIVE, null);
        }

        Page<ProductCatalogResponse> firstPage = publicProductService.getPublicCatalog(0, 2);
        Page<ProductCatalogResponse> secondPage = publicProductService.getPublicCatalog(1, 2);
        Page<ProductCatalogResponse> thirdPage = publicProductService.getPublicCatalog(2, 2);

        assertEquals(2, firstPage.getContent().size());
        assertEquals(2, secondPage.getContent().size());
        assertEquals(1, thirdPage.getContent().size());
        assertEquals(5, firstPage.getTotalElements());
    }

    @Test
    void getPublicCatalog_retornaPaginaVaciaCuandoNoExistenProductosPublicos() {
        Page<ProductCatalogResponse> result = publicProductService.getPublicCatalog(0, 20);

        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getPublicCatalog_noRetornaProductosEnEstadoDraft() {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.DRAFT, null);

        Page<ProductCatalogResponse> result = publicProductService.getPublicCatalog(0, 20);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getPublicCatalog_noRetornaProductosEnEstadoPendingReview() {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.PENDING_REVIEW, null);

        Page<ProductCatalogResponse> result = publicProductService.getPublicCatalog(0, 20);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getPublicCatalog_noRetornaProductosEnEstadoRejected() {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.REJECTED, null);

        Page<ProductCatalogResponse> result = publicProductService.getPublicCatalog(0, 20);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    // ------------------------------------------------------------- getPublicProduct

    @Test
    void getPublicProduct_retornaElDetalleCompletoDeUnProductoActivo() {
        Product product = persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.ACTIVE, null);
        addAttribute(product, ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        ProductImage image = addImage(product, true);
        ProductVariant variant = addVariant(product, true);

        ProductDetailResponse detail =
                publicProductService.getPublicProduct(SELLER_SLUG, PRODUCT_SLUG);

        assertEquals(product.getId(), detail.id());
        assertEquals(PRODUCT_NAME, detail.name());
        assertEquals(PRODUCT_SLUG, detail.slug());
        assertEquals(ProductStatus.ACTIVE, detail.status());
        assertEquals(1, detail.attributes().size());
        assertEquals(ATTRIBUTE_NAME, detail.attributes().get(0).name());
        assertEquals(1, detail.images().size());
        ProductImageResponse imageResponse = detail.images().get(0);
        assertEquals(image.getId(), imageResponse.id());
        assertTrue(imageResponse.isPrimary());
        assertEquals(1, detail.variants().size());
        ProductVariantResponse variantResponse = detail.variants().get(0);
        assertEquals(variant.getId(), variantResponse.id());
        assertEquals(variant.getSku(), variantResponse.sku());
        assertTrue(variantResponse.isActive());
    }

    @Test
    void getPublicProduct_buscaCorrectamentePorSellerSlugYProductSlug() {
        Product product = persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.ACTIVE, null);
        persistProduct("otra-tienda", "Otro", uniqueSlug("otro"), ProductStatus.ACTIVE, null);

        ProductDetailResponse detail =
                publicProductService.getPublicProduct(SELLER_SLUG, PRODUCT_SLUG);

        assertEquals(product.getId(), detail.id());
    }

    @Test
    void getPublicProduct_noRetornaProductosEliminados() {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE,
                LocalDateTime.now());

        assertThrows(ProductNotFoundException.class,
                () -> publicProductService.getPublicProduct(SELLER_SLUG, PRODUCT_SLUG));
    }

    @Test
    void getPublicProduct_noRetornaProductosConEstadoDistintoDeActive() {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.DRAFT, null);

        assertThrows(ProductNotFoundException.class,
                () -> publicProductService.getPublicProduct(SELLER_SLUG, PRODUCT_SLUG));
    }

    @Test
    void getPublicProduct_lanzaExcepcionCuandoElProductoNoExiste() {
        assertThrows(ProductNotFoundException.class,
                () -> publicProductService.getPublicProduct(SELLER_SLUG, PRODUCT_SLUG));
    }

    @Test
    void getPublicProduct_lanzaExcepcionCuandoElSellerSlugNoCoincide() {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE, null);

        assertThrows(ProductNotFoundException.class,
                () -> publicProductService.getPublicProduct("otra-tienda", PRODUCT_SLUG));
    }

    @Test
    void getPublicProduct_lanzaExcepcionCuandoElProductSlugNoCoincide() {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE, null);

        assertThrows(ProductNotFoundException.class,
                () -> publicProductService.getPublicProduct(SELLER_SLUG, "no-existe"));
    }

    // ------------------------------------------------------------------- helpers

    private String uniqueSlug(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
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

    private Product persistProduct(String sellerSlug, String name, String slug,
                                   ProductStatus status, LocalDateTime deletedAt) {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(persistBrand())
                .withCategory(persistCategory())
                .withSellerKeycloakId(SELLER_KEYCLOAK_ID)
                .withSellerSlug(sellerSlug)
                .withName(name)
                .withSlug(slug)
                .withStatus(status)
                .withDeletedAt(deletedAt)
                .build();
        return productRepository.save(product);
    }

    private ProductAttribute addAttribute(Product product, String name, String value) {
        ProductAttribute attribute = ProductAttribute.builder()
                .product(product)
                .name(name)
                .value(value)
                .build();
        attribute = productAttributeRepository.save(attribute);
        product.getAttributes().add(attribute);
        return attribute;
    }

    private ProductImage addImage(Product product, boolean isPrimary) {
        ProductImage image = ProductImageTestDataBuilder.anImage()
                .withId(null)
                .withProduct(product)
                .withUrl("https://cdn.example.com/" + UUID.randomUUID() + ".jpg")
                .withIsPrimary(isPrimary)
                .build();
        image = productImageRepository.save(image);
        product.getImages().add(image);
        return image;
    }

    private ProductVariant addVariant(Product product, boolean isActive) {
        ProductVariant variant = ProductVariantTestDataBuilder.aVariant()
                .withId(null)
                .withProduct(product)
                .withSku("SKU-" + UUID.randomUUID().toString().substring(0, 8))
                .withIsActive(isActive)
                .build();
        variant = productVariantRepository.save(variant);
        product.getVariants().add(variant);
        return variant;
    }
}
