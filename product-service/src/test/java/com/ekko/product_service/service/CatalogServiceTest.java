package com.ekko.product_service.service;

import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.builder.ProductVariantTestDataBuilder;
import com.ekko.product_service.dto.request.ProductFiltersRequest;
import com.ekko.product_service.dto.response.ProductSummaryResponse;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductSortOption;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.InvalidPriceRangeException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.mapper.ProductMapper;
import com.ekko.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CatalogService catalogService;

    private final UUID categoryId = UUID.randomUUID();

    // ------------------------------------------------------------- searchProducts

    @Test
    void searchProducts_sinFiltrosDevuelvePagina() {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(UUID.randomUUID()).build();
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductSummaryResponse> result =
                catalogService.searchProducts(emptyFilters(), 0, 20, ProductSortOption.RECENT);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchProducts_filtroPorCategoria_aplicaDescendientes() {
        Product product = ProductTestDataBuilder.aProduct().withId(UUID.randomUUID()).build();
        when(categoryService.getDescendantIds(categoryId)).thenReturn(Set.of(categoryId));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        catalogService.searchProducts(new ProductFiltersRequest(
                categoryId, null, null, null, null, null), 0, 20, ProductSortOption.RECENT);

        verify(categoryService).getDescendantIds(categoryId);
    }

    @Test
    void searchProducts_ordenaPorPrecioAscendente() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        catalogService.searchProducts(emptyFilters(), 0, 20, ProductSortOption.PRICE_ASC);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchProducts_precioMinimoMayorQueMaximoLanzaInvalidPriceRange() {
        assertThrows(InvalidPriceRangeException.class, () ->
                catalogService.searchProducts(new ProductFiltersRequest(
                        null, null, null, new BigDecimal("500.00"), new BigDecimal("100.00"), null),
                        0, 20, ProductSortOption.RECENT));
    }

    @Test
    void searchProducts_resumenConsideraSoloVariantesActivas() {
        Product product = ProductTestDataBuilder.aProduct().withId(UUID.randomUUID()).build();
        product.getVariants().add(variant("10.00", false));
        product.getVariants().add(variant("300.00", true));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductSummaryResponse> result =
                catalogService.searchProducts(emptyFilters(), 0, 20, ProductSortOption.RECENT);

        assertEquals("300.00", result.getContent().get(0).price().toPlainString());
    }

    // ------------------------------------------------------------- getProductDetail

    @Test
    void getProductDetail_encuentraProducto() {
        Product product = ProductTestDataBuilder.aProduct().withId(UUID.randomUUID()).build();
        when(productRepository.findBySlugAndStatusAndDeletedAtIsNull(any(), eq(ProductStatus.ACTIVE)))
                .thenReturn(Optional.of(product));

        catalogService.getProductDetail(product.getSlug());

        verify(productMapper).toDetailResponse(product);
    }

    @Test
    void getProductDetail_noExisteLanzaNotFound() {
        when(productRepository.findBySlugAndStatusAndDeletedAtIsNull(any(), eq(ProductStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> catalogService.getProductDetail("no-existe"));
    }

    // --------------------------------------------------------------- helpers

    private ProductFiltersRequest emptyFilters() {
        return new ProductFiltersRequest(null, null, null, null, null, null);
    }

    private ProductVariant variant(String price, boolean active) {
        return ProductVariantTestDataBuilder.aVariant()
                .withPrice(new BigDecimal(price))
                .withIsActive(active)
                .build();
    }
}