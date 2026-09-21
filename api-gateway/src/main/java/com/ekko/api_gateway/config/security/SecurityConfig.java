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
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf
                  .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                  .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                  .requireCsrfProtectionMatcher(new AndServerWebExchangeMatcher(
                CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                new NegatedServerWebExchangeMatcher(
                        new PathPatternParserServerWebExchangeMatcher("/webhooks/stripe")
                )
        ))
)
                .addFilterAfter(new CsrfCookieWebFilter(), SecurityWebFiltersOrder.CSRF)

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health").permitAll()

                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // specs JSON de cada microservicio, consumidas por la UI del gateway
                        .pathMatchers(
                                "/api/identity/v3/api-docs/**",
                                "/api/sellers/v3/api-docs/**",
                                "/api/products/v3/api-docs/**",
                                "/api/orders/v3/api-docs/**",
                                "/api/payments/v3/api-docs/**",
                                "/api/reviews/v3/api-docs/**",
                                "/api/notifications/v3/api-docs/**"
                        ).permitAll()

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


                        .pathMatchers(HttpMethod.POST, "/api/identity/register/customer").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/identity/register/seller").permitAll()
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