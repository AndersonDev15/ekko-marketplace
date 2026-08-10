package com.ekko.order_service.util;

import java.util.UUID;

public final class TestConstants {

    private TestConstants() {
    }

    // Customer
    public static final UUID CUSTOMER_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String GUEST_EMAIL = "guest@example.com";

    // Seller
    public static final UUID SELLER_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final String SELLER_NAME = "Tech Store";

    // Variant
    public static final UUID VARIANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    public static final String PRODUCT_NAME = "iPhone 16";
    public static final String VARIANT_SKU = "IP16-128";
    public static final String IMAGE_URL = "https://cdn.example.com/iphone-16.jpg";

    // Address
    public static final String ADDRESS_FULL_NAME = "John Doe";
    public static final String ADDRESS_PHONE = "+5491112345678";
    public static final String ADDRESS_LINE = "Av. Siempre Viva 742";
    public static final String ADDRESS_CITY = "Buenos Aires";
    public static final String ADDRESS_STATE = "Buenos Aires";
    public static final String ADDRESS_COUNTRY = "AR";
    public static final String ADDRESS_POSTAL_CODE = "1414";
}