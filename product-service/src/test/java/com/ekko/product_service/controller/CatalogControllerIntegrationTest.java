package com.ekko.product_service.controller;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductImageTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.request.CreateVariantRequest;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductImageRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import com.ekko.product_service.service.InventoryService;
import com.ekko.product_service.service.VariantService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class CatalogControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VariantService variantService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository imageRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private EntityManager entityManager;

    private final UUID sellerId = SELLER_KEYCLOAK_ID;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // ------------------------------------------------------ GET /catalog/products

    @Test
    void searchProducts_retornaProductosConStock() throws Exception {
        createActiveProductWithStock(new BigDecimal("150.00"));
        flushAndClear();

        mockMvc.perform(get("/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].price").value(150.00));
    }

    @Test
    void searchProducts_filtraPorQuery() throws Exception {
        Product p1 = createActiveProductWithStock(new BigDecimal("150.00"));
        p1.setName("iPhone 16 Pro");
        productRepository.save(p1);
        flushAndClear();

        mockMvc.perform(get("/catalog/products")
                        .param("query", "iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("iPhone 16 Pro"));
    }

    @Test
    void searchProducts_rangoInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/catalog/products")
                        .param("minPrice", "500")
                        .param("maxPrice", "100"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------ GET /catalog/products/{slug}

    @Test
    void getProductDetail_retornaDetalle() throws Exception {
        Product product = createActiveProductWithStock(new BigDecimal("150.00"));
        flushAndClear();

        mockMvc.perform(get("/catalog/products/{slug}", product.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.variants", hasSize(1)));
    }

    @Test
    void getProductDetail_noExisteRetorna404() throws Exception {
        mockMvc.perform(get("/catalog/products/{slug}", "no-existe"))
                .andExpect(status().isNotFound());
    }

    // --------------------------------------------------------------- helpers

    private Product createActiveProductWithStock(BigDecimal price) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Brand brand = brandRepository.save(BrandTestDataBuilder.aBrand()
                .withId(null)
                .withName("Brand-" + suffix)
                .withSlug("brand-" + suffix)
                .build());
        Category category = categoryRepository.save(CategoryTestDataBuilder.aCategory()
                .withId(null)
                .withSlug("cat-" + suffix)
                .build());
        Product product = productRepository.save(ProductTestDataBuilder.aProduct()
                .withId(null)
                .withName("Phone-" + suffix)
                .withSlug("phone-" + suffix)
                .withBrand(brand)
                .withCategory(category)
                .withStatus(ProductStatus.ACTIVE)
                .withSellerKeycloakId(sellerId)
                .build());

        UUID variantId = variantService.createVariant(product.getId(),
                new CreateVariantRequest("SKU-" + UUID.randomUUID().toString().substring(0, 12),
                        price, null, null, null), sellerId).id();
        inventoryService.adjustStock(variantId, 10, sellerId);

        ProductImage image = ProductImageTestDataBuilder.anImage()
                .withId(null)
                .withProduct(product)
                .withUrl("https://cdn.example.com/main-" + suffix + ".jpg")
                .withIsPrimary(true)
                .withSortOrder(0)
                .build();
        imageRepository.save(image);

        return product;
    }
}