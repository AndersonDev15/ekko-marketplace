package com.ekko.product_service.mapper;

import com.ekko.product_service.dto.response.InventoryResponse;
import com.ekko.product_service.dto.response.ProductAttributeResponse;
import com.ekko.product_service.dto.response.ProductCatalogResponse;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.dto.response.ProductVariantAttributeResponse;
import com.ekko.product_service.dto.response.ProductVariantResponse;
import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        List<ProductAttributeResponse> attributes = product.getAttributes().stream()
                .map(attribute -> new ProductAttributeResponse(
                        attribute.getId(),
                        attribute.getName(),
                        attribute.getValue()))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getSellerKeycloakId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getStatus(),
                product.getAverageRating(),
                product.getReviewCount(),
                attributes);
    }

    public ProductDetailResponse toDetailResponse(Product product) {
        List<ProductAttributeResponse> attributes = product.getAttributes().stream()
                .map(attribute -> new ProductAttributeResponse(
                        attribute.getId(),
                        attribute.getName(),
                        attribute.getValue()))
                .toList();

        List<ProductImageResponse> images = product.getImages().stream()
                .map(image -> new ProductImageResponse(
                        image.getId(),
                        image.getUrl(),
                        image.getIsPrimary(),
                        image.getSortOrder()))
                .toList();

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(this::toVariantResponse)
                .toList();

        return new ProductDetailResponse(
                product.getId(),
                product.getSellerKeycloakId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getStatus(),
                product.getAverageRating(),
                product.getReviewCount(),
                attributes,
                images,
                variants);
    }

    public ProductVariantResponse toVariantResponse(ProductVariant variant) {
        List<ProductVariantAttributeResponse> attributes = variant.getAttributes().stream()
                .map(attribute -> new ProductVariantAttributeResponse(
                        attribute.getId(),
                        attribute.getName(),
                        attribute.getValue()))
                .toList();

        InventoryResponse inventory = null;
        Inventory variantInventory = variant.getInventory();
        if (variantInventory != null) {
            inventory = new InventoryResponse(
                    variantInventory.getId(),
                    variantInventory.getStockAvailable(),
                    variantInventory.getStockReserved(),
                    variantInventory.getStockMinimum());
        }

        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getPrice(),
                variant.getDiscountPrice(),
                variant.getCurrency(),
                variant.getIsActive(),
                inventory,
                attributes);
    }

    public ProductCatalogResponse toCatalogResponse(Product product) {
        String primaryImageUrl = product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);

        return new ProductCatalogResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getAverageRating(),
                product.getReviewCount(),
                primaryImageUrl);
    }
}