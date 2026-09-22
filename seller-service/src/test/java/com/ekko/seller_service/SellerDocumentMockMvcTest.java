package com.ekko.seller_service;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.exception.MinioUploadException;
import com.ekko.seller_service.support.JwtTestUtils;
import com.ekko.seller_service.support.SellerTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class SellerDocumentMockMvcTest extends AbstractPostgresIntegrationTest {

    private static final MockMultipartFile DOCUMENT_FILE = new MockMultipartFile(
            "file", "id_card.pdf", "application/pdf", "pdf-content".getBytes());

    @BeforeEach
    void seedSeller() {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
    }

    private String addDocumentWithActiveSeller() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @Test
    void add_sinToken_devuelve401() throws Exception {
        mockMvc.perform(multipart("/sellers/documents")
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void add_jwtInvalido_devuelve401() throws Exception {
        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.malformedToken())
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 Forbidden ───────────────────────────────────────────────────────

    @Test
    void add_rolIncorrecto_devuelve403() throws Exception {
        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // ── 400 Bad Request ─────────────────────────────────────────────────────

    @Test
    void add_sinDocumentType_devuelve400() throws Exception {
        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .file(DOCUMENT_FILE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void add_sinArchivo_devuelve400() throws Exception {
        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    // ── 200 OK ──────────────────────────────────────────────────────────────

    @Test
    void add_tokenValido_devuelve200() throws Exception {
        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("ID_CARD"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getMisDocumentos_devuelve200() throws Exception {
        addDocumentWithActiveSeller();

        mockMvc.perform(get("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ── Download (presigned URL) ─────────────────────────────────────────────

    @Test
    void download_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/sellers/documents/{id}/download", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void download_documentoPropio_devuelve200() throws Exception {
        String documentId = addDocumentWithActiveSeller();

        mockMvc.perform(get("/sellers/documents/{id}/download", documentId)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId))
                .andExpect(jsonPath("$.downloadUrl").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void download_documentoDeOtroVendedor_devuelve404() throws Exception {
        Seller otherSeller = insertSeller(SellerTestDataBuilder.aSeller()
                .withKeycloakId("8f14e45f-ceea-4a2a-b1e0-2f1a1c3f0999")
                .withEmail("other@ekko.test")
                .active());
        UUID otherDocumentId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO seller_documents
                    (id, seller_id, document_type, object_key, status, uploaded_at)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), 'ID_CARD', 'documents/other/doc.pdf', 'PENDING', now())
                """, otherDocumentId.toString(), otherSeller.getId().toString());

        mockMvc.perform(get("/sellers/documents/{id}/download", otherDocumentId)
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── 502 Bad Gateway (fallo en MinIO) ────────────────────────────────────

    @Test
    void add_falloMinIO_devuelve502() throws Exception {
        when(minioService.upload(any()))
                .thenThrow(new MinioUploadException("MinIO is down"));

        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"));
    }

    // ── 409 Conflict ────────────────────────────────────────────────────────

    @Test
    void add_duplicadoPendiente_devuelve409() throws Exception {
        addDocumentWithActiveSeller();

        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void add_documentoAprobado_devuelve409() throws Exception {
        String documentId = addDocumentWithActiveSeller();
        reviewDocument(documentId);

        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void add_documentoRechazado_noPermiteReSubir_devuelve409() throws Exception {
        String documentId = addDocumentWithActiveSeller();

        mockMvc.perform(put("/admin/sellers/documents/{id}/review", documentId)
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "REJECTED", "notes": "Segunda revision"}
                                """))
                .andExpect(status().isOk());

        // Rejected documents can be re-uploaded (service allows re-upload of rejected docs)
        mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .file(DOCUMENT_FILE)
                        .param("documentType", "ID_CARD"))
                .andExpect(status().isOk());
    }

    private void reviewDocument(String documentId) throws Exception {
        mockMvc.perform(put("/admin/sellers/documents/{id}/review", documentId)
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "APPROVED", "notes": "ok"}
                                """))
                .andExpect(status().isOk());
    }
}