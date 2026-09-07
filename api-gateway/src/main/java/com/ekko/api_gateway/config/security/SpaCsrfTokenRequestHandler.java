package com.ekko.api_gateway.config.security;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public final class SpaCsrfTokenRequestHandler extends ServerCsrfTokenRequestAttributeHandler {

    private final ServerCsrfTokenRequestAttributeHandler delegate =
            new XorServerCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(ServerWebExchange exchange, Mono<CsrfToken> csrfToken) {
        this.delegate.handle(exchange, csrfToken);
    }

    @Override
    public Mono<String> resolveCsrfTokenValue(ServerWebExchange exchange, CsrfToken csrfToken) {
        String headerValue = exchange.getRequest().getHeaders().getFirst(csrfToken.getHeaderName());
        return headerValue != null
                ? super.resolveCsrfTokenValue(exchange, csrfToken)
                : this.delegate.resolveCsrfTokenValue(exchange, csrfToken);
    }
}

