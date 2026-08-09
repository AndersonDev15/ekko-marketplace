package com.ekko.api_gateway.config.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class OAuth2TokenRelayFilter implements GlobalFilter, Ordered {

    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2TokenRelayFilter(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        return exchange.getPrincipal()
                .filter(Authentication.class::isInstance)
                .cast(Authentication.class)
                .flatMap(authentication -> {

                    OAuth2AuthorizeRequest authorizeRequest =
                            OAuth2AuthorizeRequest
                                    .withClientRegistrationId("keycloak")
                                    .principal(authentication)
                                    .build();

                    return authorizedClientManager
                            .authorize(authorizeRequest);
                })
                .flatMap(authorizedClient -> {

                    String accessToken =
                            authorizedClient.getAccessToken().getTokenValue();

                    ServerHttpRequest request = exchange.getRequest()
                            .mutate()
                            .headers(headers ->
                                    headers.setBearerAuth(accessToken))
                            .build();

                    return chain.filter(
                            exchange.mutate()
                                    .request(request)
                                    .build()
                    );
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }
}