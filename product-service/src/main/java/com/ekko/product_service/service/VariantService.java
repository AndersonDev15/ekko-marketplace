package com.ekko.product_service.service;

import com.ekko.product_service.dto.request.CreateVariantAttributeRequest;
import com.ekko.product_service.dto.request.CreateVariantRequest;
import com.ekko.product_service.dto.request.UpdateVariantRequest;
import com.ekko.product_service.dto.response.VariantResponse;
import com.ekko.product_service.dto.response.VariantSummaryResponse;
import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.entity.ProductVariantAttribute;
import com.ekko.product_service.exception.DuplicateSkuException;
import com.ekko.product_service.exception.InvalidDiscountPriceException;
import com.ekko.product_service.exception.LastActiveVariantException;
import com.ekko.product_service.exception.VariantNotFoundException;
import com.ekko.product_service.repository.InventoryRepository;
import com.ekko.product_service.repository.ProductVariantAttributeRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VariantService {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final long DEFAULT_STOCK_MINIMUM = 5L;

    private final ProductVariantRepository variantRepository;
    private final ProductVariantAttributeRepository variantAttributeRepository;
    private final InventoryRepository inventoryRepository;
    private final ActiveProductOwnershipValidator activeProductOwnershipValidator;
    private final VariantMapper variantMapper;

    @Transactional
    public VariantResponse createVariant(UUID productId, CreateVariantRequest request, UUID sellerKeycloakId) {
        Product product = activeProductOwnershipValidator.validate(productId, sellerKeycloakId);

        String sku = resolveSku(request.sku());
        BigDecimal price = request.price();
        validateDiscountPrice(request.discountPrice(), price);

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku)
                .price(price)
                .discountPrice(request.discountPrice())
                .currency(resolveCurrency(request.currency()))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        persistVariant(variant);

        if (request.attributes() != null) {
            for (CreateVariantAttributeRequest attributeRequest : request.attributes()) {
                ProductVariantAttribute attribute = ProductVariantAttribute.builder()
                        .variant(variant)
                        .name(attributeRequest.name())
                        .value(attributeRequest.value())
                        .build();
                variantAttributeRepository.save(attribute);
                variant.getAttributes().add(attribute);
            }
        }

        Inventory inventory = Inventory.builder()
                .variant(variant)
                .stockAvailable(0L)
                .stockReserved(0L)
                .stockMinimum(DEFAULT_STOCK_MINIMUM)
                .updatedAt(LocalDateTime.now())
                .build();
        inventoryRepository.save(inventory);

        return variantMapper.toResponse(variant, 0L);
    }

    @Transactional
    public VariantSummaryResponse updateVariant(UUID productId, UUID variantId, UpdateVariantRequest request, UUID sellerKeycloakId) {
        activeProductOwnershipValidator.validate(productId, sellerKeycloakId);

        ProductVariant variant = findOwnedVariant(productId, variantId);

        if (request.sku() != null && !request.sku().isBlank() && !request.sku().equals(variant.getSku())) {
            if (variantRepository.existsBySkuAndIdNot(request.sku(), variantId)) {
                throw new DuplicateSkuException();
            }
            variant.setSku(request.sku());
        }

        if (request.price() != null || request.discountPrice() != null) {
            BigDecimal resultingPrice = request.price() != null ? request.price() : variant.getPrice();
            BigDecimal resultingDiscount = request.discountPrice() != null ? request.discountPrice() : variant.getDiscountPrice();
            validateDiscountPrice(resultingDiscount, resultingPrice);
        }

        if (request.price() != null) {
            variant.setPrice(request.price());
        }
        if (request.discountPrice() != null) {
            variant.setDiscountPrice(request.discountPrice());
        }

        variant.setUpdatedAt(LocalDateTime.now());

        persistVariant(variant);

        return variantMapper.toSummary(variant);
    }

    @Transactional
    public VariantSummaryResponse deactivateVariant(UUID productId, UUID variantId, UUID sellerKeycloakId) {
        activeProductOwnershipValidator.validate(productId, sellerKeycloakId);

        ProductVariant variant = findOwnedVariant(productId, variantId);
        ensureNotLastActiveVariant(variant);

        variant.setIsActive(false);
        variant.setUpdatedAt(LocalDateTime.now());

        persistVariant(variant);

        return variantMapper.toSummary(variant);
    }

    @Transactional
    public void softDeleteVariant(UUID productId, UUID variantId, UUID sellerKeycloakId) {
        activeProductOwnershipValidator.validate(productId, sellerKeycloakId);

        ProductVariant variant = findOwnedVariant(productId, variantId);
        ensureNotLastActiveVariant(variant);

        variant.setDeletedAt(LocalDateTime.now());
        variant.setIsActive(false);
        variant.setUpdatedAt(LocalDateTime.now());

        persistVariant(variant);
    }

    private ProductVariant findOwnedVariant(UUID productId, UUID variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .filter(v -> v.getProduct().getId().equals(productId))
                .orElseThrow(VariantNotFoundException::new);

        if (variant.getDeletedAt() != null) {
            throw new VariantNotFoundException();
        }

        return variant;
    }

    private String resolveSku(String requestedSku) {
        if (requestedSku != null && !requestedSku.isBlank()) {
            if (variantRepository.existsBySku(requestedSku)) {
                throw new DuplicateSkuException();
            }
            return requestedSku;
        }

        String sku = generateSku();
        while (variantRepository.existsBySku(sku)) {
            sku = generateSku();
        }
        return sku;
    }

    private String generateSku() {
        return "EKKO-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return currency;
    }

    private void validateDiscountPrice(BigDecimal discountPrice, BigDecimal price) {
        if (discountPrice != null && discountPrice.compareTo(price) >= 0) {
            throw new InvalidDiscountPriceException();
        }
    }

    private void ensureNotLastActiveVariant(ProductVariant variant) {
        if (!Boolean.TRUE.equals(variant.getIsActive())) {
            return;
        }

        long activeOthers = variantRepository
                .countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                        variant.getProduct().getId(), variant.getId());
        if (activeOthers == 0) {
            throw new LastActiveVariantException();
        }
    }

    private void persistVariant(ProductVariant variant) {
        try {
            variantRepository.saveAndFlush(variant);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateSkuException();
        }
    }
}
