package com.ekko.product_service.controller;

import com.ekko.product_service.config.SecurityConfig;
import com.ekko.product_service.dto.response.CategoryNodeResponse;
import com.ekko.product_service.dto.response.CategoryResponse;
import com.ekko.product_service.exception.CategoryDeletionException;
import com.ekko.product_service.exception.CategoryNotFoundException;
import com.ekko.product_service.exception.CyclicCategoryException;
import com.ekko.product_service.exception.DuplicateSlugException;
import com.ekko.product_service.exception.InactiveParentCategoryException;
import com.ekko.product_service.service.CategoryService;
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

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID PARENT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------ GET /categories

    @Test
    void getCategories_publicoDevuelve200YArbol() throws Exception {
        CategoryNodeResponse node = new CategoryNodeResponse(PARENT_ID,
                "Electrónica", "electronica", List.of());
        when(categoryService.getCategoryTree()).thenReturn(List.of(node));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electrónica"))
                .andExpect(jsonPath("$[0].slug").value("electronica"));

        verify(categoryService).getCategoryTree();
    }

    // ------------------------------------------------------ POST /admin/categories

    @Test
    void createCategory_devuelve201YCategoryResponse() throws Exception {
        CategoryResponse response = new CategoryResponse(CATEGORY_ID, "Electrónica", "electronica", true);
        when(categoryService.createCategory(any())).thenReturn(response);

        mockMvc.perform(post("/admin/categories")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Electrónica",
                                  "slug": "electronica"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.name").value("Electrónica"));

        verify(categoryService).createCategory(any());
    }

    @Test
    void createCategory_duplicateSlugDevuelve409() throws Exception {
        when(categoryService.createCategory(any()))
                .thenThrow(DuplicateSlugException.class);

        mockMvc.perform(post("/admin/categories")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Electrónica", "slug": "electronica" }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createCategory_parentNoExisteDevuelve404() throws Exception {
        when(categoryService.createCategory(any()))
                .thenThrow(CategoryNotFoundException.class);

        mockMvc.perform(post("/admin/categories")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Electrónica", "slug": "electronica", "parentId": "%s" }
                                """.formatted(PARENT_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory_parentInactivoDevuelve409() throws Exception {
        when(categoryService.createCategory(any()))
                .thenThrow(InactiveParentCategoryException.class);

        mockMvc.perform(post("/admin/categories")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Electrónica", "slug": "electronica", "parentId": "%s" }
                                """.formatted(PARENT_ID)))
                .andExpect(status().isConflict());
    }

    @Test
    void createCategory_sinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Electrónica", "slug": "electronica" }
                                """))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any());
    }

    // ------------------------------------------------------ PUT /admin/categories/{id}

    @Test
    void updateCategory_devuelve200YCategoryResponse() throws Exception {
        CategoryResponse category = new CategoryResponse(CATEGORY_ID, "Tecnología", "tecnologia", true);
        when(categoryService.updateCategory(eq(CATEGORY_ID), any())).thenReturn(category);

        mockMvc.perform(put("/admin/categories/{id}", CATEGORY_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Tecnología", "slug": "tecnologia" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tecnología"));

        verify(categoryService).updateCategory(eq(CATEGORY_ID), any());
    }

    @Test
    void updateCategory_noExisteDevuelve404() throws Exception {
        doThrow(CategoryNotFoundException.class)
                .when(categoryService).updateCategory(eq(CATEGORY_ID), any());

        mockMvc.perform(put("/admin/categories/{id}", CATEGORY_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Tecnología", "slug": "tecnologia" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCategory_duplicateSlugDevuelve409() throws Exception {
        doThrow(DuplicateSlugException.class)
                .when(categoryService).updateCategory(eq(CATEGORY_ID), any());

        mockMvc.perform(put("/admin/categories/{id}", CATEGORY_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Tecnología", "slug": "tecnologia" }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCategory_cicloDevuelve409() throws Exception {
        doThrow(CyclicCategoryException.class)
                .when(categoryService).updateCategory(eq(CATEGORY_ID), any());

        mockMvc.perform(put("/admin/categories/{id}", CATEGORY_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Tecnología", "slug": "tecnologia" }
                                """))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------ DELETE /admin/categories/{id}

    @Test
    void deleteCategory_devuelve204() throws Exception {
        mockMvc.perform(delete("/admin/categories/{id}", CATEGORY_ID)
                        .with(adminAuth()))
                .andExpect(status().isNoContent());

        verify(categoryService).softDeleteCategory(CATEGORY_ID);
    }

    @Test
    void deleteCategory_noExisteDevuelve404() throws Exception {
        doThrow(CategoryNotFoundException.class)
                .when(categoryService).softDeleteCategory(CATEGORY_ID);

        mockMvc.perform(delete("/admin/categories/{id}", CATEGORY_ID)
                        .with(adminAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_delecionDenegadaDevuelve409() throws Exception {
        doThrow(CategoryDeletionException.class)
                .when(categoryService).softDeleteCategory(CATEGORY_ID);

        mockMvc.perform(delete("/admin/categories/{id}", CATEGORY_ID)
                        .with(adminAuth()))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteCategory_sinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(delete("/admin/categories/{id}", CATEGORY_ID)
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).softDeleteCategory(CATEGORY_ID);
    }

    // --------------------------------------------------------------- helpers

    private RequestPostProcessor adminAuth() {
        return JwtTestUtils.adminAuth(jwtAuthenticationConverter);
    }

    private RequestPostProcessor sellerAuth() {
        return JwtTestUtils.sellerAuth(jwtAuthenticationConverter);
    }
}