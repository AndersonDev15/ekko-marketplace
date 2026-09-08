package com.ekko.api_gateway.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .addFilterAfter(new CsrfCookieWebFilter(), SecurityWebFiltersOrder.CSRF)

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/catalog/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/brands/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/reviews/me").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/reviews/eligible").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/product-reviews/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/sellers/*").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/orders/*").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/orders/*/cancel").permitAll()

                        .pathMatchers(HttpMethod.POST, "/api/identity/password/forgot").permitAll()
                        .pathMatchers("/api/vendor-accounts/onboarding/**").permitAll()
                        .pathMatchers("/webhooks/stripe").permitAll()
                        .pathMatchers("/csrf").permitAll()
                        .pathMatchers("/test/oauth2").authenticated()
                        .pathMatchers("/test/relay").authenticated()
                        .anyExchange().authenticated()
                )

                .oauth2Login(Customizer.withDefaults())

                .build();
    }
}