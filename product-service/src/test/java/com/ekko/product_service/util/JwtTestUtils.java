package com.ekko.product_service.util;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ADMIN_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.SERVICE_ORDER_KEYCLOAK_ID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class JwtTestUtils {

    private JwtTestUtils() {
    }

    public static Jwt sellerJwt() {
        return buildJwt(SELLER_KEYCLOAK_ID, "SELLER");
    }

    public static Jwt adminJwt() {
        return buildJwt(ADMIN_KEYCLOAK_ID, "ADMIN");
    }

    public static Jwt customerJwt() {
        return buildJwt(CUSTOMER_KEYCLOAK_ID, "CUSTOMER");
    }

    public static Jwt serviceOrderJwt() {
        return buildJwt(SERVICE_ORDER_KEYCLOAK_ID, "SERVICE_ORDER");
    }

    private static Jwt buildJwt(UUID subject, String role) {
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

    public static JwtRequestPostProcessor jwtSeller() {
        return jwt().jwt(sellerJwt());
    }

    public static JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(adminJwt());
    }

    public static RequestPostProcessor sellerAuth(JwtAuthenticationConverter converter) {
        return jwt().jwt(sellerJwt())
                .authorities(jwt -> converter.convert(jwt).getAuthorities());
    }

    public static RequestPostProcessor adminAuth(JwtAuthenticationConverter converter) {
        return jwt().jwt(adminJwt())
                .authorities(jwt -> converter.convert(jwt).getAuthorities());
    }

    public static RequestPostProcessor customerAuth(JwtAuthenticationConverter converter) {
        return jwt().jwt(customerJwt())
                .authorities(jwt -> converter.convert(jwt).getAuthorities());
    }

    public static RequestPostProcessor serviceOrderAuth(JwtAuthenticationConverter converter) {
        return jwt().jwt(serviceOrderJwt())
                .authorities(jwt -> converter.convert(jwt).getAuthorities());
    }
}