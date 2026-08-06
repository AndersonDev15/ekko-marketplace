package com.ekko.product_service.builder;

import com.ekko.product_service.entity.Brand;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.BRAND_ID;
import static com.ekko.product_service.util.TestConstants.BRAND_NAME;
import static com.ekko.product_service.util.TestConstants.BRAND_SLUG;

public class BrandTestDataBuilder {

    private UUID id = BRAND_ID;
    private String name = BRAND_NAME;
    private String slug = BRAND_SLUG;
    private String logoUrl = "https://cdn.example.com/apple.png";
    private String description = "Apple brand";
    private Boolean isActive = true;
    private LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 0, 0);
    private LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);

    private BrandTestDataBuilder() {
    }

    public static BrandTestDataBuilder aBrand() {
        return new BrandTestDataBuilder();
    }

    public BrandTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public BrandTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public BrandTestDataBuilder withSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public BrandTestDataBuilder withLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }

    public BrandTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public BrandTestDataBuilder withIsActive(Boolean isActive) {
        this.isActive = isActive;
        return this;
    }

    public BrandTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public BrandTestDataBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Brand build() {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setSlug(slug);
        brand.setLogoUrl(logoUrl);
        brand.setDescription(description);
        brand.setIsActive(isActive);
        brand.setCreatedAt(createdAt);
        brand.setUpdatedAt(updatedAt);
        return brand;
    }
}