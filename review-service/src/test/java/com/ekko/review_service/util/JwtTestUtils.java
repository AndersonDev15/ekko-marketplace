package com.ekko.review_service.util;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class JwtTestUtils {

    private JwtTestUtils() {
    }

    public static RequestPostProcessor customerAuth(UUID customerId) {
        return jwt().jwt(jwt -> jwt.subject(customerId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    public static RequestPostProcessor adminAuth(UUID adminId) {
        return jwt().jwt(jwt -> jwt.subject(adminId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}