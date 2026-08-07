package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductImageTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.builder.ProductVariantTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.request.CreateProductAttributeRequest;
import com.ekko.product_service.dto.request.CreateProductRequest;
import com.ekko.product_service.dto.request.UpdateProductRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.dto.response.ProductVariantResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductAttribute;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.ForbiddenProductAccessException;
import com.ekko.product_service.exception.InvalidProductStatusException;
import com.ekko.product_service.exception.NoActiveVariantException;
import com.ekko.product_service.exception.NoPrimaryImageException;
import com.ekko.product_service.exception.ProductAlreadyDeletedException;
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
import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_NAME;
import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_VALUE;
import static com.ekko.product_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ProductServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProductService productService;

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

    private final UUID otherSellerId = CUSTOMER_KEYCLOAK_ID;

    // ------------------------------------------------------------------ createProduct

    @Test
    void createProduct_creaProductoConEstadoDraft() {
        Brand brand = persistBrand();
        Category category = persistCategory();
        CreateProductRequest request = new CreateProductRequest(
                PRODUCT_NAME, "Smartphone de alta gama", brand.getId(), category.getId(), List.of());

        ProductResponse response = productService.createProduct(request, SELLER_KEYCLOAK_ID);

        assertEquals(ProductStatus.DRAFT, response.status());
        assertEquals(PRODUCT_NAME, response.name());
        assertEquals(SELLER_KEYCLOAK_ID, response.sellerKeycloakId());
        assertTrue(productRepository.findById(response.id()).isPresent());
        assertEquals(ProductStatus.DRAFT,
                productRepository.findById(response.id()).orElseThrow().getStatus());
    }

    @Test
    void createProduct_generaUnSlugUnico() {
        Brand brand = persistBrand();
        Category category = persistCategory();
        CreateProductRequest request = new CreateProductRequest(
                PRODUCT_NAME, "desc", brand.getId(), category.getId(), List.of());

        ProductResponse first = productService.createProduct(request, SELLER_KEYCLOAK_ID);
        ProductResponse second = productService.createProduct(request, SELLER_KEYCLOAK_ID);

        assertEquals("iphone-16", first.slug());
        assertEquals("iphone-16-2", second.slug());
        assertEquals("iphone-16-2",
                productRepository.findById(second.id()).orElseThrow().getSlug());
    }

    @Test
    void createProduct_persisteLosAtributosRecibidos() {
        Brand brand = persistBrand();
        Category category = persistCategory();
        CreateProductAttributeRequest attributeRequest =
                new CreateProductAttributeRequest(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        CreateProductRequest request = new CreateProductRequest(
                PRODUCT_NAME, "desc", brand.getId(), category.getId(), List.of(attributeRequest));

        ProductResponse response = productService.createProduct(request, SELLER_KEYCLOAK_ID);

        Product saved = productRepository.findById(response.id()).orElseThrow();
        assertEquals(1, saved.getAttributes().size());
        assertEquals(ATTRIBUTE_NAME, saved.getAttributes().get(0).getName());
        assertEquals(ATTRIBUTE_VALUE, saved.getAttributes().get(0).getValue());
    }

    @Test
    void createProduct_asociaCorrectamenteLaMarcaYLaCategoria() {
        Brand brand = persistBrand();
        Category category = persistCategory();
        CreateProductRequest request = new CreateProductRequest(
                PRODUCT_NAME, "desc", brand.getId(), category.getId(), List.of());

        ProductResponse response = productService.createProduct(request, SELLER_KEYCLOAK_ID);

        Product saved = productRepository.findById(response.id()).orElseThrow();
        assertEquals(brand.getId(), saved.getBrand().getId());
        assertEquals(brand.getName(), saved.getBrand().getName());
        assertEquals(category.getId(), saved.getCategory().getId());
        assertEquals(category.getName(), saved.getCategory().getName());
    }

    // ------------------------------------------------------------------ updateProduct

    @Test
    void updateProduct_actualizaLosCamposEnviados() {
        Brand originalBrand = persistBrand();
        Category originalCategory = persistCategory();
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        Brand newBrand = persistBrand();
        Category newCategory = persistCategory();
        UpdateProductRequest request = new UpdateProductRequest(
                null, "Descripcion actualizada", newBrand.getId(), newCategory.getId());

        ProductResponse response = productService.updateProduct(
                product.getId(), request, SELLER_KEYCLOAK_ID);

        assertEquals("Descripcion actualizada", response.description());
        assertEquals(PRODUCT_NAME, response.name());
        assertEquals(PRODUCT_SLUG, response.slug());

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals("Descripcion actualizada", saved.getDescription());
        assertEquals(newBrand.getId(), saved.getBrand().getId());
        assertEquals(newCategory.getId(), saved.getCategory().getId());
        assertFalse(originalBrand.getId().equals(saved.getBrand().getId()));
        assertFalse(originalCategory.getId().equals(saved.getCategory().getId()));
    }

    @Test
    void updateProduct_regeneraElSlugCuandoCambiaElNombre() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        UpdateProductRequest request = new UpdateProductRequest(
                "Samsung Galaxy", null, null, null);

        ProductResponse response = productService.updateProduct(
                product.getId(), request, SELLER_KEYCLOAK_ID);

        assertEquals("Samsung Galaxy", response.name());
        assertEquals("samsung-galaxy", response.slug());

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals("Samsung Galaxy", saved.getName());
        assertEquals("samsung-galaxy", saved.getSlug());
    }

    @Test
    void updateProduct_noSobreescribeValoresNull() {
        Brand brand = persistBrand();
        Category category = persistCategory();
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        product.setBrand(brand);
        product.setCategory(category);
        UpdateProductRequest request = new UpdateProductRequest(null, null, null, null);

        productService.updateProduct(product.getId(), request, SELLER_KEYCLOAK_ID);

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(PRODUCT_NAME, saved.getName());
        assertEquals(PRODUCT_SLUG, saved.getSlug());
        assertEquals(brand.getId(), saved.getBrand().getId());
        assertEquals(category.getId(), saved.getCategory().getId());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void updateProduct_permiteModificarProductoEnEstadoDraft() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        UpdateProductRequest request = new UpdateProductRequest(
                null, "nueva descripcion", null, null);

        ProductResponse response = productService.updateProduct(
                product.getId(), request, SELLER_KEYCLOAK_ID);

        assertEquals("nueva descripcion", response.description());
        assertEquals(ProductStatus.DRAFT,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void updateProduct_permiteModificarProductoEnEstadoRejected() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.REJECTED, null);
        UpdateProductRequest request = new UpdateProductRequest(
                null, "nueva descripcion", null, null);

        ProductResponse response = productService.updateProduct(
                product.getId(), request, SELLER_KEYCLOAK_ID);

        assertEquals("nueva descripcion", response.description());
        assertEquals(ProductStatus.REJECTED,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void updateProduct_noPermiteModificarProductoEnEstadoPendingReview() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.PENDING_REVIEW, null);
        UpdateProductRequest request = new UpdateProductRequest(
                "Nuevo nombre", null, null, null);

        assertThrows(InvalidProductStatusException.class,
                () -> productService.updateProduct(product.getId(), request, SELLER_KEYCLOAK_ID));

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(PRODUCT_NAME, saved.getName());
        assertEquals(ProductStatus.PENDING_REVIEW, saved.getStatus());
    }

    @Test
    void updateProduct_noPermiteModificarProductoEnEstadoActive() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.ACTIVE, null);
        UpdateProductRequest request = new UpdateProductRequest(
                "Nuevo nombre", null, null, null);

        assertThrows(InvalidProductStatusException.class,
                () -> productService.updateProduct(product.getId(), request, SELLER_KEYCLOAK_ID));

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(PRODUCT_NAME, saved.getName());
        assertEquals(ProductStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void updateProduct_lanzaExcepcionCuandoElProductoEstaEliminado() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, LocalDateTime.now());
        UpdateProductRequest request = new UpdateProductRequest(
                "Nuevo nombre", null, null, null);

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct(product.getId(), request, SELLER_KEYCLOAK_ID));
    }

    // ------------------------------------------------------------- submitForReview

    @Test
    void submitForReview_cambiaEstadoAPendingReview() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        addVariant(product, true);
        addImage(product, true);

        productService.submitForReview(product.getId(), SELLER_KEYCLOAK_ID);

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.PENDING_REVIEW, saved.getStatus());
    }

    @Test
    void submitForReview_lanzaExcepcionCuandoNoExistenVariantesActivas() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        addVariant(product, false);
        addImage(product, true);

        assertThrows(NoActiveVariantException.class,
                () -> productService.submitForReview(product.getId(), SELLER_KEYCLOAK_ID));
    }

    @Test
    void submitForReview_lanzaExcepcionCuandoNoExisteImagenPrincipal() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        addVariant(product, true);

        assertThrows(NoPrimaryImageException.class,
                () -> productService.submitForReview(product.getId(), SELLER_KEYCLOAK_ID));
    }

    @Test
    void submitForReview_lanzaExcepcionCuandoElEstadoNoEsValido() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.ACTIVE, null);
        addVariant(product, true);
        addImage(product, true);

        assertThrows(InvalidProductStatusException.class,
                () -> productService.submitForReview(product.getId(), SELLER_KEYCLOAK_ID));

        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }

    // ------------------------------------------------------------ softDeleteProduct

    @Test
    void softDeleteProduct_asignaDeletedAt() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        productService.softDeleteProduct(product.getId(), SELLER_KEYCLOAK_ID);

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertNotNull(saved.getDeletedAt());
    }

    @Test
    void softDeleteProduct_desactivaTodasLasVariantesActivas() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        ProductVariant v1 = addVariant(product, true);
        ProductVariant v2 = addVariant(product, true);

        productService.softDeleteProduct(product.getId(), SELLER_KEYCLOAK_ID);

        ProductVariant savedV1 = productVariantRepository.findById(v1.getId()).orElseThrow();
        ProductVariant savedV2 = productVariantRepository.findById(v2.getId()).orElseThrow();
        assertFalse(savedV1.getIsActive());
        assertFalse(savedV2.getIsActive());
        assertNotNull(productRepository.findById(product.getId()).orElseThrow().getDeletedAt());
    }

    @Test
    void softDeleteProduct_lanzaExcepcionCuandoElProductoYaEstaEliminado() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, LocalDateTime.now());

        assertThrows(ProductAlreadyDeletedException.class,
                () -> productService.softDeleteProduct(product.getId(), SELLER_KEYCLOAK_ID));
    }

    // --------------------------------------------------------------- getMyProducts

    @Test
    void getMyProducts_retornaSoloLosProductosNoEliminadosDelVendedor() {
        persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, "iPhone 15", "iphone-15", ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, "iPhone SE", "iphone-se", ProductStatus.DRAFT,
                LocalDateTime.now());
        persistProduct(otherSellerId, "Galaxy", "galaxy-s25", ProductStatus.DRAFT, null);

        Page<ProductResponse> result = productService.getMyProducts(SELLER_KEYCLOAK_ID, 0, 20);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(product -> SELLER_KEYCLOAK_ID.equals(product.sellerKeycloakId())));
        assertEquals(0, result.getContent().stream()
                .filter(product -> product.name().equals("iPhone SE")).count());
    }

    @Test
    void getMyProducts_respetaLaPaginacion() {
        persistProduct(SELLER_KEYCLOAK_ID, "Producto A", "producto-a", ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, "Producto B", "producto-b", ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, "Producto C", "producto-c", ProductStatus.DRAFT, null);

        Page<ProductResponse> firstPage = productService.getMyProducts(SELLER_KEYCLOAK_ID, 0, 2);
        Page<ProductResponse> secondPage = productService.getMyProducts(SELLER_KEYCLOAK_ID, 1, 2);

        assertEquals(2, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
        assertEquals(3, firstPage.getTotalElements());
    }

    @Test
    void getMyProducts_retornaPaginaVaciaCuandoElVendedorNoTieneProductos() {
        Page<ProductResponse> result = productService.getMyProducts(otherSellerId, 0, 20);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    // --------------------------------------------------------------- getProductById

    @Test
    void getProductById_retornaElDetalleCompletoDelProducto() {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.ACTIVE, null);
        addAttribute(product, ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        ProductImage image = addImage(product, true);
        ProductVariant variant = addVariant(product, true);

        ProductDetailResponse detail = productService.getProductById(
                product.getId(), SELLER_KEYCLOAK_ID);

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
        assertTrue(variantResponse.isActive());
    }

    @Test
    void getProductById_lanzaExcepcionCuandoElProductoNoPerteneceAlVendedor() {
        Product product = persistProduct(otherSellerId, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        assertThrows(ForbiddenProductAccessException.class,
                () -> productService.getProductById(product.getId(), SELLER_KEYCLOAK_ID));
    }

    @Test
    void getProductById_lanzaExcepcionCuandoElProductoNoExiste() {
        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(UUID.randomUUID(), SELLER_KEYCLOAK_ID));
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

    private Product persistProduct(UUID sellerKeycloakId, String name, String slug,
                                   ProductStatus status, LocalDateTime deletedAt) {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(persistBrand())
                .withCategory(persistCategory())
                .withSellerKeycloakId(sellerKeycloakId)
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
