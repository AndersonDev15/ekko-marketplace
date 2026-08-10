package com.ekko.product_service.controller;

import com.ekko.product_service.config.SecurityConfig;
import com.ekko.product_service.dto.response.InventoryViewResponse;
import com.ekko.product_service.exception.InvalidStockAdjustmentException;
import com.ekko.product_service.exception.InvalidStockOperationException;
import com.ekko.product_service.exception.InventoryNotFoundException;
import com.ekko.product_service.exception.InventoryOwnershipException;
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

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import(SecurityConfig.class)
class InventoryControllerTest {

    private static final UUID VARIANT_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = SELLER_KEYCLOAK_ID;
    private static final UUID INVENTORY_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------ POST /internal/inventory/confirm

    @Test
    void confirm_llamaServicioYDevuelve200() throws Exception {
        mockMvc.perform(post("/internal/inventory/confirm")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityBody(2)))
                .andExpect(status().isOk());

        verify(inventoryService).confirmStock(eq(VARIANT_ID), eq(2L));
    }

    @Test
    void confirm_invalidOperationDevuelve409() throws Exception {
        doThrow(InvalidStockOperationException.class)
                .when(inventoryService).confirmStock(eq(VARIANT_ID), eq(2L));

        mockMvc.perform(post("/internal/inventory/confirm")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityBody(2)))
                .andExpect(status().isConflict());
    }

    @Test
    void confirm_inventoryNotFoundDevuelve404() throws Exception {
        doThrow(InventoryNotFoundException.class)
                .when(inventoryService).confirmStock(eq(VARIANT_ID), eq(2L));

        mockMvc.perform(post("/internal/inventory/confirm")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityBody(2)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------ POST /internal/inventory/release

    @Test
    void release_llamaServicioYDevuelve200() throws Exception {
        mockMvc.perform(post("/internal/inventory/release")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityBody(1)))
                .andExpect(status().isOk());

        verify(inventoryService).releaseStock(eq(VARIANT_ID), eq(1L));
    }

    @Test
    void release_invalidOperationDevuelve409() throws Exception {
        doThrow(InvalidStockOperationException.class)
                .when(inventoryService).releaseStock(eq(VARIANT_ID), eq(1L));

        mockMvc.perform(post("/internal/inventory/release")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityBody(1)))
                .andExpect(status().isConflict());
    }

    @Test
    void release_inventoryNotFoundDevuelve404() throws Exception {
        doThrow(InventoryNotFoundException.class)
                .when(inventoryService).releaseStock(eq(VARIANT_ID), eq(1L));

        mockMvc.perform(post("/internal/inventory/release")
                        .with(serviceOrderAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantityBody(1)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------ GET /seller/variants/{variantId}/inventory

    @Test
    void getInventory_devuelve200YInventario() throws Exception {
        when(inventoryService.getInventory(VARIANT_ID, SELLER_ID))
                .thenReturn(inventoryViewResponse());

        mockMvc.perform(get("/seller/variants/{variantId}/inventory", VARIANT_ID)
                        .with(sellerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INVENTORY_ID.toString()))
                .andExpect(jsonPath("$.stockAvailable").value(10))
                .andExpect(jsonPath("$.stockReserved").value(3))
                .andExpect(jsonPath("$.stockMinimum").value(5))
                .andExpect(jsonPath("$.availableForSale").value(7));

        verify(inventoryService).getInventory(VARIANT_ID, SELLER_ID);
    }

    @Test
    void getInventory_sinJwtDevuelve401() throws Exception {
        mockMvc.perform(get("/seller/variants/{variantId}/inventory", VARIANT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getInventory_rolIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/seller/variants/{variantId}/inventory", VARIANT_ID)
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getInventory_ownershipExceptionDevuelve403() throws Exception {
        when(inventoryService.getInventory(VARIANT_ID, SELLER_ID))
                .thenThrow(InventoryOwnershipException.class);

        mockMvc.perform(get("/seller/variants/{variantId}/inventory", VARIANT_ID)
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getInventory_noExisteDevuelve404() throws Exception {
        when(inventoryService.getInventory(VARIANT_ID, SELLER_ID))
                .thenThrow(InventoryNotFoundException.class);

        mockMvc.perform(get("/seller/variants/{variantId}/inventory", VARIANT_ID)
                        .with(sellerAuth()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------ PUT /seller/variants/{variantId}/inventory

    @Test
    void adjustStock_devuelve200YInventarioActualizado() throws Exception {
        when(inventoryService.adjustStock(VARIANT_ID, 20L, SELLER_ID))
                .thenReturn(inventoryViewResponse());

        mockMvc.perform(put("/seller/variants/{variantId}/inventory", VARIANT_ID)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStock": 20 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockAvailable").value(10));

        verify(inventoryService).adjustStock(VARIANT_ID, 20L, SELLER_ID);
    }

    @Test
    void adjustStock_ownershipExceptionDevuelve403() throws Exception {
        doThrow(InventoryOwnershipException.class)
                .when(inventoryService).adjustStock(VARIANT_ID, 20L, SELLER_ID);

        mockMvc.perform(put("/seller/variants/{variantId}/inventory", VARIANT_ID)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStock": 20 }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adjustStock_invalidAdjustmentDevuelve400() throws Exception {
        doThrow(InvalidStockAdjustmentException.class)
                .when(inventoryService).adjustStock(VARIANT_ID, 20L, SELLER_ID);

        mockMvc.perform(put("/seller/variants/{variantId}/inventory", VARIANT_ID)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStock": 20 }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adjustStock_noExisteDevuelve404() throws Exception {
        doThrow(InventoryNotFoundException.class)
                .when(inventoryService).adjustStock(VARIANT_ID, 20L, SELLER_ID);

        mockMvc.perform(put("/seller/variants/{variantId}/inventory", VARIANT_ID)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStock": 20 }
                                """))
                .andExpect(status().isNotFound());
    }

    // --------------------------------------------------------------- helpers

    private InventoryViewResponse inventoryViewResponse() {
        return new InventoryViewResponse(INVENTORY_ID, 10L, 3L, 5L, 7L);
    }

    private String quantityBody(int quantity) {
        return """
                {
                  "variantId": "%s",
                  "quantity": %d
                }
                """.formatted(VARIANT_ID, quantity);
    }

    private RequestPostProcessor sellerAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.sellerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_SELLER"))));
    }

    private RequestPostProcessor customerAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.customerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private RequestPostProcessor serviceOrderAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.adminJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE_ORDER"))));
    }
}