package com.ekko.payment_service.util;

import java.util.UUID;

public final class TestConstants {

    private TestConstants() {
    }

    // Customer
    public static final UUID CUSTOMER_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID OTHER_CUSTOMER_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    public static final UUID ADMIN_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final String GUEST_EMAIL = "guest@example.com";
    public static final String WRONG_GUEST_EMAIL = "otro@example.com";
}
