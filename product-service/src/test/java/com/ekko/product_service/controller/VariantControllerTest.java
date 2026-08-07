package com.ekko.product_service.controller;

import com.ekko.product_service.config.SecurityConfig;
import com.ekko.product_service.dto.request.CreateVariantRequest;
import com.ekko.product_service.dto.request.UpdateVariantRequest;
import com.ekko.product_service.dto.response.VariantAttributeResponse;
import com.ekko.product_service.dto.response.VariantResponse;
import com.ekko.product_service.dto.response.VariantSummaryResponse;
import com.ekko.product_service.service.VariantService;
import com.ekko.product_service.util.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VariantController.class)
@Import(SecurityConfig.class)
class VariantControllerTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = SELLER_KEYCLOAK_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VariantService variantService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------ POST /seller/products/{id}/variants

    @Test
    void postVariant_devuelve201YVariantResponse() throws Exception {
        VariantResponse response = variantResponse();
        when(variantService.createVariant(eq(PRODUCT_ID), any(CreateVariantRequest.class), eq(SELLER_ID)))
                .thenReturn(response);

        mockMvc.perform(post(baseUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-1",
                                  "price": 100.00,
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(VARIANT_ID.toString()))
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()));

        verify(variantService).createVariant(eq(PRODUCT_ID), any(CreateVariantRequest.class), eq(SELLER_ID));
    }

    @Test
    void postVariant_devuelve400ParaBodyInvalido() throws Exception {
        mockMvc.perform(post(baseUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postVariant_devuelve401SinJWT() throws Exception {
        mockMvc.perform(post(baseUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-1",
                                  "price": 100.00
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postVariant_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(post(baseUrl())
                        .with(customerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-1",
                                  "price": 100.00
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ PUT /seller/products/{id}/variants/{variantId}

    @Test
    void putVariant_devuelve200YVariantSummaryResponse() throws Exception {
        VariantSummaryResponse response = variantSummaryResponse();
        when(variantService.updateVariant(eq(PRODUCT_ID), eq(VARIANT_ID),
                any(UpdateVariantRequest.class), eq(SELLER_ID))).thenReturn(response);

        mockMvc.perform(put(baseUrl() + "/" + VARIANT_ID)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 150.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VARIANT_ID.toString()))
                .andExpect(jsonPath("$.price").value(150.00));

        verify(variantService).updateVariant(eq(PRODUCT_ID), eq(VARIANT_ID),
                any(UpdateVariantRequest.class), eq(SELLER_ID));
    }

    @Test
    void putVariant_devuelve400ParaBodyInvalido() throws Exception {
        mockMvc.perform(put(baseUrl() + "/" + VARIANT_ID)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putVariant_devuelve401SinJWT() throws Exception {
        mockMvc.perform(put(baseUrl() + "/" + VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 150.00
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putVariant_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(put(baseUrl() + "/" + VARIANT_ID)
                        .with(customerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 150.00
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ PATCH /seller/products/{id}/variants/{variantId}/deactivate

    @Test
    void deactivateVariant_devuelve200YVariantSummaryResponse() throws Exception {
        VariantSummaryResponse response = variantSummaryResponse();
        when(variantService.deactivateVariant(eq(PRODUCT_ID), eq(VARIANT_ID), eq(SELLER_ID)))
                .thenReturn(response);

        mockMvc.perform(patch(baseUrl() + "/" + VARIANT_ID + "/deactivate")
                        .with(sellerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VARIANT_ID.toString()))
                .andExpect(jsonPath("$.isActive").value(false));

        verify(variantService).deactivateVariant(eq(PRODUCT_ID), eq(VARIANT_ID), eq(SELLER_ID));
    }

    @Test
    void deactivateVariant_devuelve401SinJWT() throws Exception {
        mockMvc.perform(patch(baseUrl() + "/" + VARIANT_ID + "/deactivate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivateVariant_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(patch(baseUrl() + "/" + VARIANT_ID + "/deactivate")
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ DELETE /seller/products/{id}/variants/{variantId}

    @Test
    void deleteVariant_devuelve204() throws Exception {
        mockMvc.perform(delete(baseUrl() + "/" + VARIANT_ID)
                        .with(sellerAuth()))
                .andExpect(status().isNoContent());

        verify(variantService).softDeleteVariant(eq(PRODUCT_ID), eq(VARIANT_ID), eq(SELLER_ID));
    }

    @Test
    void deleteVariant_devuelve401SinJWT() throws Exception {
        mockMvc.perform(delete(baseUrl() + "/" + VARIANT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteVariant_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(delete(baseUrl() + "/" + VARIANT_ID)
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------- helpers

    private String baseUrl() {
        return "/seller/products/" + PRODUCT_ID + "/variants";
    }

    private RequestPostProcessor sellerAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.sellerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_SELLER"))));
    }

    private RequestPostProcessor customerAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.customerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private VariantResponse variantResponse() {
        return new VariantResponse(
                VARIANT_ID,
                PRODUCT_ID,
                "SKU-1",
                new BigDecimal("100.00"),
                null,
                "USD",
                true,
                0L,
                List.of(new VariantAttributeResponse("color", "black")),
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 1, 0, 0));
    }

    private VariantSummaryResponse variantSummaryResponse() {
        return new VariantSummaryResponse(
                VARIANT_ID,
                PRODUCT_ID,
                "SKU-1",
                new BigDecimal("150.00"),
                null,
                "USD",
                false,
                List.of(new VariantAttributeResponse("color", "black")),
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 1, 0, 0));
    }
}