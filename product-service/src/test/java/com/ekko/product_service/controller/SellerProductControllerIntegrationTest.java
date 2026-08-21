package com.ekko.product_service.controller;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductImageTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.builder.ProductVariantTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductImageRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import com.ekko.product_service.util.JwtTestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class SellerProductControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String BASE_URL = "/seller/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private final UUID otherSellerId = CUSTOMER_KEYCLOAK_ID;

    // ------------------------------------------------------ POST /seller/products

    @Test
    void postProduct_retorna201CreatedCuandoLaPeticionEsValida() throws Exception {
        Brand brand = persistBrand();
        Category category = persistCategory();
        String json = createJson(PRODUCT_NAME, brand.getId(), category.getId());

        mockMvc.perform(post(BASE_URL)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(PRODUCT_NAME))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.sellerKeycloakId").value(SELLER_KEYCLOAK_ID.toString()));
    }

    @Test
    void postProduct_creaElProductoConEstadoDraft() throws Exception {
        Brand brand = persistBrand();
        Category category = persistCategory();
        String json = createJson(PRODUCT_NAME, brand.getId(), category.getId());

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UUID productId = extractId(result);
        Product saved = productRepository.findById(productId).orElseThrow();
        assertEquals(ProductStatus.DRAFT, saved.getStatus());
        assertEquals(PRODUCT_NAME, saved.getName());
        assertEquals(SELLER_KEYCLOAK_ID, saved.getSellerKeycloakId());
        assertEquals(brand.getId(), saved.getBrand().getId());
        assertEquals(category.getId(), saved.getCategory().getId());
    }

    @Test
    void postProduct_retorna401SinAutenticacion() throws Exception {
        Brand brand = persistBrand();
        Category category = persistCategory();
        String json = createJson(PRODUCT_NAME, brand.getId(), category.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postProduct_retorna403ParaUsuariosQueNoSonSeller() throws Exception {
        Brand brand = persistBrand();
        Category category = persistCategory();
        String json = createJson(PRODUCT_NAME, brand.getId(), category.getId());

        mockMvc.perform(post(BASE_URL)
                        .with(customerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void postProduct_retorna400CuandoElRequestEsInvalido() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------ GET /seller/products

    @Test
    void getMyProducts_retorna200OK() throws Exception {
        persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.DRAFT, null);

        mockMvc.perform(get(BASE_URL)
                        .with(sellerAuth()))
                .andExpect(status().isOk());
    }

    @Test
    void getMyProducts_retornaSoloLosProductosDelVendedorAutenticado() throws Exception {
        persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, "iPhone 15", uniqueSlug("iphone-15"),
                ProductStatus.DRAFT, null);
        persistProduct(otherSellerId, "Galaxy", uniqueSlug("galaxy"), ProductStatus.DRAFT, null);

        mockMvc.perform(get(BASE_URL)
                        .with(sellerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].sellerKeycloakId",
                        everyItem(equalTo(SELLER_KEYCLOAK_ID.toString()))));
    }

    @Test
    void getMyProducts_respetaLaPaginacion() throws Exception {
        persistProduct(SELLER_KEYCLOAK_ID, "A", uniqueSlug("a"), ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, "B", uniqueSlug("b"), ProductStatus.DRAFT, null);
        persistProduct(SELLER_KEYCLOAK_ID, "C", uniqueSlug("c"), ProductStatus.DRAFT, null);

        mockMvc.perform(get(BASE_URL)
                        .with(sellerAuth())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0));

        mockMvc.perform(get(BASE_URL)
                        .with(sellerAuth())
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void getMyProducts_retornaListaVaciaCuandoNoExistenProductos() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(sellerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getMyProducts_retorna401SinAutenticacion() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------ GET /seller/products/{id}

    @Test
    void getProductById_retorna200OKCuandoElProductoPerteneceAlVendedor() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(get(BASE_URL + "/" + product.getId())
                        .with(sellerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.name").value(PRODUCT_NAME))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getProductById_retorna404CuandoElProductoNoExiste() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                        .with(sellerAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductById_retorna403CuandoElProductoPerteneceAOtroVendedor() throws Exception {
        Product product = persistProduct(otherSellerId, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(get(BASE_URL + "/" + product.getId())
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProductById_retorna401SinAutenticacion() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(get(BASE_URL + "/" + product.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------ PUT /seller/products/{id}

    @Test
    void updateProduct_retorna200OKCuandoLaActualizacionEsValida() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        String json = """
                {
                  "description": "nueva descripcion"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/" + product.getId())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("nueva descripcion"))
                .andExpect(jsonPath("$.name").value(PRODUCT_NAME));
    }

    @Test
    void updateProduct_actualizaLosCamposEnviados() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        String json = """
                {
                  "name": "Nuevo Nombre",
                  "description": "nueva descripcion"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/" + product.getId())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo Nombre"))
                .andExpect(jsonPath("$.slug").value("nuevo-nombre"));

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertEquals("Nuevo Nombre", saved.getName());
        assertEquals("nuevo-nombre", saved.getSlug());
        assertEquals("nueva descripcion", saved.getDescription());
    }

    @Test
    void updateProduct_retorna404CuandoElProductoNoExiste() throws Exception {
        String json = """
                {
                  "description": "nueva descripcion"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/" + UUID.randomUUID())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_retorna403CuandoElProductoPerteneceAOtroVendedor() throws Exception {
        Product product = persistProduct(otherSellerId, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        String json = """
                {
                  "description": "nueva descripcion"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/" + product.getId())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProduct_retorna400CuandoElRequestEsInvalido() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(put(BASE_URL + "/" + product.getId())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_retorna401SinAutenticacion() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        String json = """
                {
                  "description": "nueva descripcion"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------ DELETE /seller/products/{id}

    @Test
    void deleteProduct_retorna204NoContent() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(delete(BASE_URL + "/" + product.getId())
                        .with(sellerAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_realizaElSoftDeleteEnLaBaseDeDatos() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        ProductVariant variant = addVariant(product, true);

        mockMvc.perform(delete(BASE_URL + "/" + product.getId())
                        .with(sellerAuth()))
                .andExpect(status().isNoContent());

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertNotNull(saved.getDeletedAt());
        assertEquals(false, productVariantRepository.findById(variant.getId()).orElseThrow().getIsActive());
    }

    @Test
    void deleteProduct_retorna404CuandoElProductoNoExiste() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID())
                        .with(sellerAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_retorna403CuandoElProductoPerteneceAOtroVendedor() throws Exception {
        Product product = persistProduct(otherSellerId, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(delete(BASE_URL + "/" + product.getId())
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_retorna401SinAutenticacion() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(delete(BASE_URL + "/" + product.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------ POST /seller/products/{id}/submit

    @Test
    void submitForReview_retorna204CuandoElProductoPasaAPendingReview() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);
        addVariant(product, true);
        addImage(product, true);

        mockMvc.perform(post(BASE_URL + "/" + product.getId() + "/submit")
                        .with(sellerAuth()))
                .andExpect(status().isNoContent());

        assertEquals(ProductStatus.PENDING_REVIEW,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void submitForReview_retorna409CuandoElEstadoNoEsValido() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.ACTIVE, null);
        addVariant(product, true);
        addImage(product, true);

        mockMvc.perform(post(BASE_URL + "/" + product.getId() + "/submit")
                        .with(sellerAuth()))
                .andExpect(status().isConflict());

        assertEquals(ProductStatus.ACTIVE,
                productRepository.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void submitForReview_retorna404CuandoElProductoNoExiste() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/submit")
                        .with(sellerAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitForReview_retorna403CuandoElProductoPerteneceAOtroVendedor() throws Exception {
        Product product = persistProduct(otherSellerId, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(post(BASE_URL + "/" + product.getId() + "/submit")
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitForReview_retorna401SinAutenticacion() throws Exception {
        Product product = persistProduct(SELLER_KEYCLOAK_ID, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.DRAFT, null);

        mockMvc.perform(post(BASE_URL + "/" + product.getId() + "/submit"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------- helpers

    private RequestPostProcessor sellerAuth() {
        return JwtTestUtils.sellerAuth(jwtAuthenticationConverter);
    }

    private RequestPostProcessor customerAuth() {
        return JwtTestUtils.customerAuth(jwtAuthenticationConverter);
    }

    private String createJson(String name, UUID brandId, UUID categoryId) {
        return """
                {
                  "name": "%s",
                  "description": "Smartphone de alta gama",
                  "brandId": "%s",
                  "categoryId": "%s"
                }
                """.formatted(name, brandId, categoryId);
    }

    private UUID extractId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(node.get("id").asText());
    }

    private String uniqueSlug(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
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

    private Product persistProduct(UUID sellerKeycloakId, String name, String slug,
                                   ProductStatus status, LocalDateTime deletedAt) {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(persistBrand())
                .withCategory(persistCategory())
                .withSellerKeycloakId(sellerKeycloakId)
                .withName(name)
                .withSlug(slug)
                .withStatus(status)
                .withDeletedAt(deletedAt)
                .build();
        return productRepository.save(product);
    }

    private ProductImage addImage(Product product, boolean isPrimary) {
        ProductImage image = ProductImageTestDataBuilder.anImage()
                .withId(null)
                .withProduct(product)
                .withUrl("https://cdn.example.com/" + UUID.randomUUID() + ".jpg")
                .withIsPrimary(isPrimary)
                .build();
        image = productImageRepository.save(image);
        product.getImages().add(image);
        return image;
    }

    private ProductVariant addVariant(Product product, boolean isActive) {
        ProductVariant variant = ProductVariantTestDataBuilder.aVariant()
                .withId(null)
                .withProduct(product)
                .withSku("SKU-" + UUID.randomUUID().toString().substring(0, 8))
                .withIsActive(isActive)
                .build();
        variant = productVariantRepository.save(variant);
        product.getVariants().add(variant);
        return variant;
    }
}
