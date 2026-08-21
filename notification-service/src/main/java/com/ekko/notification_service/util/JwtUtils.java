package com.ekko.notification_service.util;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class JwtUtils {

    private JwtUtils() {
    }

    public static UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}