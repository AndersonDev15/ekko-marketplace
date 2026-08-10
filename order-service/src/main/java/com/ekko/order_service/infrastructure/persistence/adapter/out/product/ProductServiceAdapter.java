package com.ekko.order_service.infrastructure.persistence.adapter.out.product;

import com.ekko.order_service.domain.exception.InsufficientStockException;
import com.ekko.order_service.domain.exception.StockReservationException;
import com.ekko.order_service.domain.model.ProductVariant;
import com.ekko.order_service.domain.model.StockItem;
import com.ekko.order_service.domain.port.out.ProductServicePort;
import com.ekko.order_service.infrastructure.persistence.adapter.out.product.dto.ProductVariantInfo;
import com.ekko.order_service.infrastructure.persistence.adapter.out.product.dto.StockReservationItem;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceAdapter implements ProductServicePort {

    private final ProductServiceClient productServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<ProductVariant> getVariantsInfo(List<UUID> variantIds) {
        try {
            return productServiceClient.getVariantsInfo(variantIds)
                    .stream()
                    .map(this::toDomain)
                    .toList();
        } catch (FeignException ex) {
            throw new StockReservationException(
                    "Failed to fetch product variants from product-service", ex);
        }
    }

    @Override
    public void reserveStock(List<StockItem> items) {
        List<StockReservationItem> request = items.stream()
                .map(item -> new StockReservationItem(item.variantId(), item.quantity()))
                .toList();
        try {
            productServiceClient.reserveStock(request);
        } catch (FeignException.Conflict ex) {
            throw insufficientStockFrom(ex);
        } catch (FeignException ex) {
            throw new StockReservationException(
                    "Failed to reserve stock in product-service", ex);
        }
    }

    private InsufficientStockException insufficientStockFrom(FeignException.Conflict ex) {
        UUID variantId = extractVariantId(ex);
        if (variantId != null) {
            return new InsufficientStockException(variantId, 0);
        }
        return new InsufficientStockException("Insufficient stock reported by product-service");
    }

    private UUID extractVariantId(FeignException ex) {
        if (ex.responseBody().isEmpty()) {
            return null;
        }
        try {
            byte[] raw = ex.responseBody().get().array();
            JsonNode root = objectMapper.readTree(
                    new String(raw, StandardCharsets.UTF_8));
            if (root.hasNonNull("variantId")) {
                return UUID.fromString(root.get("variantId").asText());
            }
        } catch (Exception ignored) {
            // body no parseable; se propaga sin variantId
        }
        return null;
    }

    private ProductVariant toDomain(ProductVariantInfo info) {
        return new ProductVariant(
                info.variantId(),
                info.productId(),
                info.productName(),
                info.sku(),
                info.price(),
                info.sellerKeycloakId(),
                info.storeName(),
                info.imageUrl(),
                info.availableStock()
        );
    }
}