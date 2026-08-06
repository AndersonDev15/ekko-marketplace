package com.ekko.product_service.builder;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.entity.ProductVariantAttribute;
import com.ekko.product_service.entity.Inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.CURRENCY;
import static com.ekko.product_service.util.TestConstants.VARIANT_ID;
import static com.ekko.product_service.util.TestConstants.VARIANT_PRICE;
import static com.ekko.product_service.util.TestConstants.VARIANT_SKU;

public class ProductVariantTestDataBuilder {

    private UUID id = VARIANT_ID;
    private Product product = null;
    private String sku = VARIANT_SKU;
    private BigDecimal price = VARIANT_PRICE;
    private BigDecimal discountPrice = null;
    private String currency = CURRENCY;
    private Boolean isActive = true;
    private LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 0, 0);
    private LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
    private List<ProductVariantAttribute> attributes = new ArrayList<>();
    private Inventory inventory = null;

    private ProductVariantTestDataBuilder() {
    }

    public static ProductVariantTestDataBuilder aVariant() {
        return new ProductVariantTestDataBuilder();
    }

    public ProductVariantTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ProductVariantTestDataBuilder withProduct(Product product) {
        this.product = product;
        return this;
    }

    public ProductVariantTestDataBuilder withSku(String sku) {
        this.sku = sku;
        return this;
    }

    public ProductVariantTestDataBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ProductVariantTestDataBuilder withDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
        return this;
    }

    public ProductVariantTestDataBuilder withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public ProductVariantTestDataBuilder withIsActive(Boolean isActive) {
        this.isActive = isActive;
        return this;
    }

    public ProductVariantTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ProductVariantTestDataBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public ProductVariantTestDataBuilder withAttributes(List<ProductVariantAttribute> attributes) {
        this.attributes = attributes;
        return this;
    }

    public ProductVariantTestDataBuilder withInventory(Inventory inventory) {
        this.inventory = inventory;
        return this;
    }

    public ProductVariant build() {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setPrice(price);
        variant.setDiscountPrice(discountPrice);
        variant.setCurrency(currency);
        variant.setIsActive(isActive);
        variant.setCreatedAt(createdAt);
        variant.setUpdatedAt(updatedAt);
        variant.setAttributes(attributes);
        variant.setInventory(inventory);
        return variant;
    }
}