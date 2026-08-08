package com.ekko.product_service.controller;

import com.ekko.product_service.dto.response.ProductVariantInfo;
import com.ekko.product_service.service.VariantQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/variants")
@RequiredArgsConstructor
public class InternalVariantController {

    private final VariantQueryService variantQueryService;

    @GetMapping
    public ResponseEntity<List<ProductVariantInfo>> getVariantsInfo(
            @RequestParam("variantIds") List<UUID> variantIds) {
        if (variantIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductVariantInfo> variantsInfo = variantQueryService.getVariantsInfo(variantIds);
        return ResponseEntity.ok(variantsInfo);
    }
}