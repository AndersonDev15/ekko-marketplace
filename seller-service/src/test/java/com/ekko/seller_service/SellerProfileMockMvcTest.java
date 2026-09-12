package com.ekko.seller_service;

import com.ekko.seller_service.support.JwtTestUtils;
import com.ekko.seller_service.support.SellerTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SellerProfileMockMvcTest extends AbstractPostgresIntegrationTest {

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @Test
    void getMyProfile_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/sellers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyProfile_jwtInvalido_devuelve401() throws Exception {
        mockMvc.perform(get("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.malformedToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyProfile_tokenExpirado_devuelve401() throws Exception {
        mockMvc.perform(get("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.expiredToken()))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 Forbidden ───────────────────────────────────────────────────────

    @Test
    void getMyProfile_rolIncorrecto_devuelve403() throws Exception {
        mockMvc.perform(get("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void getMyProfile_sinRol_devuelve403() throws Exception {
        mockMvc.perform(get("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.noRolesToken()))
                .andExpect(status().isForbidden());
    }

    // ── 200 OK ──────────────────────────────────────────────────────────────

    @Test
    void getMyProfile_tokenValido_autoCreaPerfil_devuelve200() throws Exception {
        mockMvc.perform(get("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakId").value(JwtTestUtils.SELLER_KEYCLOAK_ID))
                .andExpect(jsonPath("$.email").value(JwtTestUtils.SELLER_EMAIL))
                .andExpect(jsonPath("$.storeName").value("Mi tienda"))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    void getMyProfile_tokenValido_vendedorExistente_devuelve200() throws Exception {
        insertSeller(SellerTestDataBuilder.aSeller()
                .withKeycloakId(JwtTestUtils.SELLER_KEYCLOAK_ID)
                .withStoreName("Tienda existente")
                .withEmail("existente@ekko.test")
                .active());

        mockMvc.perform(get("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Tienda existente"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ── 400 Bad Request ─────────────────────────────────────────────────────

    @Test
    void updateMyProfile_dtoInvalido_devuelve400() throws Exception {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);

        mockMvc.perform(put("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("storeName"));
    }

    @Test
    void updateMyProfile_phoneDemasiadoLargo_devuelve400() throws Exception {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);

        mockMvc.perform(put("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName": "Tienda",
                                 "phone": "123456789012345678901"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));
    }

    // ── 200 OK (update) ─────────────────────────────────────────────────────

    @Test
    void updateMyProfile_tokenValido_devuelve200() throws Exception {
        insertActiveSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);

        mockMvc.perform(put("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName": "Tienda nueva",
                                 "phone": "3001234567",
                                 "description": "Nueva descripcion"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Tienda nueva"))
                .andExpect(jsonPath("$.description").value("Nueva descripcion"));
    }

    // ── 404 Not Found ───────────────────────────────────────────────────────

    @Test
    void updateMyProfile_vendedorInexistente_devuelve404() throws Exception {
        mockMvc.perform(put("/sellers/me")
                        .header("Authorization", "Bearer " + JwtTestUtils.sellerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName": "Tienda nueva"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Ruta pública /sellers/{id} ─────────────────────────────────────────

    @Test
    void getPublicProfile_sinToken_devuelve200() throws Exception {
        var seller = insertSeller(SellerTestDataBuilder.aSeller()
                .withStoreName("Tienda publica")
                .withEmail("publica@ekko.test")
                .active());

        mockMvc.perform(get("/sellers/{id}", seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Tienda publica"));
    }

    @Test
    void getPublicProfile_inexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/sellers/{id}", java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
