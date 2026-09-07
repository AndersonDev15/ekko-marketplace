package com.ekko.api_gateway.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class CsrfCookieWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(CsrfCookieWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .doOnSuccess(v -> {
                    Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
                    log.info(">>> csrfToken attr = {}", csrfToken);
                    if (csrfToken != null) {
                        csrfToken.subscribe(
                                t -> log.info(">>> CsrfToken resuelto: {}", t.getToken()),
                                e -> log.error(">>> ERROR resolviendo/guardando csrfToken", e)
                        );
                    } else {
                        log.warn(">>> csrfToken attr es NULL");
                    }
                });
    }
}