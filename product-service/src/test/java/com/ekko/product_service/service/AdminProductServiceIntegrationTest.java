package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.response.ProductAttributeResponse;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductAttribute;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.InvalidProductStatusException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductAttributeRepository;
import com.ekko.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_NAME;
import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_VALUE;
import static com.ekko.product_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class AdminProductServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AdminProductService adminProductService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductAttributeRepository productAttributeRepository;

    // ----------------------------------------------------------------- approveProduct

    @Test
    void approveProduct_cambiaEstadoDePendingReviewAActive() {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        adminProductService.approveProduct(product.getId());

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void approveProduct_persisteElCambioEnLaBaseDeDatos() {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        adminProductService.approveProduct(product.getId());

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(PRODUCT_NAME, saved.getName());
        assertEquals(SELLER_KEYCLOAK_ID, saved.getSellerKeycloakId());
    }

    @Test
    void approveProduct_lanzaInvalidProductStatusExceptionCuandoNoEstaEnPendingReview() {
        Product product = persistProduct(ProductStatus.DRAFT);

        assertThrows(InvalidProductStatusException.class,
                () -> adminProductService.approveProduct(product.getId()));

        assertEquals(ProductStatus.DRAFT,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void approveProduct_lanzaProductNotFoundExceptionCuandoElProductoNoExiste() {
        assertThrows(ProductNotFoundException.class,
                () -> adminProductService.approveProduct(UUID.randomUUID()));
    }

    // ----------------------------------------------------------------- rejectProduct

    @Test
    void rejectProduct_cambiaEstadoDePendingReviewARejected() {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        adminProductService.rejectProduct(product.getId(), "marca prohibida");

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.REJECTED, saved.getStatus());
    }

    @Test
    void rejectProduct_persisteElCambioEnLaBaseDeDatos() {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        adminProductService.rejectProduct(product.getId(), "marca prohibida");

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.REJECTED, saved.getStatus());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(PRODUCT_NAME, saved.getName());
        assertEquals(SELLER_KEYCLOAK_ID, saved.getSellerKeycloakId());
    }

    @Test
    void rejectProduct_lanzaInvalidProductStatusExceptionCuandoNoEstaEnPendingReview() {
        Product product = persistProduct(ProductStatus.ACTIVE);

        assertThrows(InvalidProductStatusException.class,
                () -> adminProductService.rejectProduct(product.getId(), "motivo"));

        assertEquals(ProductStatus.ACTIVE,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void rejectProduct_lanzaProductNotFoundExceptionCuandoElProductoNoExiste() {
        assertThrows(ProductNotFoundException.class,
                () -> adminProductService.rejectProduct(UUID.randomUUID(), "motivo"));
    }

    // --------------------------------------------------------------- getAllProducts

    @Test
    void getAllProducts_retornaProductosPaginados() {
        persistProduct(ProductStatus.DRAFT);
        persistProduct(ProductStatus.DRAFT);
        persistProduct(ProductStatus.ACTIVE);
        persistProduct(ProductStatus.PENDING_REVIEW);

        Page<ProductResponse> result = adminProductService.getAllProducts(0, 2);

        assertEquals(2, result.getContent().size());
        assertEquals(4, result.getTotalElements());
    }

    @Test
    void getAllProducts_respetaElTamanoYNumeroDePagina() {
        for (int i = 0; i < 5; i++) {
            persistProduct(ProductStatus.DRAFT);
        }

        Page<ProductResponse> firstPage = adminProductService.getAllProducts(0, 2);
        Page<ProductResponse> secondPage = adminProductService.getAllProducts(1, 2);
        Page<ProductResponse> thirdPage = adminProductService.getAllProducts(2, 2);

        assertEquals(2, firstPage.getContent().size());
        assertEquals(2, secondPage.getContent().size());
        assertEquals(1, thirdPage.getContent().size());
        assertEquals(5, firstPage.getTotalElements());
    }

    @Test
    void getAllProducts_retornaPaginaVaciaCuandoNoExistenProductos() {
        Page<ProductResponse> result = adminProductService.getAllProducts(0, 20);

        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getAllProducts_verificaElMapeoADto() {
        Product product = persistProduct(ProductStatus.DRAFT);
        addAttribute(product, ATTRIBUTE_NAME, ATTRIBUTE_VALUE);

        Page<ProductResponse> result = adminProductService.getAllProducts(0, 20);

        assertEquals(1, result.getTotalElements());
        ProductResponse response = result.getContent().get(0);
        assertEquals(product.getId(), response.id());
        assertEquals(PRODUCT_NAME, response.name());
        assertEquals(ProductStatus.DRAFT, response.status());
        assertEquals(SELLER_KEYCLOAK_ID, response.sellerKeycloakId());
        assertEquals(1, response.attributes().size());
        ProductAttributeResponse attribute = response.attributes().get(0);
        assertEquals(ATTRIBUTE_NAME, attribute.name());
        assertEquals(ATTRIBUTE_VALUE, attribute.value());
    }

    // ------------------------------------------------------------------- helpers

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
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(persistBrand())
                .withCategory(persistCategory())
                .withSellerKeycloakId(SELLER_KEYCLOAK_ID)
                .withName(PRODUCT_NAME)
                .withSlug(PRODUCT_SLUG + "-" + suffix)
                .withStatus(status)
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
}
