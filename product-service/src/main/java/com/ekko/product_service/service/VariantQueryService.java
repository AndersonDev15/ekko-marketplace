package com.ekko.product_service.service;

import com.ekko.product_service.dto.response.ProductVariantInfo;
import com.ekko.product_service.exception.VariantNotFoundException;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.entity.SellerSnapshot;
import com.ekko.product_service.repository.InventoryRepository;
import com.ekko.product_service.repository.ProductImageRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import com.ekko.product_service.repository.SellerSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VariantQueryService {

    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductImageRepository productImageRepository;
    private final SellerSnapshotRepository sellerSnapshotRepository;

    @Transactional(readOnly = true)
    public List<ProductVariantInfo> getVariantsInfo(List<UUID> variantIds) {
        List<ProductVariant> variants = variantRepository.findAllWithProductByIds(variantIds);

        if (variants.size() != variantIds.size()) {
            throw new VariantNotFoundException();
        }

        List<UUID> resolvedVariantIds = variants.stream().map(ProductVariant::getId).toList();
        Set<UUID> productIds = variants.stream()
                .map(v -> v.getProduct().getId())
                .collect(Collectors.toSet());
        Set<UUID> sellerKeycloakIds = variants.stream()
                .map(v -> v.getProduct().getSellerKeycloakId())
                .collect(Collectors.toSet());

        Map<UUID, Long> stockByVariantId = inventoryRepository
                .findAllByVariantIdIn(resolvedVariantIds)
                .stream()
                .collect(Collectors.toMap(
                        i -> i.getVariant().getId(),
                        i -> i.getStockAvailable() - i.getStockReserved()));

        Map<UUID, String> imageUrlByProductId = productImageRepository
                .findAllByProductIdInAndIsPrimaryTrue(productIds)
                .stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), ProductImage::getUrl));

        Map<UUID, String> storeNameBySeller = sellerSnapshotRepository
                .findAllBySellerKeycloakIdIn(sellerKeycloakIds)
                .stream()
                .collect(Collectors.toMap(SellerSnapshot::getSellerKeycloakId, SellerSnapshot::getStoreName));

        return variants.stream()
                .map(v -> toVariantInfo(v, stockByVariantId, imageUrlByProductId, storeNameBySeller))
                .toList();
    }

    private ProductVariantInfo toVariantInfo(
            ProductVariant variant,
            Map<UUID, Long> stockByVariantId,
            Map<UUID, String> imageUrlByProductId,
            Map<UUID, String> storeNameBySeller) {
        Product product = variant.getProduct();
        return new ProductVariantInfo(
                variant.getId(),
                product.getId(),
                product.getName(),
                variant.getSku(),
                variant.getPrice(),
                product.getSellerKeycloakId(),
                storeNameBySeller.get(product.getSellerKeycloakId()),
                imageUrlByProductId.get(product.getId()),
                stockByVariantId.get(variant.getId())
        );
    }
}