package com.ekko.product_service.util;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ADMIN_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;

public final class JwtTestUtils {

    private JwtTestUtils() {
    }

    public static Jwt sellerJwt() {
        return jwt(SELLER_KEYCLOAK_ID, "SELLER");
    }

    public static Jwt adminJwt() {
        return jwt(ADMIN_KEYCLOAK_ID, "ADMIN");
    }

    public static Jwt customerJwt() {
        return jwt(CUSTOMER_KEYCLOAK_ID, "CUSTOMER");
    }

    private static Jwt jwt(UUID subject, String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token-" + role)
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(subject.toString())
                .claim("realm_access", Map.of("roles", List.of(role)))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
    }
}