package com.ekko.seller_service;

import com.ekko.seller_service.support.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SellerAddressMockMvcTest extends AbstractPostgresIntegrationTest {

    private static final String ADDRESS_JSON = """
            {
              "addressLine": "Calle 1 # 2-3",
              "city": "Bogota",
              "state": "Cundinamarca",
              "country": "Colombia",
              "postalCode": "110111"
            }
            """;

    private String addAddress() throws Exception {
        MvcResult result = mockMvc.perform(post("/sellers/addresses")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADDRESS_JSON))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @Test
    void add_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/sellers/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADDRESS_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 Forbidden ───────────────────────────────────────────────────────

    @Test
    void add_rolIncorrecto_devuelve403() throws Exception {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        mockMvc.perform(post("/sellers/addresses")
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADDRESS_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ── 400 Bad Request ─────────────────────────────────────────────────────

    @Test
    void add_dtoInvalido_devuelve400() throws Exception {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        mockMvc.perform(post("/sellers/addresses")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'addressLine')]").exists())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // ── 200 OK ──────────────────────────────────────────────────────────────

    @Test
    void add_tokenValido_devuelve200() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        mockMvc.perform(post("/sellers/addresses")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADDRESS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andExpect(jsonPath("$.city").value("Bogota"));
    }

    @Test
    void getMisDirecciones_devuelve200() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        addAddress();
        addAddress();

        mockMvc.perform(get("/sellers/addresses")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateDireccion_tokenValido_devuelve200() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        String id = addAddress();

        mockMvc.perform(put("/sellers/addresses/{id}", id)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADDRESS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Bogota"));
    }

    // ── 409 Conflict ────────────────────────────────────────────────────────

    @Test
    void delete_ultimaDireccion_devuelve409() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        String id = addAddress();

        mockMvc.perform(delete("/sellers/addresses/{id}", id)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void delete_primaria_devuelve409() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        String primaryId = addAddress();
        addAddress();

        mockMvc.perform(delete("/sellers/addresses/{id}", primaryId)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isConflict());
    }

    // ── 204 No Content ──────────────────────────────────────────────────────

    @Test
    void delete_secundaria_devuelve204() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        String primaryId = addAddress();
        String secondaryId = addAddress();

        mockMvc.perform(delete("/sellers/addresses/{id}", secondaryId)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isNoContent());
    }
}