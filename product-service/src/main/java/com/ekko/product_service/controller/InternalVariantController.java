package com.ekko.product_service.controller;

import com.ekko.product_service.dto.response.ProductVariantInfo;
import com.ekko.product_service.service.VariantQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/variants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE_ORDER')")
@Tag(name = "Internal Variants", description = "Internal variant endpoints for order service")
@SecurityRequirement(name = "bearerAuth")
public class InternalVariantController {

    private final VariantQueryService variantQueryService;

    @Operation(summary = "Get variants info", description = "Returns variant information for multiple variant IDs (internal use by order service)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "No variant IDs provided"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SERVICE_ORDER role")
    })
    @GetMapping
    public ResponseEntity<List<ProductVariantInfo>> getVariantsInfo(
            @Parameter(description = "List of variant IDs", required = true) @RequestParam("variantIds") List<UUID> variantIds) {
        if (variantIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductVariantInfo> variantsInfo = variantQueryService.getVariantsInfo(variantIds);
        return ResponseEntity.ok(variantsInfo);
    }
}