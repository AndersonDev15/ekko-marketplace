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
import com.ekko.product_service.entity.ProductAttribute;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductAttributeRepository;
import com.ekko.product_service.repository.ProductImageRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_NAME;
import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_VALUE;
import static com.ekko.product_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.SELLER_SLUG;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class PublicProductControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String BASE_URL = "/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductAttributeRepository productAttributeRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    // ------------------------------------------------------ GET /products

    @Test
    void getCatalog_retorna200OK() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, uniqueSlug(PRODUCT_SLUG),
                ProductStatus.ACTIVE, null);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    void getCatalog_retornaUnicamenteProductosActivos() throws Exception {
        persistProduct(SELLER_SLUG, "iPhone 16", uniqueSlug("iphone-16"),
                ProductStatus.ACTIVE, null);
        persistProduct(SELLER_SLUG, "iPhone 15", uniqueSlug("iphone-15"),
                ProductStatus.ACTIVE, null);
        persistProduct(SELLER_SLUG, "iPhone SE", uniqueSlug("iphone-se"),
                ProductStatus.DRAFT, null);
        persistProduct(SELLER_SLUG, "iPhone 14", uniqueSlug("iphone-14"),
                ProductStatus.PENDING_REVIEW, null);
        persistProduct(SELLER_SLUG, "iPhone 13", uniqueSlug("iphone-13"),
                ProductStatus.REJECTED, null);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getCatalog_noRetornaProductosEliminados() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, uniqueSlug(PRODUCT_SLUG),
                ProductStatus.ACTIVE, null);
        persistProduct(SELLER_SLUG, "iPhone 15", uniqueSlug("iphone-15"),
                ProductStatus.ACTIVE, LocalDateTime.now());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value(PRODUCT_NAME));
    }

    @Test
    void getCatalog_respetaLaPaginacion() throws Exception {
        for (int i = 0; i < 5; i++) {
            persistProduct(SELLER_SLUG, "iPhone " + i, uniqueSlug("iphone-" + i),
                    ProductStatus.ACTIVE, null);
        }

        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.number").value(0));

        mockMvc.perform(get(BASE_URL)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number").value(1));

        mockMvc.perform(get(BASE_URL)
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.number").value(2));
    }

    @Test
    void getCatalog_retornaListaVaciaCuandoNoExistenProductosPublicos() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getCatalog_noRequiereAutenticacion() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, uniqueSlug(PRODUCT_SLUG),
                ProductStatus.ACTIVE, null);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getCatalog_ignoraProductosEnEstadoDraft() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, uniqueSlug(PRODUCT_SLUG),
                ProductStatus.DRAFT, null);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getCatalog_ignoraProductosEnEstadoPendingReview() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, uniqueSlug(PRODUCT_SLUG),
                ProductStatus.PENDING_REVIEW, null);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getCatalog_ignoraProductosEnEstadoRejected() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, uniqueSlug(PRODUCT_SLUG),
                ProductStatus.REJECTED, null);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ------------------------------------------------------ GET /products/stores/{sellerSlug}/{productSlug}

    @Test
    void getProductByStoreAndSlug_retorna200OKCuandoElProductoExisteYEstaActivo() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE, null);

        mockMvc.perform(get(BASE_URL + "/stores/" + SELLER_SLUG + "/" + PRODUCT_SLUG))
                .andExpect(status().isOk());
    }

    @Test
    void getProductByStoreAndSlug_retornaElDetalleCompletoDelProducto() throws Exception {
        Product product = persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG,
                ProductStatus.ACTIVE, null);
        addAttribute(product, ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        addImage(product, true);
        addVariant(product, true);

        mockMvc.perform(get(BASE_URL + "/stores/" + SELLER_SLUG + "/" + PRODUCT_SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.name").value(PRODUCT_NAME))
                .andExpect(jsonPath("$.slug").value(PRODUCT_SLUG))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.attributes", hasSize(1)))
                .andExpect(jsonPath("$.attributes[0].name").value(ATTRIBUTE_NAME))
                .andExpect(jsonPath("$.attributes[0].value").value(ATTRIBUTE_VALUE))
                .andExpect(jsonPath("$.images", hasSize(1)))
                .andExpect(jsonPath("$.images[0].isPrimary").value(true))
                .andExpect(jsonPath("$.variants", hasSize(1)))
                .andExpect(jsonPath("$.variants[0].isActive").value(true));
    }

    @Test
    void getProductByStoreAndSlug_noRequiereAutenticacion() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE, null);

        mockMvc.perform(get(BASE_URL + "/stores/" + SELLER_SLUG + "/" + PRODUCT_SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(PRODUCT_NAME));
    }

    @Test
    void getProductByStoreAndSlug_retorna404CuandoElProductoNoExiste() throws Exception {
        mockMvc.perform(get(BASE_URL + "/stores/" + SELLER_SLUG + "/" + PRODUCT_SLUG))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductByStoreAndSlug_retorna404CuandoElSellerSlugNoExiste() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE, null);

        mockMvc.perform(get(BASE_URL + "/stores/otra-tienda/" + PRODUCT_SLUG))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductByStoreAndSlug_retorna404CuandoElProductSlugNoExiste() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE, null);

        mockMvc.perform(get(BASE_URL + "/stores/" + SELLER_SLUG + "/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductByStoreAndSlug_noRetornaProductosEliminados() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.ACTIVE,
                LocalDateTime.now());

        mockMvc.perform(get(BASE_URL + "/stores/" + SELLER_SLUG + "/" + PRODUCT_SLUG))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductByStoreAndSlug_noRetornaProductosConEstadoDistintoDeActive() throws Exception {
        persistProduct(SELLER_SLUG, PRODUCT_NAME, PRODUCT_SLUG, ProductStatus.DRAFT, null);

        mockMvc.perform(get(BASE_URL + "/stores/" + SELLER_SLUG + "/" + PRODUCT_SLUG))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------- helpers

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

    private Product persistProduct(String sellerSlug, String name, String slug,
                                   ProductStatus status, LocalDateTime deletedAt) {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(persistBrand())
                .withCategory(persistCategory())
                .withSellerKeycloakId(SELLER_KEYCLOAK_ID)
                .withSellerSlug(sellerSlug)
                .withName(name)
                .withSlug(slug)
                .withStatus(status)
                .withDeletedAt(deletedAt)
                .build();
        return productRepository.save(product);
    }

    private ProductAttribute addAttribute(Product product, String name, String value) {
        ProductAttribute attribute = ProductAttribute.builder()
                .product(product)
                .name(name)
                .value(value)
                .build();
        attribute = productAttributeRepository.save(attribute);
        product.getAttributes().add(attribute);
        return attribute;
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
