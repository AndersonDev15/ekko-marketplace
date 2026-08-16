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
import com.ekko.product_service.entity.ProductAttribute;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.entity.ProductVariantAttribute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    ProductDetailResponse toDetailResponse(Product product);

    ProductVariantResponse toVariantResponse(ProductVariant variant);

    @Mapping(target = "primaryImageUrl", expression = "java(primaryImageUrl(product))")
    ProductCatalogResponse toCatalogResponse(Product product);

    ProductAttributeResponse toAttributeResponse(ProductAttribute attribute);

    ProductImageResponse toImageResponse(ProductImage image);

    ProductVariantAttributeResponse toVariantAttributeResponse(ProductVariantAttribute attribute);

    InventoryResponse toInventoryResponse(Inventory inventory);

    default String primaryImageUrl(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);
    }
}