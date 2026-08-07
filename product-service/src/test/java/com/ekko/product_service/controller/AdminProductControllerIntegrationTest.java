package com.ekko.product_service.controller;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.util.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class AdminProductControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String BASE_URL = "/admin/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // ------------------------------------------------------ GET /admin/products

    @Test
    void getAllProducts_retorna200OKCuandoElUsuarioEsAdmin() throws Exception {
        persistProduct(ProductStatus.DRAFT);

        mockMvc.perform(get(BASE_URL)
                        .with(adminAuth()))
                .andExpect(status().isOk());
    }

    @Test
    void getAllProducts_retornaProductosPaginados() throws Exception {
        persistProduct(ProductStatus.DRAFT);
        persistProduct(ProductStatus.DRAFT);
        persistProduct(ProductStatus.ACTIVE);
        persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(get(BASE_URL)
                        .with(adminAuth())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void getAllProducts_respetaElNumeroDePaginaYElTamano() throws Exception {
        for (int i = 0; i < 5; i++) {
            persistProduct(ProductStatus.DRAFT);
        }

        mockMvc.perform(get(BASE_URL)
                        .with(adminAuth())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5));

        mockMvc.perform(get(BASE_URL)
                        .with(adminAuth())
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number").value(1));

        mockMvc.perform(get(BASE_URL)
                        .with(adminAuth())
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.number").value(2));
    }

    @Test
    void getAllProducts_retornaPaginaVaciaCuandoNoExistenProductos() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllProducts_retorna401SinAutenticacion() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllProducts_retorna403ParaRolSeller() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllProducts_retorna403ParaRolCustomer() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ PATCH /admin/products/{id}/approve

    @Test
    void approveProduct_retorna204CuandoElProductoEstaEnPendingReview() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/approve")
                        .with(adminAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void approveProduct_cambiaElEstadoAActive() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/approve")
                        .with(adminAuth()))
                .andExpect(status().isNoContent());

        assertEquals(ProductStatus.ACTIVE,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void approveProduct_persisteElCambioEnLaBaseDeDatos() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/approve")
                        .with(adminAuth()))
                .andExpect(status().isNoContent());

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(PRODUCT_NAME, saved.getName());
        assertEquals(SELLER_KEYCLOAK_ID, saved.getSellerKeycloakId());
    }

    @Test
    void approveProduct_retorna404CuandoElProductoNoExiste() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + UUID.randomUUID() + "/approve")
                        .with(adminAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveProduct_retorna409CuandoElProductoNoEstaEnPendingReview() throws Exception {
        Product product = persistProduct(ProductStatus.DRAFT);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/approve")
                        .with(adminAuth()))
                .andExpect(status().isConflict());

        assertEquals(ProductStatus.DRAFT,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void approveProduct_retorna401SinAutenticacion() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveProduct_retorna403ParaRolSeller() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/approve")
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveProduct_retorna403ParaRolCustomer() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/approve")
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ PATCH /admin/products/{id}/reject

    @Test
    void rejectProduct_retorna204CuandoElProductoEstaEnPendingReview() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/reject")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "marca prohibida"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectProduct_cambiaElEstadoARejected() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/reject")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "marca prohibida"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertEquals(ProductStatus.REJECTED,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void rejectProduct_persisteElCambioEnLaBaseDeDatos() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/reject")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "marca prohibida"
                                }
                                """))
                .andExpect(status().isNoContent());

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.REJECTED, saved.getStatus());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(PRODUCT_NAME, saved.getName());
        assertEquals(SELLER_KEYCLOAK_ID, saved.getSellerKeycloakId());
    }

    @Test
    void rejectProduct_retorna404CuandoElProductoNoExiste() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + UUID.randomUUID() + "/reject")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "marca prohibida"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectProduct_retorna409CuandoElProductoNoEstaEnPendingReview() throws Exception {
        Product product = persistProduct(ProductStatus.ACTIVE);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/reject")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "marca prohibida"
                                }
                                """))
                .andExpect(status().isConflict());

        assertEquals(ProductStatus.ACTIVE,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void rejectProduct_retorna400CuandoElMotivoDeRechazoEsInvalido() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/reject")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        assertEquals(ProductStatus.PENDING_REVIEW,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void rejectProduct_retorna401SinAutenticacion() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "marca prohibida"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectProduct_retorna403ParaRolSeller() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/reject")
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "marca prohibida"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectProduct_retorna403ParaRolCustomer() throws Exception {
        Product product = persistProduct(ProductStatus.PENDING_REVIEW);

        mockMvc.perform(patch(BASE_URL + "/" + product.getId() + "/reject")
                        .with(customerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "marca prohibida"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------- helpers

    private RequestPostProcessor adminAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.adminJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private RequestPostProcessor sellerAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.sellerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_SELLER"))));
    }

    private RequestPostProcessor customerAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.customerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private Brand persistBrand() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Brand brand = BrandTestDataBuilder.aBrand()
                .withId(null)
                .withName("Brand-" + suffix)
                .withSlug("brand-" + suffix)
                .build();
        return brandRepository.save(brand);
    }

    private Category persistCategory() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Category category = CategoryTestDataBuilder.aCategory()
                .withId(null)
                .withSlug("category-" + suffix)
                .build();
        return categoryRepository.save(category);
    }

    private Product persistProduct(ProductStatus status) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(persistBrand())
                .withCategory(persistCategory())
                .withSellerKeycloakId(SELLER_KEYCLOAK_ID)
                .withName(PRODUCT_NAME)
                .withSlug(PRODUCT_SLUG + "-" + suffix)
                .withStatus(status)
                .build();
        return productRepository.save(product);
    }
}
