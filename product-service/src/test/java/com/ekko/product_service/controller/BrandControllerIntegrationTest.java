package com.ekko.product_service.controller;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.util.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class BrandControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private BrandRepository brandRepository;

    // ------------------------------------------------------ GET /brands

    @Test
    void getBrands_retornaSoloMarcasActivas() throws Exception {
        persistBrand("Apple", "apple");
        persistBrand("Inactiva", "inactiva").setIsActive(false);

        mockMvc.perform(get("/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Apple"));
    }

    // ------------------------------------------------------ POST /admin/brands

    @Test
    void createBrand_retorna201() throws Exception {
        mockMvc.perform(post("/admin/brands")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Samsung",
                                  "slug": "samsung",
                                  "logoUrl": "https://cdn.example.com/samsung.png"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Samsung"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void createBrand_nombreDuplicadoRetorna409() throws Exception {
        persistBrand("Apple", "apple");

        mockMvc.perform(post("/admin/brands")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Apple", "slug": "apple-2" }
                                """))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------ PUT /admin/brands/{id}

    @Test
    void updateBrand_retorna200() throws Exception {
        UUID id = persistBrand("Apple", "apple").getId();

        mockMvc.perform(put("/admin/brands/{id}", id)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Apple Inc", "slug": "apple-inc" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Apple Inc"))
                .andExpect(jsonPath("$.slug").value("apple-inc"));
    }

    @Test
    void updateBrand_noExisteRetorna404() throws Exception {
        mockMvc.perform(put("/admin/brands/{id}", UUID.randomUUID())
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Apple Inc", "slug": "apple-inc" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBrand_sinRolAdminRetorna403() throws Exception {
        mockMvc.perform(put("/admin/brands/{id}", UUID.randomUUID())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Apple Inc", "slug": "apple-inc" }
                                """))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ DELETE /admin/brands/{id}

    @Test
    void deleteBrand_retorna204YDesactiva() throws Exception {
        UUID id = persistBrand("Apple", "apple").getId();

        mockMvc.perform(delete("/admin/brands/{id}", id)
                        .with(adminAuth()))
                .andExpect(status().isNoContent());

        assertFalse(brandRepository.findById(id).orElseThrow().getIsActive());
    }

    // --------------------------------------------------------------- helpers

    private RequestPostProcessor adminAuth() {
        return JwtTestUtils.adminAuth(jwtAuthenticationConverter);
    }

    private RequestPostProcessor sellerAuth() {
        return JwtTestUtils.sellerAuth(jwtAuthenticationConverter);
    }

    private Brand persistBrand(String name, String slug) {
        return brandRepository.save(BrandTestDataBuilder.aBrand()
                .withId(null)
                .withName(name)
                .withSlug(slug + "-" + UUID.randomUUID().toString().substring(0, 8))
                .build());
    }
}