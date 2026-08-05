package com.ekko.seller_service;

import com.ekko.seller_service.support.JwtTestUtils;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class TestJwtDecoderConfig {

    private static final SecretKey HMAC_KEY = new SecretKeySpec(
            JwtTestUtils.SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private static final String ISSUER = "http://localhost:8180/realms/ekko";

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            try {
                SignedJWT signedJWT = SignedJWT.parse(token);
                if (!signedJWT.verify(new MACVerifier(HMAC_KEY))) {
                    throw new BadJwtException("Invalid signature");
                }
                var claims = signedJWT.getJWTClaimsSet();

                if (!ISSUER.equals(claims.getIssuer())) {
                    throw new BadJwtException("Invalid issuer");
                }
                if (claims.getExpirationTime() == null
                        || claims.getExpirationTime().before(new java.util.Date())) {
                    throw new BadJwtException("Token expired");
                }

                Map<String, Object> claimMap = new HashMap<>(claims.getClaims());

                return Jwt.withTokenValue(token)
                        .header("alg", "HS256")
                        .claims(c -> c.putAll(claimMap))
                        .subject(claims.getSubject())
                        .issuer(claims.getIssuer())
                        .issuedAt(claims.getIssueTime().toInstant())
                        .expiresAt(claims.getExpirationTime().toInstant())
                        .build();
            } catch (BadJwtException e) {
                throw e;
            } catch (Exception e) {
                throw new BadJwtException("Invalid token", e);
            }
        };
    }
}
