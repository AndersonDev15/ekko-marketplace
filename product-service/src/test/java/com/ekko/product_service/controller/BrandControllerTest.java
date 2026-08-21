package com.ekko.product_service.controller;

import com.ekko.product_service.config.SecurityConfig;
import com.ekko.product_service.dto.response.BrandResponse;
import com.ekko.product_service.exception.BrandNotFoundException;
import com.ekko.product_service.exception.DuplicateBrandNameException;
import com.ekko.product_service.exception.DuplicateBrandSlugException;
import com.ekko.product_service.service.BrandService;
import com.ekko.product_service.util.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrandController.class)
@Import(SecurityConfig.class)
class BrandControllerTest {

    private static final UUID BRAND_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------ GET /brands

    @Test
    void getBrands_publicoDevuelve200YLista() throws Exception {
        BrandResponse brand = new BrandResponse(BRAND_ID, "Apple", "apple",
                "https://cdn.example.com/apple.png", "Apple brand", true);
        when(brandService.getActiveBrands()).thenReturn(List.of(brand));

        mockMvc.perform(get("/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[0].slug").value("apple"));

        verify(brandService).getActiveBrands();
    }

    // ------------------------------------------------------ POST /admin/brands

    @Test
    void createBrand_devuelve201YBrandResponse() throws Exception {
        BrandResponse brand = new BrandResponse(BRAND_ID, "Apple", "apple",
                "https://cdn.example.com/apple.png", "Apple brand", true);
        when(brandService.createBrand(any())).thenReturn(brand);

        mockMvc.perform(post("/admin/brands")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Apple",
                                  "slug": "apple"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(BRAND_ID.toString()))
                .andExpect(jsonPath("$.name").value("Apple"));

        verify(brandService).createBrand(any());
    }

    @Test
    void createBrand_nombreDuplicadoDevuelve409() throws Exception {
        when(brandService.createBrand(any()))
                .thenThrow(DuplicateBrandNameException.class);

        mockMvc.perform(post("/admin/brands")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Apple", "slug": "apple" }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createBrand_slugDuplicadoDevuelve409() throws Exception {
        when(brandService.createBrand(any()))
                .thenThrow(DuplicateBrandSlugException.class);

        mockMvc.perform(post("/admin/brands")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Apple", "slug": "apple" }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createBrand_sinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(post("/admin/brands")
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Apple", "slug": "apple" }
                                """))
                .andExpect(status().isForbidden());

        verify(brandService, never()).createBrand(any());
    }

    // ------------------------------------------------------ PUT /admin/brands/{id}

    @Test
    void updateBrand_devuelve200YBrandResponse() throws Exception {
        BrandResponse brand = new BrandResponse(BRAND_ID, "Samsung", "samsung",
                "https://cdn.example.com/samsung.png", "Samsung brand", true);
        when(brandService.updateBrand(eq(BRAND_ID), any())).thenReturn(brand);

        mockMvc.perform(put("/admin/brands/{id}", BRAND_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Samsung", "slug": "samsung" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Samsung"));

        verify(brandService).updateBrand(eq(BRAND_ID), any());
    }

    @Test
    void updateBrand_noExisteDevuelve404() throws Exception {
        doThrow(BrandNotFoundException.class)
                .when(brandService).updateBrand(eq(BRAND_ID), any());

        mockMvc.perform(put("/admin/brands/{id}", BRAND_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Samsung", "slug": "samsung" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBrand_nombreDuplicadoDevuelve409() throws Exception {
        doThrow(DuplicateBrandNameException.class)
                .when(brandService).updateBrand(eq(BRAND_ID), any());

        mockMvc.perform(put("/admin/brands/{id}", BRAND_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Samsung", "slug": "samsung" }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateBrand_sinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(put("/admin/brands/{id}", BRAND_ID)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Samsung", "slug": "samsung" }
                                """))
                .andExpect(status().isForbidden());

        verify(brandService, never()).updateBrand(eq(BRAND_ID), any());
    }

    // ------------------------------------------------------ DELETE /admin/brands/{id}

    @Test
    void deleteBrand_devuelve204() throws Exception {
        mockMvc.perform(delete("/admin/brands/{id}", BRAND_ID)
                        .with(adminAuth()))
                .andExpect(status().isNoContent());

        verify(brandService).deactivateBrand(BRAND_ID);
    }

    @Test
    void deleteBrand_noExisteDevuelve404() throws Exception {
        doThrow(BrandNotFoundException.class)
                .when(brandService).deactivateBrand(BRAND_ID);

        mockMvc.perform(delete("/admin/brands/{id}", BRAND_ID)
                        .with(adminAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBrand_sinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(delete("/admin/brands/{id}", BRAND_ID)
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());

        verify(brandService, never()).deactivateBrand(BRAND_ID);
    }

    // --------------------------------------------------------------- helpers

    private RequestPostProcessor adminAuth() {
        return JwtTestUtils.adminAuth(jwtAuthenticationConverter);
    }

    private RequestPostProcessor sellerAuth() {
        return JwtTestUtils.sellerAuth(jwtAuthenticationConverter);
    }
}