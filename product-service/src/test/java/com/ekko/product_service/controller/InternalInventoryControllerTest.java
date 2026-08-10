package com.ekko.product_service.controller;

import com.ekko.product_service.config.SecurityConfig;
import com.ekko.product_service.exception.InsufficientStockException;
import com.ekko.product_service.exception.InventoryNotFoundException;
import com.ekko.product_service.exception.ProductNotAvailableException;
import com.ekko.product_service.service.InventoryService;
import com.ekko.product_service.util.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalInventoryController.class)
@Import(SecurityConfig.class)
class InternalInventoryControllerTest {

    private static final UUID VARIANT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------ POST /internal/inventory/reserve (batch)

    @Test
    void reserve_llamaAlServicioYDevuelve200() throws Exception {
        mockMvc.perform(post("/internal/inventory/reserve")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(3)))
                .andExpect(status().isOk());

        verify(inventoryService).reserveStockBatch(anyList());
    }

    @Test
    void reserve_insufficientStockDevuelve409() throws Exception {
        doThrow(InsufficientStockException.class)
                .when(inventoryService).reserveStockBatch(anyList());

        mockMvc.perform(post("/internal/inventory/reserve")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(3)))
                .andExpect(status().isConflict());
    }

    @Test
    void reserve_inventoryNotFoundDevuelve404() throws Exception {
        doThrow(InventoryNotFoundException.class)
                .when(inventoryService).reserveStockBatch(anyList());

        mockMvc.perform(post("/internal/inventory/reserve")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(3)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reserve_productNotAvailableDevuelve409() throws Exception {
        doThrow(ProductNotAvailableException.class)
                .when(inventoryService).reserveStockBatch(anyList());

        mockMvc.perform(post("/internal/inventory/reserve")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(3)))
                .andExpect(status().isConflict());
    }

    @Test
    void reserve_sinJwtDevuelve401() throws Exception {
        mockMvc.perform(post("/internal/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(3)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reserve_rolIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(post("/internal/inventory/reserve")
                        .with(authentication(new JwtAuthenticationToken(JwtTestUtils.sellerJwt(),
                                List.of(new SimpleGrantedAuthority("ROLE_SELLER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(3)))
                .andExpect(status().isForbidden());
    }

    // --------------------------------------------------------------- helpers

    private String batchBody(int quantity) {
        return """
                [
                  {
                    "variantId": "%s",
                    "quantity": %d
                  }
                ]
                """.formatted(VARIANT_ID, quantity);
    }

    private RequestPostProcessor serviceOrderAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.adminJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE_ORDER"))));
    }
}