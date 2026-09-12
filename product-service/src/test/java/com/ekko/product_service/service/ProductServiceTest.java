package com.ekko.product_service.service;

import com.ekko.product_service.builder.ProductImageTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.builder.ProductVariantTestDataBuilder;
import com.ekko.product_service.dto.request.CreateProductAttributeRequest;
import com.ekko.product_service.dto.request.CreateProductRequest;
import com.ekko.product_service.dto.request.UpdateProductRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductAttribute;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.entity.SellerStatusView;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.enums.SellerStatus;
import com.ekko.product_service.exception.InvalidProductStatusException;
import com.ekko.product_service.exception.NoActiveVariantException;
import com.ekko.product_service.exception.NoPrimaryImageException;
import com.ekko.product_service.exception.ProductAlreadyDeletedException;
import com.ekko.product_service.exception.ProductNotAvailableException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.exception.VariantNotFoundException;
import com.ekko.product_service.mapper.ProductMapper;
import com.ekko.product_service.messaging.ProductEventPublisher;
import com.ekko.product_service.messaging.dto.publish.ProductDeactivatedEvent;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductAttributeRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_NAME;
import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_VALUE;
import static com.ekko.product_service.util.TestConstants.CATEGORY_ID;
import static com.ekko.product_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductAttributeRepository productAttributeRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SlugService slugService;

    @Mock
    private OwnershipValidator ownershipValidator;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductEventPublisher productEventPublisher;

    @Mock(lenient = true)
    private com.ekko.product_service.util.SellerStatusValidator sellerStatusValidator;

    @InjectMocks
    private ProductService productService;

    private final UUID productId = UUID.randomUUID();
    private final UUID sellerId = SELLER_KEYCLOAK_ID;

    @BeforeEach
    void setUp() {
        SellerStatusView sellerView = SellerStatusView.builder()
                .sellerKeycloakId(sellerId)
                .sellerSlug("test-seller")
                .status(SellerStatus.ACTIVE)
                .build();
        when(sellerStatusValidator.validateCanOperate(sellerId)).thenReturn(sellerView);
    }

    // ------------------------------------------------------------------ createProduct

    @Test
    void createProduct_creaProductoEnDraft() {
        CreateProductRequest request = new CreateProductRequest(
                PRODUCT_NAME, "desc", null, CATEGORY_ID, null);

        when(slugService.generate(request.name())).thenReturn("iphone-16");
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(mock(Category.class)));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        productService.createProduct(request, sellerId);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();
        assertEquals(ProductStatus.DRAFT, saved.getStatus());
        assertEquals(sellerId, saved.getSellerKeycloakId());
        assertEquals("iphone-16", saved.getSlug());
    }

    @Test
    void createProduct_generaSlugUnico() {
        CreateProductRequest request = new CreateProductRequest(
                PRODUCT_NAME, null, null, CATEGORY_ID, null);

        when(slugService.generate(request.name())).thenReturn("iphone-16-2");
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(mock(Category.class)));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        productService.createProduct(request, sellerId);

        verify(slugService).generate(request.name());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("iphone-16-2", captor.getValue().getSlug());
    }

    @Test
    void createProduct_persisteAtributosCuandoVienenEnElRequest() {
        CreateProductAttributeRequest attributeRequest =
                new CreateProductAttributeRequest(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        CreateProductRequest request = new CreateProductRequest(
                PRODUCT_NAME, null, null, CATEGORY_ID, List.of(attributeRequest));

        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(mock(Category.class)));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        productService.createProduct(request, sellerId);

        ArgumentCaptor<ProductAttribute> captor = ArgumentCaptor.forClass(ProductAttribute.class);
        verify(productAttributeRepository).save(captor.capture());

        ProductAttribute saved = captor.getValue();
        assertEquals(ATTRIBUTE_NAME, saved.getName());
        assertEquals(ATTRIBUTE_VALUE, saved.getValue());
        assertNotNull(saved.getProduct());
    }

    @Test
    void createProduct_noPersisteAtributosSiNoVienenEnElRequest() {
        CreateProductRequest request = new CreateProductRequest(
                PRODUCT_NAME, "d", null, CATEGORY_ID, null);

        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(mock(Category.class)));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        productService.createProduct(request, sellerId);

        verify(productAttributeRepository, never()).save(any(ProductAttribute.class));
    }

    // ------------------------------------------------------------------ updateProduct

    @Test
    void updateProduct_actualizaLosCamposEnviados() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.DRAFT)
                .withDescription("original").build();
        UpdateProductRequest request = new UpdateProductRequest(
                null, "new description", null, null);

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        productService.updateProduct(productId, request, sellerId);

        assertEquals("new description", product.getDescription());
        assertEquals(PRODUCT_NAME, product.getName());
        assertEquals("iphone-16", product.getSlug());
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_regeneraSlugCuandoCambiaElNombre() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.DRAFT)
                .withName("iPhone 16")
                .withSlug("iphone-16").build();
        UpdateProductRequest request = new UpdateProductRequest(
                "Samsung Galaxy", null, null, null);

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);
        when(slugService.generate("Samsung Galaxy"))
                .thenReturn("samsung-galaxy");
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        productService.updateProduct(productId, request, sellerId);

        assertEquals("Samsung Galaxy", product.getName());
        assertEquals("samsung-galaxy", product.getSlug());
        verify(slugService).generate("Samsung Galaxy");
    }

    @Test
    void updateProduct_noSobreescribeCamposConNull() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.DRAFT)
                .withName("iPhone 16")
                .withSlug("iphone-16")
                .withDescription("original").build();
        UpdateProductRequest request = new UpdateProductRequest(null, null, null, null);

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        productService.updateProduct(productId, request, sellerId);

        assertEquals("iPhone 16", product.getName());
        assertEquals("iphone-16", product.getSlug());
        assertEquals("original", product.getDescription());
    }

    @Test
    void updateProduct_noPermiteModificarEnEstadoPendingReview() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.PENDING_REVIEW).build();
        UpdateProductRequest request = new UpdateProductRequest(
                "Nuevo nombre", null, null, null);

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        assertThrows(InvalidProductStatusException.class,
                () -> productService.updateProduct(productId, request, sellerId));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_noPermiteModificarEnEstadoActive() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.ACTIVE).build();
        UpdateProductRequest request = new UpdateProductRequest(
                "Nuevo nombre", null, null, null);

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        assertThrows(InvalidProductStatusException.class,
                () -> productService.updateProduct(productId, request, sellerId));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_lanzaExcepcionSiElProductoEstaEliminado() {
        Product product = ProductTestDataBuilder.aProduct()
                .withDeletedAt(LocalDateTime.now())
                .withStatus(ProductStatus.DRAFT).build();

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct(
                        productId, new UpdateProductRequest(null, null, null, null), sellerId));

        verify(productRepository, never()).save(any(Product.class));
    }

    // ------------------------------------------------------------- submitForReview

    @Test
    void submitForReview_cambiaEstadoAPendienteDeRevision() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.DRAFT)
                .withVariants(List.of(activeVariant()))
                .withImages(primaryImageList())
                .build();

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        productService.submitForReview(productId, sellerId);

        assertEquals(ProductStatus.PENDING_REVIEW, product.getStatus());
        verify(productRepository).save(product);
    }

    @Test
    void submitForReview_lanzaExcepcionSiNoHayVariantesActivas() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.DRAFT)
                .withVariants(List.of(ProductVariantTestDataBuilder.aVariant()
                        .withIsActive(false).build()))
                .withImages(primaryImageList())
                .build();

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        assertThrows(NoActiveVariantException.class,
                () -> productService.submitForReview(productId, sellerId));
    }

    @Test
    void submitForReview_lanzaExcepcionSiNoHayImagenPrincipal() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.DRAFT)
                .withVariants(List.of(activeVariant()))
                .withImages(List.of())
                .build();

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        assertThrows(NoPrimaryImageException.class,
                () -> productService.submitForReview(productId, sellerId));
    }

    @Test
    void submitForReview_lanzaExcepcionSiEstadoNoEsValido() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.ACTIVE)
                .withVariants(List.of(activeVariant()))
                .withImages(primaryImageList())
                .build();

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        assertThrows(InvalidProductStatusException.class,
                () -> productService.submitForReview(productId, sellerId));

        verify(productRepository, never()).save(any(Product.class));
    }

    // ------------------------------------------------------------ softDeleteProduct

    @Test
    void softDeleteProduct_asignaDeletedAt() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.ACTIVE)
                .build();

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        productService.softDeleteProduct(productId, sellerId);

        assertNotNull(product.getDeletedAt());
        verify(productRepository).save(product);

        ArgumentCaptor<ProductDeactivatedEvent> captor = ArgumentCaptor.forClass(ProductDeactivatedEvent.class);
        verify(productEventPublisher).publishProductDeactivated(captor.capture());
        ProductDeactivatedEvent event = captor.getValue();
        assertEquals(product.getId(), event.productId());
        assertEquals(product.getSellerKeycloakId(), event.sellerKeycloakId());
        assertEquals(ProductStatus.ACTIVE, event.previousStatus());
        assertNotNull(event.deactivatedAt());
    }

    @Test
    void softDeleteProduct_desactivaVariantes() {
        ProductVariant variant = ProductVariantTestDataBuilder.aVariant()
                .withIsActive(true).build();
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.DRAFT)
                .withVariants(List.of(variant))
                .build();

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        productService.softDeleteProduct(productId, sellerId);

        assertFalse(variant.getIsActive());
        verify(productEventPublisher, never()).publishProductDeactivated(any());
    }

    @Test
    void softDeleteProduct_lanzaExcepcionSiYaEstaEliminado() {
        Product product = ProductTestDataBuilder.aProduct()
                .withDeletedAt(LocalDateTime.now())
                .withStatus(ProductStatus.ACTIVE)
                .build();

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        assertThrows(ProductAlreadyDeletedException.class,
                () -> productService.softDeleteProduct(productId, sellerId));

        verify(productRepository, never()).save(any(Product.class));
        verify(productEventPublisher, never()).publishProductDeactivated(any());
    }

    // --------------------------------------------------------------- getMyProducts

    @Test
    void getMyProducts_devuelveSoloLosProductosDelVendedorPaginado() {
        Product p1 = ProductTestDataBuilder.aProduct().build();
        Product p2 = ProductTestDataBuilder.aProduct()
                .withId(UUID.randomUUID()).build();
        Page<Product> page = new PageImpl<>(List.of(p1, p2),
                PageRequest.of(0, 20), 2);

        when(productRepository.findBySellerKeycloakIdAndDeletedAtIsNull(
                eq(sellerId), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        Page<ProductResponse> result = productService.getMyProducts(sellerId, 0, 20);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(productRepository).findBySellerKeycloakIdAndDeletedAtIsNull(
                eq(sellerId), any(Pageable.class));
        verify(productMapper, times(2)).toResponse(any(Product.class));
    }

    // --------------------------------------------------------------- getProductById

    @Test
    void getProductById_devuelveElDetalleCompleto() {
        Product product = ProductTestDataBuilder.aProduct().build();
        ProductDetailResponse detail = mock(ProductDetailResponse.class);

        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);
        when(productMapper.toDetailResponse(product)).thenReturn(detail);

        ProductDetailResponse result = productService.getProductById(productId, sellerId);

        assertSame(detail, result);
        verify(ownershipValidator).validate(productId, sellerId);
        verify(productMapper).toDetailResponse(product);
    }

    // ------------------------------------------------------------------- verifyPurchasable

    @Test
    void verifyPurchasable_varianteNoExisteLanzaNotFoundException() {
        when(productVariantRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(VariantNotFoundException.class,
                () -> productService.verifyPurchasable(productId));
    }

    @Test
    void verifyPurchasable_productoNoActivoLanzaNotAvailable() {
        ProductVariant variant = ProductVariantTestDataBuilder.aVariant().build();
        Product product = ProductTestDataBuilder.aProduct().withStatus(ProductStatus.DRAFT).build();
        variant.setProduct(product);

        when(productVariantRepository.findById(any())).thenReturn(Optional.of(variant));

        assertThrows(ProductNotAvailableException.class,
                () -> productService.verifyPurchasable(productId));
    }

    @Test
    void verifyPurchasable_productoActivoPermiteCompra() {
        ProductVariant variant = ProductVariantTestDataBuilder.aVariant().build();
        Product product = ProductTestDataBuilder.aProduct().withStatus(ProductStatus.ACTIVE).build();
        variant.setProduct(product);

        when(productVariantRepository.findById(any())).thenReturn(Optional.of(variant));

        assertDoesNotThrow(() -> productService.verifyPurchasable(productId));
    }

    // ------------------------------------------------------------------- helpers

    private ProductVariant activeVariant() {
        return ProductVariantTestDataBuilder.aVariant().withIsActive(true).build();
    }

    private List<ProductImage> primaryImageList() {
        return List.of(ProductImageTestDataBuilder.anImage()
                .withIsPrimary(true).build());
    }
}