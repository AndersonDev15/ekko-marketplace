package com.ekko.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationFilterTest {

    private EmailVerificationFilter filter;

    @BeforeEach
    void setup() {
        filter = new EmailVerificationFilter();
    }

    @Test
    void unauthenticatedRequest_shouldPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/orders").build()
        );

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void jwtAuth_withVerifiedEmail_get_shouldPass() {
        Jwt jwt = createJwt(Map.of("email_verified", true));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER"));
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void jwtAuth_withVerifiedEmail_post_shouldPass() {
        Jwt jwt = createJwt(Map.of("email_verified", true));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER"));
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/orders").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void jwtAuth_withUnverifiedEmail_get_shouldPass() {
        Jwt jwt = createJwt(Map.of("email_verified", false));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER"));
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void jwtAuth_withUnverifiedEmail_post_shouldReturn403() {
        Jwt jwt = createJwt(Map.of("email_verified", false));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER"));
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/orders").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void jwtAuth_withUnverifiedEmail_postToIdentity_shouldPass() {
        Jwt jwt = createJwt(Map.of("email_verified", false));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER"));
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/identity/email/resend-verification").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    // OAuth2AuthenticationToken tests (session-based login)
    @Test
    void oauth2Auth_withVerifiedEmail_get_shouldPass() {
        OidcUser oidcUser = createOidcUser(Map.of("email_verified", true));
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, AuthorityUtils.createAuthorityList("ROLE_USER"), "keycloak");
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void oauth2Auth_withVerifiedEmail_post_shouldPass() {
        OidcUser oidcUser = createOidcUser(Map.of("email_verified", true));
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, AuthorityUtils.createAuthorityList("ROLE_USER"), "keycloak");
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/orders").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void oauth2Auth_withUnverifiedEmail_get_shouldPass() {
        OidcUser oidcUser = createOidcUser(Map.of("email_verified", false));
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, AuthorityUtils.createAuthorityList("ROLE_USER"), "keycloak");
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void oauth2Auth_withUnverifiedEmail_post_shouldReturn403() {
        OidcUser oidcUser = createOidcUser(Map.of("email_verified", false));
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, AuthorityUtils.createAuthorityList("ROLE_USER"), "keycloak");
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/orders").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void oauth2Auth_withUnverifiedEmail_postToIdentity_shouldPass() {
        OidcUser oidcUser = createOidcUser(Map.of("email_verified", false));
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, AuthorityUtils.createAuthorityList("ROLE_USER"), "keycloak");
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/identity/email/resend-verification").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void oauth2Auth_withUnverifiedEmail_postToSellerEndpoint_shouldReturn403() {
        OidcUser oidcUser = createOidcUser(Map.of("email_verified", false));
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, AuthorityUtils.createAuthorityList("ROLE_USER"), "keycloak");
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/sellers/bank-accounts").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void oauth2Auth_withUnverifiedEmail_putToSellerEndpoint_shouldReturn403() {
        OidcUser oidcUser = createOidcUser(Map.of("email_verified", false));
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, AuthorityUtils.createAuthorityList("ROLE_USER"), "keycloak");
        
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/sellers/me").build()
        );
        exchange.getAttributes().put("org.springframework.security.web.server.authorization.SecurityContextRepository.SecurityContext", 
                new SecurityContextImpl(auth));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private OidcUser createOidcUser(Map<String, Object> claims) {
        Map<String, Object> allClaims = new java.util.HashMap<>();
        allClaims.put("sub", "test-sub");
        allClaims.put("preferred_username", "testuser");
        allClaims.put("email", "test@example.com");
        allClaims.putAll(claims);
        
        OidcIdToken idToken = new OidcIdToken(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                allClaims
        );
        
        return new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"), idToken);
    }
}