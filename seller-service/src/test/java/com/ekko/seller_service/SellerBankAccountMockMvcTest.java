package com.ekko.seller_service;

import com.ekko.seller_service.support.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class SellerBankAccountMockMvcTest extends AbstractPostgresIntegrationTest {

    private static final String BANK_ACCOUNT_JSON = """
            {
              "bankName": "Banco Nacional",
              "accountType": "SAVINGS",
              "accountNumber": "123456789",
              "accountHolder": "Vendedor Test"
            }
            """;

    private static final java.util.concurrent.atomic.AtomicInteger ACCOUNT_SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    private String addAccount() throws Exception {
        String payload = """
                {
                  "bankName": "Banco Nacional",
                  "accountType": "SAVINGS",
                  "accountNumber": "9%08d",
                  "accountHolder": "Vendedor Test"
                }
                """.formatted(ACCOUNT_SEQ.incrementAndGet());

        MvcResult result = mockMvc.perform(post("/sellers/bank-accounts")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @Test
    void add_sinToken_devuelve401() throws Exception {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        mockMvc.perform(post("/sellers/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BANK_ACCOUNT_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 Forbidden ──────────────────────────────────────────────────────

    @Test
    void add_rolIncorrecto_devuelve403() throws Exception {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        mockMvc.perform(post("/sellers/bank-accounts")
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BANK_ACCOUNT_JSON))
                .andExpect(status().isForbidden());
    }

    // ── 400 Bad Request ─────────────────────────────────────────────────────

    @Test
    void add_dtoInvalido_devuelve400() throws Exception {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        mockMvc.perform(post("/sellers/bank-accounts")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'bankName')]").exists())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // ── 200 OK ──────────────────────────────────────────────────────────────

    @Test
    void add_tokenValido_devuelve200() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        mockMvc.perform(post("/sellers/bank-accounts")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BANK_ACCOUNT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }

    @Test
    void getMisCuentas_devuelve200() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        addAccount();
        addAccount();

        mockMvc.perform(get("/sellers/bank-accounts")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateCuenta_tokenValido_devuelve200() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        String id = addAccount();

        mockMvc.perform(put("/sellers/bank-accounts/{id}", id)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BANK_ACCOUNT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName").value("Banco Nacional"));
    }

    // ── 409 Conflict ────────────────────────────────────────────────────────

    @Test
    void add_duplicado_devuelve409() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        mockMvc.perform(post("/sellers/bank-accounts")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BANK_ACCOUNT_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/sellers/bank-accounts")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BANK_ACCOUNT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void delete_ultima_devuelve409() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        String id = addAccount();

        mockMvc.perform(delete("/sellers/bank-accounts/{id}", id)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_primaria_devuelve409() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        String primaryId = addAccount();
        addAccount();

        mockMvc.perform(delete("/sellers/bank-accounts/{id}", primaryId)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isConflict());
    }

    // ── 200 OK (set primary) ────────────────────────────────────────────────

    @Test
    void setPrimary_tokenValido_devuelve200() throws Exception {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
        String firstId = addAccount();
        String secondId = addAccount();

        mockMvc.perform(patch("/sellers/bank-accounts/{id}/primary", secondId)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrimary").value(true));
    }
}