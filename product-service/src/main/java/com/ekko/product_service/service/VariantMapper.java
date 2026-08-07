package com.ekko.product_service.service;

import com.ekko.product_service.dto.response.VariantAttributeResponse;
import com.ekko.product_service.dto.response.VariantResponse;
import com.ekko.product_service.dto.response.VariantSummaryResponse;
import com.ekko.product_service.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VariantMapper {

    public VariantResponse toResponse(ProductVariant variant, long stockAvailable) {
        return new VariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getSku(),
                variant.getPrice(),
                variant.getDiscountPrice(),
                variant.getCurrency(),
                Boolean.TRUE.equals(variant.getIsActive()),
                stockAvailable,
                mapAttributes(variant),
                variant.getCreatedAt(),
                variant.getUpdatedAt());
    }

    public VariantSummaryResponse toSummary(ProductVariant variant) {
        return new VariantSummaryResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getSku(),
                variant.getPrice(),
                variant.getDiscountPrice(),
                variant.getCurrency(),
                Boolean.TRUE.equals(variant.getIsActive()),
                mapAttributes(variant),
                variant.getCreatedAt(),
                variant.getUpdatedAt());
    }

    private List<VariantAttributeResponse> mapAttributes(ProductVariant variant) {
        if (variant.getAttributes() == null) {
            return List.of();
        }
        return variant.getAttributes().stream()
                .map(attribute -> new VariantAttributeResponse(attribute.getName(), attribute.getValue()))
                .toList();
    }
}
