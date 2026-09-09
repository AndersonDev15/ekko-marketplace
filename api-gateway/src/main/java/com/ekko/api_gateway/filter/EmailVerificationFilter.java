package com.ekko.api_gateway.filter;

import com.ekko.api_gateway.exception.EmailNotVerifiedException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class EmailVerificationFilter implements GlobalFilter, Ordered {

    private static final Set<String> ALLOWED_IDENTITY_PATHS = Set.of(
            "/api/identity/email/resend-verification",
            "/api/identity/credentials",
            "/api/identity/credentials/password"
    );

    private static final String ERROR_RESPONSE = """
            {
              "code": "EMAIL_NOT_VERIFIED",
              "message": "Email verification is required to perform this action"
            }
            """;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();

        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken || auth instanceof OAuth2AuthenticationToken)
                .flatMap(auth -> {
                    Boolean emailVerified = extractEmailVerified(auth);
                    if (emailVerified == null) {
                        return chain.filter(exchange);
                    }
                    
                    if (emailVerified) {
                        return chain.filter(exchange);
                    }

                    if (HttpMethod.GET.name().equals(method)) {
                        return chain.filter(exchange);
                    }

                    if (path.startsWith("/api/identity/")) {
                        return chain.filter(exchange);
                    }

                    return writeErrorResponse(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    Boolean extractEmailVerified(org.springframework.security.core.Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return extractFromJwt(jwtAuth.getToken());
        } else if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
            Object principal = oauth2Auth.getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                OidcIdToken idToken = oidcUser.getIdToken();
                if (idToken != null) {
                    return extractFromOidcIdToken(idToken);
                }
            }
            // Fallback to user attributes
            Object emailVerifiedAttr = oauth2Auth.getPrincipal() instanceof OidcUser 
                ? ((OidcUser) oauth2Auth.getPrincipal()).getClaimAsBoolean("email_verified")
                : null;
            return emailVerifiedAttr != null ? (Boolean) emailVerifiedAttr : false;
        }
        return null;
    }

    private Boolean extractFromJwt(Jwt jwt) {
        Boolean emailVerified = jwt.getClaim("email_verified");
        return emailVerified != null ? emailVerified : false;
    }

    private Boolean extractFromOidcIdToken(OidcIdToken idToken) {
        Boolean emailVerified = idToken.getClaim("email_verified");
        return emailVerified != null ? emailVerified : false;
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ERROR_RESPONSE.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}