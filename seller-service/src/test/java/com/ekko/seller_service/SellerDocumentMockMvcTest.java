package com.ekko.seller_service;

import com.ekko.seller_service.support.JwtTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SellerDocumentMockMvcTest extends AbstractPostgresIntegrationTest {

    private static final String DOCUMENT_JSON = """
            {
              "documentType": "ID_CARD",
              "documentUrl": "https://cdn.ekko.test/id_card.pdf"
            }
            """;

    @BeforeEach
    void seedSeller() {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
    }

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @Test
    void add_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/sellers/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOCUMENT_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void add_jwtInvalido_devuelve401() throws Exception {
        mockMvc.perform(post("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.malformedToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOCUMENT_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 Forbidden ───────────────────────────────────────────────────────

    @Test
    void add_rolIncorrecto_devuelve403() throws Exception {
        mockMvc.perform(post("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOCUMENT_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // ── 400 Bad Request ─────────────────────────────────────────────────────

    @Test
    void add_dtoInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'documentType')]").exists())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // ── 200 OK ──────────────────────────────────────────────────────────────

    @Test
    void add_tokenValido_devuelve200() throws Exception {
        mockMvc.perform(post("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOCUMENT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("ID_CARD"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getMisDocumentos_devuelve200() throws Exception {
        addDocument();

        mockMvc.perform(get("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ── 409 Conflict ────────────────────────────────────────────────────────

    @Test
    void add_duplicadoPendiente_devuelve409() throws Exception {
        addDocument();

        mockMvc.perform(post("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOCUMENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void add_documentoAprobado_devuelve409() throws Exception {
        String documentId = addDocument();
        reviewDocument(documentId);

        mockMvc.perform(post("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOCUMENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ── 409 Conflict (re-subida de un tipo ya subido, sin importar estado) ──

    @Test
    void add_documentoRechazado_noPermiteReSubir_devuelve409() throws Exception {
        String documentId = addDocument();

        mockMvc.perform(put("/admin/sellers/documents/{id}/review", documentId)
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "REJECTED", "notes": "Documento ilegible"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOCUMENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    private String addDocument() throws Exception {
        MvcResult result = mockMvc.perform(post("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOCUMENT_JSON))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void reviewDocument(String documentId) throws Exception {
        mockMvc.perform(put("/admin/sellers/documents/{id}/review", documentId)
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "APPROVED", "notes": "ok"}
                                """))
                .andExpect(status().isOk());
    }
}
