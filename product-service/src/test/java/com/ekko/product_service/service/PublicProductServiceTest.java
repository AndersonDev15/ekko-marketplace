package com.ekko.product_service.service;

import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.dto.response.ProductCatalogResponse;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.mapper.ProductMapper;
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

import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_SLUG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private PublicProductService publicProductService;

    // ------------------------------------------------------------- getPublicCatalog

    @Test
    void getPublicCatalog_retornaProductosActivosPaginados() {
        Product p1 = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.ACTIVE).build();
        Product p2 = ProductTestDataBuilder.aProduct()
                .withId(UUID.randomUUID())
                .withStatus(ProductStatus.ACTIVE).build();
        Page<Product> page = new PageImpl<>(List.of(p1, p2),
                PageRequest.of(0, 20), 2);

        when(productRepository.findByStatusAndDeletedAtIsNull(
                any(ProductStatus.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toCatalogResponse(any(Product.class)))
                .thenReturn(mock(ProductCatalogResponse.class));

        Page<ProductCatalogResponse> result =
                publicProductService.getPublicCatalog(0, 20);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getNumberOfElements());
        verify(productMapper, times(2)).toCatalogResponse(any(Product.class));
    }

    @Test
    void getPublicCatalog_unicaConsultaConEstadoActiveYPaginacion() {
        Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productRepository.findByStatusAndDeletedAtIsNull(
                any(ProductStatus.class), any(Pageable.class))).thenReturn(page);

        publicProductService.getPublicCatalog(0, 20);

        ArgumentCaptor<ProductStatus> statusCaptor =
                ArgumentCaptor.forClass(ProductStatus.class);
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(productRepository).findByStatusAndDeletedAtIsNull(
                statusCaptor.capture(), pageableCaptor.capture());

        assertEquals(ProductStatus.ACTIVE, statusCaptor.getValue());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getPublicCatalog_retornaPaginaVaciaCuandoNoHayProductos() {
        Page<Product> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productRepository.findByStatusAndDeletedAtIsNull(
                any(ProductStatus.class), any(Pageable.class))).thenReturn(emptyPage);

        Page<ProductCatalogResponse> result =
                publicProductService.getPublicCatalog(0, 20);

        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getNumberOfElements());
        verify(productMapper, never())
                .toCatalogResponse(any(Product.class));
    }

    // ------------------------------------------------------------- getPublicProduct

    @Test
    void getPublicProduct_retornaDetalleCompleto() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.ACTIVE).build();
        ProductDetailResponse detail = mock(ProductDetailResponse.class);

        when(productRepository.findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                any(String.class), any(String.class), any(ProductStatus.class)))
                .thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(detail);

        ProductDetailResponse result =
                publicProductService.getPublicProduct(SELLER_SLUG, PRODUCT_SLUG);

        assertSame(detail, result);
        verify(productMapper).toDetailResponse(product);
    }

    @Test
    void getPublicProduct_buscaConSellerSlugProductSlugYEstadoActive() {
        Product product = ProductTestDataBuilder.aProduct()
                .withStatus(ProductStatus.ACTIVE).build();

        when(productRepository.findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                any(String.class), any(String.class), any(ProductStatus.class)))
                .thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(any())).thenReturn(mock(ProductDetailResponse.class));

        publicProductService.getPublicProduct(SELLER_SLUG, PRODUCT_SLUG);

        ArgumentCaptor<String> sellerCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> slugCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ProductStatus> statusCaptor =
                ArgumentCaptor.forClass(ProductStatus.class);

        verify(productRepository)
                .findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                        sellerCaptor.capture(), slugCaptor.capture(), statusCaptor.capture());

        assertEquals(SELLER_SLUG, sellerCaptor.getValue());
        assertEquals(PRODUCT_SLUG, slugCaptor.getValue());
        assertEquals(ProductStatus.ACTIVE, statusCaptor.getValue());
    }

    @Test
    void getPublicProduct_lanzaExcepcionSiNoExiste() {
        when(productRepository.findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                any(String.class), any(String.class), any(ProductStatus.class)))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> publicProductService.getPublicProduct(SELLER_SLUG, PRODUCT_SLUG));

        verify(productMapper, never()).toDetailResponse(any(Product.class));
    }
}