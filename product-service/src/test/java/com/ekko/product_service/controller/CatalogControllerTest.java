package com.ekko.product_service.controller;

import com.ekko.product_service.config.SecurityConfig;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductSummaryResponse;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.InvalidPriceRangeException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.service.CatalogService;
import com.ekko.product_service.util.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
class CatalogControllerTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------ GET /catalog/products

    @Test
    void searchProducts_devuelve200YPagina() throws Exception {
        ProductSummaryResponse summary = new ProductSummaryResponse(PRODUCT_ID,
                "iPhone 16", "iphone-16", "apple-store",
                new BigDecimal("999.99"),
                "https://cdn.example.com/iphone-16.jpg", "Apple", "Phones", true);
        org.springframework.data.domain.Page<ProductSummaryResponse> page =
                new org.springframework.data.domain.PageImpl<>(List.of(summary));
        when(catalogService.searchProducts(any(), eq(0), eq(20), any())).thenReturn(page);

        mockMvc.perform(get("/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("iPhone 16"))
                .andExpect(jsonPath("$.content[0].price").value(999.99));

        verify(catalogService).searchProducts(any(), eq(0), eq(20), any());
    }

    @Test
    void searchProducts_aplicaFiltrosDeQueryParam() throws Exception {
        when(catalogService.searchProducts(any(), any(Integer.class), any(Integer.class), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(get("/catalog/products")
                        .param("categoryId", "00000000-0000-0000-0000-000000000003")
                        .param("minPrice", "100")
                        .param("maxPrice", "500")
                        .param("query", "iphone")
                        .param("page", "2")
                        .param("size", "10")
                        .param("sort", "PRICE_ASC"))
                .andExpect(status().isOk());

        verify(catalogService).searchProducts(any(), eq(2), eq(10), any());
    }

    @Test
    void searchProducts_sizeMayorQue100Devuelve400() throws Exception {
        mockMvc.perform(get("/catalog/products")
                        .param("size", "200"))
                .andExpect(status().isBadRequest());

        verify(catalogService, never()).searchProducts(any(), any(Integer.class),
                any(Integer.class), any());
    }

    @Test
    void searchProducts_pageNegativaDevuelve400() throws Exception {
        mockMvc.perform(get("/catalog/products")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(catalogService, never()).searchProducts(any(), any(Integer.class),
                any(Integer.class), any());
    }

    @Test
    void searchProducts_precioNegativoDevuelve400() throws Exception {
        mockMvc.perform(get("/catalog/products")
                        .param("minPrice", "-10"))
                .andExpect(status().isBadRequest());

        verify(catalogService, never()).searchProducts(any(), any(Integer.class),
                any(Integer.class), any());
    }

    @Test
    void searchProducts_rangoInvalidoDevuelve400() throws Exception {
        when(catalogService.searchProducts(any(), any(Integer.class), any(Integer.class), any()))
                .thenThrow(InvalidPriceRangeException.class);

        mockMvc.perform(get("/catalog/products")
                        .param("minPrice", "500")
                        .param("maxPrice", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchProducts_publicoSinAutenticacionDevuelve200() throws Exception {
        when(catalogService.searchProducts(any(), any(Integer.class), any(Integer.class), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(get("/catalog/products"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------ GET /catalog/products/{slug}

    @Test
    void getProductDetail_devuelve200YDetalle() throws Exception {
        ProductDetailResponse detail = new ProductDetailResponse(PRODUCT_ID,
                UUID.randomUUID(), "iPhone 16", "iphone-16", "desc", ProductStatus.ACTIVE,
                BigDecimal.ZERO, 0, List.of(), List.of(), List.of());
        when(catalogService.getProductDetail("iphone-16")).thenReturn(detail);

        mockMvc.perform(get("/catalog/products/{slug}", "iphone-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone 16"));

        verify(catalogService).getProductDetail("iphone-16");
    }

    @Test
    void getProductDetail_noExisteDevuelve404() throws Exception {
        when(catalogService.getProductDetail("no-existe")).thenThrow(ProductNotFoundException.class);

        mockMvc.perform(get("/catalog/products/{slug}", "no-existe"))
                .andExpect(status().isNotFound());
    }

    // --------------------------------------------------------------- helpers

    @SuppressWarnings("unused")
    private RequestPostProcessor anyAuth() {
        return JwtTestUtils.customerAuth(jwtAuthenticationConverter);
    }
}