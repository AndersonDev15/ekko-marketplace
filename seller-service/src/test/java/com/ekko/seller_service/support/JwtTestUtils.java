package com.ekko.seller_service.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class JwtTestUtils {

    public static final String SELLER_KEYCLOAK_ID = "8f14e45f-ceea-4a2a-b1e0-2f1a1c3f0001";
    public static final String SELLER_EMAIL = "seller@ekko.test";
    public static final String ADMIN_KEYCLOAK_ID = "9f14e45f-ceea-4a2a-b1e0-2f1a1c3f0002";
    public static final String ADMIN_EMAIL = "admin@ekko.test";

    public static final String SIGNING_SECRET = "seller-service-test-signing-secret-0123456789abcdef";
    private static final SecretKey HMAC_KEY = new SecretKeySpec(
            SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private static final String ISSUER = "http://localhost:8180/realms/ekko";

    private JwtTestUtils() {
    }

    // -------------------------------------------------------------------------
    // Objetos Jwt para SecurityMockMvcRequestPostProcessors.jwt()
    // -------------------------------------------------------------------------

    public static Jwt sellerJwt() {
        return jwtWithRoles(SELLER_KEYCLOAK_ID, SELLER_EMAIL, List.of("SELLER"));
    }

    public static Jwt adminJwt() {
        return jwtWithRoles(ADMIN_KEYCLOAK_ID, ADMIN_EMAIL, List.of("ADMIN"));
    }

    public static Jwt noRolesJwt() {
        return jwtWithRoles(SELLER_KEYCLOAK_ID, SELLER_EMAIL, List.of());
    }

    public static Jwt expiredJwt() {
        return jwtWithRolesAndExpiry(SELLER_KEYCLOAK_ID, SELLER_EMAIL, List.of("SELLER"),
                Instant.now().minusSeconds(3600));
    }

    public static Jwt jwtWithRoles(String keycloakId, String email, List<String> roles) {
        return jwtWithRolesAndExpiry(keycloakId, email, roles, Instant.now().plusSeconds(3600));
    }

    public static Jwt jwtWithRolesAndExpiry(String keycloakId, String email, List<String> roles, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(3600);
        return Jwt.withTokenValue("unsigned-test-token")
                .header("alg", "none")
                .header("typ", "JWT")
                .issuer(ISSUER)
                .subject(keycloakId)
                .claim("email", email)
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    // -------------------------------------------------------------------------
    // Tokens firmados (HS256) para escenarios con header Authorization: Bearer
    // -------------------------------------------------------------------------

    public static String sellerToken() {
        return signedToken(SELLER_KEYCLOAK_ID, SELLER_EMAIL, List.of("SELLER"), Instant.now().plusSeconds(3600));
    }

    public static String adminToken() {
        return signedToken(ADMIN_KEYCLOAK_ID, ADMIN_EMAIL, List.of("ADMIN"), Instant.now().plusSeconds(3600));
    }

    public static String noRolesToken() {
        return signedToken(SELLER_KEYCLOAK_ID, SELLER_EMAIL, List.of(), Instant.now().plusSeconds(3600));
    }

    public static String expiredToken() {
        return signedToken(SELLER_KEYCLOAK_ID, SELLER_EMAIL, List.of("SELLER"), Instant.now().minusSeconds(3600));
    }

    public static String malformedToken() {
        return "not-a-valid-jwt";
    }

    private static String signedToken(String keycloakId, String email, List<String> roles, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(3600);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(keycloakId)
                .claim("email", email)
                .claim("realm_access", Map.of("roles", roles))
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();
        try {
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJwt.sign(new MACSigner(HMAC_KEY));
            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign test JWT", e);
        }
    }

    // -------------------------------------------------------------------------
    // Postprocesadores de MockMvc para tokens válidos
    // -------------------------------------------------------------------------

    public static JwtRequestPostProcessor jwtSeller() {
        return jwt().jwt(sellerJwt());
    }

    public static JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(adminJwt());
    }

    public static JwtRequestPostProcessor jwtNoRoles() {
        return jwt().jwt(noRolesJwt());
    }
}
