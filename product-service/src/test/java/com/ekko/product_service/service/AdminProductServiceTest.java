package com.ekko.product_service.service;

import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.InvalidProductStatusException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.mapper.ProductMapper;
import com.ekko.product_service.messaging.ProductEventPublisher;
import com.ekko.product_service.messaging.dto.publish.ProductPublishedEvent;
import com.ekko.product_service.messaging.dto.publish.ProductRejectedEvent;
import com.ekko.product_service.repository.ProductRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductEventPublisher productEventPublisher;

    @InjectMocks
    private AdminProductService adminProductService;

    private final UUID productId = UUID.randomUUID();

    // ----------------------------------------------------------------- approveProduct

    @Test
    void approveProduct_cambiaEstadoAActive() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.PENDING_REVIEW)
                .withName("iPhone 16")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminProductService.approveProduct(productId);

        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        verify(productRepository).save(product);

        ArgumentCaptor<ProductPublishedEvent> captor = ArgumentCaptor.forClass(ProductPublishedEvent.class);
        verify(productEventPublisher).publishProductPublished(captor.capture());
        ProductPublishedEvent event = captor.getValue();
        assertEquals(product.getId(), event.productId());
        assertEquals(product.getSellerKeycloakId(), event.sellerKeycloakId());
        assertEquals(product.getName(), event.name());
        assertEquals(product.getCategory().getName(), event.category());
        assertNotNull(event.publishedAt());
    }

    @Test
    void approveProduct_noModificaOtrosCampos() {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(productId)
                .withStatus(ProductStatus.PENDING_REVIEW)
                .withName("iPhone 16")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminProductService.approveProduct(productId);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();
        assertSame(product, saved);
        assertEquals(ProductStatus.ACTIVE, saved.getStatus());
        assertEquals(productId, saved.getId());
        assertEquals("iPhone 16", saved.getName());
        assertEquals(SELLER_KEYCLOAK_ID, saved.getSellerKeycloakId());
    }

    @Test
    void approveProduct_lanzaExcepcionSiElProductoNoExiste() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> adminProductService.approveProduct(productId));

        verify(productRepository, never()).save(any(Product.class));
        verify(productEventPublisher, never()).publishProductPublished(any());
    }

    @Test
    void approveProduct_lanzaExcepcionSiElEstadoNoEsPendingReview() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductStatusException.class,
                () -> adminProductService.approveProduct(productId));

        verify(productRepository, never()).save(any(Product.class));
        verify(productEventPublisher, never()).publishProductPublished(any());
    }

    // ----------------------------------------------------------------- rejectProduct

    @Test
    void rejectProduct_cambiaEstadoARejected() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.PENDING_REVIEW)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminProductService.rejectProduct(productId, "marca prohibida");

        assertEquals(ProductStatus.REJECTED, product.getStatus());
        verify(productRepository).save(product);

        ArgumentCaptor<ProductRejectedEvent> captor = ArgumentCaptor.forClass(ProductRejectedEvent.class);
        verify(productEventPublisher).publishProductRejected(captor.capture());
        ProductRejectedEvent event = captor.getValue();
        assertEquals(product.getId(), event.productId());
        assertEquals(product.getSellerKeycloakId(), event.sellerKeycloakId());
        assertEquals(product.getName(), event.name());
        assertEquals("marca prohibida", event.reason());
        assertNotNull(event.rejectedAt());
    }

    @Test
    void rejectProduct_procesaElMotivo() {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(productId)
                .withStatus(ProductStatus.PENDING_REVIEW)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String reason = "marca prohibida";
        adminProductService.rejectProduct(productId, reason);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();
        assertSame(product, saved);
        assertEquals(ProductStatus.REJECTED, saved.getStatus());
        assertEquals(productId, saved.getId());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void rejectProduct_lanzaExcepcionSiElProductoNoExiste() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> adminProductService.rejectProduct(productId, "motivo"));

        verify(productRepository, never()).save(any(Product.class));
        verify(productEventPublisher, never()).publishProductRejected(any());
    }

    @Test
    void rejectProduct_lanzaExcepcionSiElEstadoNoEsPendingReview() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.DRAFT)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductStatusException.class,
                () -> adminProductService.rejectProduct(productId, "motivo"));

        verify(productRepository, never()).save(any(Product.class));
        verify(productEventPublisher, never()).publishProductRejected(any());
    }

    // ----------------------------------------------------------------- getAllProducts

    @Test
    void getAllProducts_retornaProductosPaginados() {
        Product p1 = ProductTestDataBuilder.aProduct().build();
        Product p2 = ProductTestDataBuilder.aProduct()
                .withId(UUID.randomUUID()).build();
        Page<Product> page = new PageImpl<>(List.of(p1, p2),
                PageRequest.of(1, 20), 2);

        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(mock(ProductResponse.class));

        Page<ProductResponse> result = adminProductService.getAllProducts(1, 20);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getNumberOfElements());
        verify(productMapper, times(2)).toResponse(any(Product.class));
    }

    @Test
    void getAllProducts_respetaLaPaginacion() {
        Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        adminProductService.getAllProducts(0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void getAllProducts_retornaPaginaVaciaCuandoNoHayProductos() {
        Page<Product> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<ProductResponse> result = adminProductService.getAllProducts(0, 20);

        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
        verify(productMapper, never()).toResponse(any(Product.class));
    }
}