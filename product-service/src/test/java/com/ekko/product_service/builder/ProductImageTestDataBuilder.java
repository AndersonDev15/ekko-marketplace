package com.ekko.product_service.builder;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.IMAGE_ID;
import static com.ekko.product_service.util.TestConstants.IMAGE_URL;

public class ProductImageTestDataBuilder {

    private UUID id = IMAGE_ID;
    private Product product = null;
    private String url = IMAGE_URL;
    private Boolean isPrimary = true;
    private Integer sortOrder = 0;
    private LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 0, 0);

    private ProductImageTestDataBuilder() {
    }

    public static ProductImageTestDataBuilder anImage() {
        return new ProductImageTestDataBuilder();
    }

    public ProductImageTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ProductImageTestDataBuilder withProduct(Product product) {
        this.product = product;
        return this;
    }

    public ProductImageTestDataBuilder withUrl(String url) {
        this.url = url;
        return this;
    }

    public ProductImageTestDataBuilder withIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
        return this;
    }

    public ProductImageTestDataBuilder withSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public ProductImageTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ProductImage build() {
        ProductImage image = new ProductImage();
        image.setId(id);
        image.setProduct(product);
        image.setUrl(url);
        image.setIsPrimary(isPrimary);
        image.setSortOrder(sortOrder);
        image.setCreatedAt(createdAt);
        return image;
    }
}