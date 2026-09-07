package com.ekko.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class CsrfController {

    @GetMapping("/csrf")
    public Mono<Void> csrf() {
        return Mono.empty();
    }
}
