package com.ekko.product_service.builder;

import com.ekko.product_service.entity.Category;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.CATEGORY_ID;
import static com.ekko.product_service.util.TestConstants.CATEGORY_NAME;
import static com.ekko.product_service.util.TestConstants.CATEGORY_SLUG;

public class CategoryTestDataBuilder {

    private UUID id = CATEGORY_ID;
    private String name = CATEGORY_NAME;
    private String slug = CATEGORY_SLUG;
    private String description = "Smartphones";
    private String imageUrl = "https://cdn.example.com/phones.png";
    private Category parent = null;
    private Boolean isActive = true;
    private LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 0, 0);
    private LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);

    private CategoryTestDataBuilder() {
    }

    public static CategoryTestDataBuilder aCategory() {
        return new CategoryTestDataBuilder();
    }

    public CategoryTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public CategoryTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CategoryTestDataBuilder withSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public CategoryTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public CategoryTestDataBuilder withImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public CategoryTestDataBuilder withParent(Category parent) {
        this.parent = parent;
        return this;
    }

    public CategoryTestDataBuilder withIsActive(Boolean isActive) {
        this.isActive = isActive;
        return this;
    }

    public CategoryTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public CategoryTestDataBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Category build() {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setImageUrl(imageUrl);
        category.setParent(parent);
        category.setIsActive(isActive);
        category.setCreatedAt(createdAt);
        category.setUpdatedAt(updatedAt);
        return category;
    }
}