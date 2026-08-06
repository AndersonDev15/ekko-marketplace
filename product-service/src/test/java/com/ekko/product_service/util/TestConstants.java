package com.ekko.product_service.util;

import java.math.BigDecimal;
import java.util.UUID;

public final class TestConstants {

    private TestConstants() {
    }

    // Seller
    public static final UUID SELLER_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String SELLER_SLUG = "tech-store";

    // Admin
    public static final UUID ADMIN_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");

    // Customer
    public static final UUID CUSTOMER_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    // Brand
    public static final UUID BRAND_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final String BRAND_NAME = "Apple";
    public static final String BRAND_SLUG = "apple";

    // Category
    public static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final String CATEGORY_NAME = "Phones";
    public static final String CATEGORY_SLUG = "phones";

    // Product
    public static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    public static final String PRODUCT_NAME = "iPhone 16";
    public static final String PRODUCT_SLUG = "iphone-16";

    // Variant
    public static final UUID VARIANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    public static final String VARIANT_SKU = "IP16-128";
    public static final BigDecimal VARIANT_PRICE = new BigDecimal("999.99");

    // Image
    public static final UUID IMAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    public static final String IMAGE_URL = "https://cdn.example.com/iphone-16.jpg";

    // Attribute
    public static final UUID ATTRIBUTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    public static final String ATTRIBUTE_NAME = "color";
    public static final String ATTRIBUTE_VALUE = "black";

    // Misc
    public static final String CURRENCY = "USD";
}