package com.ekko.order_service;

import com.ekko.order_service.infrastructure.persistence.adapter.out.product.ProductServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class ProductServiceFeignTestController {

    private final ProductServiceClient productServiceClient;

    @GetMapping("/product-variants")
    public ResponseEntity<?> test() {

        UUID variantId =
                UUID.fromString("00000000-0000-0000-0000-000000000001");

        return ResponseEntity.ok(
                productServiceClient.getVariantsInfo(
                        List.of(variantId)
                )
        );
    }
}