package com.ekko.review_service.util;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class JwtTestUtils {

    private JwtTestUtils() {
    }

    public static RequestPostProcessor customerAuth(JwtAuthenticationConverter converter, UUID customerId) {
        return jwt().jwt(jwt -> jwt.subject(customerId.toString())
                        .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))
                .authorities(jwt -> converter.convert(jwt).getAuthorities());
    }

    public static RequestPostProcessor adminAuth(JwtAuthenticationConverter converter, UUID adminId) {
        return jwt().jwt(jwt -> jwt.subject(adminId.toString())
                        .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                .authorities(jwt -> converter.convert(jwt).getAuthorities());
    }
}