package com.ekko.product_service.builder;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductAttribute;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.PRODUCT_ID;
import static com.ekko.product_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.product_service.util.TestConstants.PRODUCT_SLUG;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.SELLER_SLUG;

public class ProductTestDataBuilder {

    private UUID id = PRODUCT_ID;
    private UUID sellerKeycloakId = SELLER_KEYCLOAK_ID;
    private String sellerSlug = SELLER_SLUG;
    private com.ekko.product_service.entity.Brand brand = BrandTestDataBuilder.aBrand().build();
    private com.ekko.product_service.entity.Category category = CategoryTestDataBuilder.aCategory().build();
    private String name = PRODUCT_NAME;
    private String slug = PRODUCT_SLUG;
    private String description = "iPhone 16 with pro features";
    private ProductStatus status = ProductStatus.DRAFT;
    private BigDecimal averageRating = BigDecimal.ZERO;
    private Integer reviewCount = 0;
    private LocalDateTime deletedAt = null;
    private LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 0, 0);
    private LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
    private List<ProductImage> images = new ArrayList<>();
    private List<ProductAttribute> attributes = new ArrayList<>();
    private List<ProductVariant> variants = new ArrayList<>();

    private ProductTestDataBuilder() {
    }

    public static ProductTestDataBuilder aProduct() {
        return new ProductTestDataBuilder();
    }

    public ProductTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ProductTestDataBuilder withSellerKeycloakId(UUID sellerKeycloakId) {
        this.sellerKeycloakId = sellerKeycloakId;
        return this;
    }

    public ProductTestDataBuilder withSellerSlug(String sellerSlug) {
        this.sellerSlug = sellerSlug;
        return this;
    }

    public ProductTestDataBuilder withBrand(com.ekko.product_service.entity.Brand brand) {
        this.brand = brand;
        return this;
    }

    public ProductTestDataBuilder withCategory(com.ekko.product_service.entity.Category category) {
        this.category = category;
        return this;
    }

    public ProductTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ProductTestDataBuilder withSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public ProductTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ProductTestDataBuilder withStatus(ProductStatus status) {
        this.status = status;
        return this;
    }

    public ProductTestDataBuilder withAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
        return this;
    }

    public ProductTestDataBuilder withReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
        return this;
    }

    public ProductTestDataBuilder withDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
        return this;
    }

    public ProductTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ProductTestDataBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public ProductTestDataBuilder withImages(List<ProductImage> images) {
        this.images = images;
        return this;
    }

    public ProductTestDataBuilder withAttributes(List<ProductAttribute> attributes) {
        this.attributes = attributes;
        return this;
    }

    public ProductTestDataBuilder withVariants(List<ProductVariant> variants) {
        this.variants = variants;
        return this;
    }

    public Product build() {
        Product product = new Product();
        product.setId(id);
        product.setSellerKeycloakId(sellerKeycloakId);
        product.setSellerSlug(sellerSlug);
        product.setBrand(brand);
        product.setCategory(category);
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(description);
        product.setStatus(status);
        product.setAverageRating(averageRating);
        product.setReviewCount(reviewCount);
        product.setDeletedAt(deletedAt);
        product.setCreatedAt(createdAt);
        product.setUpdatedAt(updatedAt);
        product.setImages(images);
        product.setAttributes(attributes);
        product.setVariants(variants);
        return product;
    }
}