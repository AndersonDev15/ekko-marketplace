package com.ekko.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecuirtyConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
         http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                 .authorizeExchange(exchanges -> exchanges
                         .pathMatchers("/actuator/health").permitAll()
                         .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                         .pathMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                         .pathMatchers(HttpMethod.GET, "/api/sellers/*").permitAll()
                         .anyExchange().authenticated()
                 )
                .oauth2ResourceServer(oauth2->oauth2
                        .jwt(jwt -> Customizer.withDefaults())
                );
        return http.build();
    }
}
