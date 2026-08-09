package com.ekko.api_gateway.controller;


import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TokenRelayTestController {

    @GetMapping("/relay")
    public ResponseEntity<Map<String, Object>> testRelay(
            ServerHttpRequest request) {

        String authorization =
                request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        Map<String, Object> response = new HashMap<>();

        response.put(
                "authorizationPresent",
                authorization != null
        );

        response.put(
                "authorizationStartsWithBearer",
                authorization != null &&
                        authorization.startsWith("Bearer ")
        );

        return ResponseEntity.ok(response);
    }
}

