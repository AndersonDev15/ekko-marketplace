package com.ekko.payment_service.util;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ekko.payment_service.util.TestConstants.ADMIN_KEYCLOAK_ID;
import static com.ekko.payment_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.payment_service.util.TestConstants.OTHER_CUSTOMER_KEYCLOAK_ID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class JwtTestUtils {

    private JwtTestUtils() {
    }

    public static Jwt customerJwt() {
        return buildJwt(CUSTOMER_KEYCLOAK_ID, "CUSTOMER", "customer@example.com");
    }

    public static Jwt otherCustomerJwt() {
        return buildJwt(OTHER_CUSTOMER_KEYCLOAK_ID, "CUSTOMER");
    }

    public static Jwt adminJwt() {
        return buildJwt(ADMIN_KEYCLOAK_ID, "ADMIN");
    }

    public static Jwt buildJwt(UUID subject, String role) {
        return buildJwt(subject, role, null);
    }

    public static Jwt buildJwt(UUID subject, String role, String email) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("token-" + role)
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(subject.toString())
                .claim("realm_access", Map.of("roles", List.of(role)))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600));
        if (email != null) {
            builder.claim("email", email);
        }
        return builder.build();
    }

    public static RequestPostProcessor customerAuth() {
        return authentication(new JwtAuthenticationToken(
                customerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    public static RequestPostProcessor otherCustomerAuth() {
        return authentication(new JwtAuthenticationToken(
                otherCustomerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    public static RequestPostProcessor adminAuth() {
        return authentication(new JwtAuthenticationToken(
                adminJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    public static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtCustomer() {
        return jwt().jwt(customerJwt());
    }

    public static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(adminJwt());
    }
}