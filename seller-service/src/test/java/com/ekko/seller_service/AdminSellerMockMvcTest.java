package com.ekko.seller_service;

import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.support.JwtTestUtils;
import com.ekko.seller_service.support.SellerTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdminSellerMockMvcTest extends AbstractPostgresIntegrationTest {

    @BeforeEach
    void seedSeller() {
        insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
    }

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @Test
    void getAllSellers_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/admin/sellers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllSellers_jwtInvalido_devuelve401() throws Exception {
        mockMvc.perform(get("/admin/sellers")
                        .header("Authorization", "Bearer " + JwtTestUtils.malformedToken()))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 Forbidden ───────────────────────────────────────────────────────

    @Test
    void getAllSellers_rolIncorrecto_devuelve403() throws Exception {
        mockMvc.perform(get("/admin/sellers")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // ── 200 OK ──────────────────────────────────────────────────────────────

    @Test
    void getAllSellers_tokenValido_devuelve200() throws Exception {
        insertSeller(SellerTestDataBuilder.aSeller()
                .withStoreName("Tienda 2")
                .withEmail("tienda2@ekko.test")
                .withStatus(SellerStatus.PENDING_REVIEW));

        mockMvc.perform(get("/admin/sellers")
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

@Test
    void getAllSellers_filtroPorEstado_devuelve200() throws Exception {
        // Seed seller is PENDING_REVIEW (from @BeforeEach), plus this one = 2 PENDING_REVIEW
        insertSeller(SellerTestDataBuilder.aSeller()
                .withStoreName("Tienda 2")
                .withEmail("tienda2@ekko.test")
                .withStatus(SellerStatus.PENDING_REVIEW));

        mockMvc.perform(get("/admin/sellers")
                        .param("status", "PENDING_REVIEW")
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value("PENDING_REVIEW"));
    }

    @Test
    void getSellerDetail_tokenValido_devuelve200() throws Exception {
        var seller = insertSeller(SellerTestDataBuilder.aSeller()
                .withStoreName("Tienda detalle")
                .withEmail("detalle@ekko.test")
                .active());
        insertSellerMetrics(seller.getId());

        mockMvc.perform(get("/admin/sellers/{id}", seller.getId())
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Tienda detalle"))
                .andExpect(jsonPath("$.metrics.totalSales").value(0));
    }

    @Test
    void updateSellerStatus_pendienteAActivo_devuelve200() throws Exception {
        var seller = insertSeller(SellerTestDataBuilder.aSeller()
                .withStoreName("Tienda status")
                .withEmail("status@ekko.test")
                .pendingReview());

        mockMvc.perform(put("/admin/sellers/{id}/status", seller.getId())
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void reviewDocument_pendienteAAprobado_devuelve200() throws Exception {
        String documentId = addDocument();

        mockMvc.perform(put("/admin/sellers/documents/{id}/review", documentId)
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "APPROVED", "notes": "Verificado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.notes").value("Verificado"));
    }

    // ── 400 Bad Request ─────────────────────────────────────────────────────

    @Test
    void updateSellerStatus_dtoInvalido_devuelve400() throws Exception {
        var seller = insertSeller(SellerTestDataBuilder.aSeller()
                .withStoreName("Tienda status")
                .withEmail("status400@ekko.test")
                .pendingReview());

        mockMvc.perform(put("/admin/sellers/{id}/status", seller.getId())
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    // ── 409 Conflict ────────────────────────────────────────────────────────

    @Test
    void updateSellerStatus_transicionInvalida_devuelve409() throws Exception {
        var seller = insertSeller(SellerTestDataBuilder.aSeller()
                .withStoreName("Tienda status")
                .withEmail("status409@ekko.test")
                .active());

        mockMvc.perform(put("/admin/sellers/{id}/status", seller.getId())
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PENDING_REVIEW"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void reviewDocument_documentoRevisado_devuelve409() throws Exception {
        String documentId = addDocument();

        reviewDocument(documentId);
        mockMvc.perform(put("/admin/sellers/documents/{id}/review", documentId)
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "REJECTED", "notes": "Segunda revision"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ── 404 Not Found ───────────────────────────────────────────────────────

    @Test
    void getSellerDetail_inexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/admin/sellers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String addDocument() throws Exception {
        var result = mockMvc.perform(multipart("/sellers/documents")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .file(new MockMultipartFile(
                                "file", "id_card.pdf", "application/pdf", "pdf-content".getBytes()))
                        .param("documentType", "ID_CARD"))
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
