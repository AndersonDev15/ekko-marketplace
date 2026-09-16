package com.ekko.api_gateway.config.security;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userOrIpKeyResolver(){
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .cast(JwtAuthenticationToken.class)
                .map(auth ->{
                    Jwt jwt = auth.getToken();
                    return jwt.getSubject();
                })
                .switchIfEmpty(Mono.defer(() -> Mono.just(
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress())))
                .onErrorResume(e -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()));

    }
}
