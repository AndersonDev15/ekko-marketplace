package com.ekko.product_service.service;

import com.ekko.product_service.dto.response.InventoryViewResponse;
import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.exception.InsufficientStockException;
import com.ekko.product_service.exception.InvalidStockAdjustmentException;
import com.ekko.product_service.exception.InvalidStockOperationException;
import com.ekko.product_service.exception.InventoryNotFoundException;
import com.ekko.product_service.exception.InventoryOwnershipException;
import com.ekko.product_service.repository.InventoryRepository;
import com.ekko.product_service.util.StockCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductService productService;

    @Transactional
    public InventoryViewResponse adjustStock(UUID variantId, long newStock, UUID sellerKeycloakId) {
        Inventory inventory = findInventoryForUpdate(variantId);

        if (!verifyOwnership(inventory, sellerKeycloakId)) {
            throw new InventoryOwnershipException();
        }

        if (newStock < 0) {
            throw new InvalidStockAdjustmentException();
        }
        if (newStock < inventory.getStockReserved()) {
            throw new InvalidStockAdjustmentException();
        }

        inventory.setStockAvailable(newStock);
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.save(inventory);

        if (StockCalculator.isLowStock(inventory)) {
            // TODO: publicar LowStockEvent cuando se implemente messaging
        }

        return toView(inventory);
    }

    @Transactional
    public void reserveStock(UUID variantId, long quantity) {
        productService.verifyPurchasable(variantId);
        Inventory inventory = findInventoryForUpdate(variantId);

        long availableForSale = StockCalculator.getAvailableForSale(inventory);
        if (availableForSale < quantity) {
            throw new InsufficientStockException();
        }

        inventory.setStockReserved(inventory.getStockReserved() + quantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.save(inventory);

        if (StockCalculator.isLowStock(inventory)) {
            // TODO: publicar LowStockEvent cuando se implemente messaging
        }
    }

    @Transactional
    public void confirmStock(UUID variantId, long quantity) {
        Inventory inventory = findInventoryForUpdate(variantId);

        if (quantity > inventory.getStockReserved()) {
            throw new InvalidStockOperationException();
        }

        inventory.setStockAvailable(inventory.getStockAvailable() - quantity);
        inventory.setStockReserved(inventory.getStockReserved() - quantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void releaseStock(UUID variantId, long quantity) {
        Inventory inventory = findInventoryForUpdate(variantId);

        if (quantity > inventory.getStockReserved()) {
            throw new InvalidStockOperationException();
        }

        inventory.setStockReserved(inventory.getStockReserved() - quantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.save(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryViewResponse getInventory(UUID variantId, UUID sellerKeycloakId) {
        Inventory inventory = inventoryRepository.findByVariantId(variantId)
                .orElseThrow(InventoryNotFoundException::new);

        if (!inventory.getVariant().getProduct().getSellerKeycloakId().equals(sellerKeycloakId)) {
            throw new InventoryOwnershipException();
        }

        return new InventoryViewResponse(
                inventory.getId(),
                inventory.getStockAvailable(),
                inventory.getStockReserved(),
                inventory.getStockMinimum(),
                StockCalculator.getAvailableForSale(inventory));
    }

    private InventoryViewResponse toView(Inventory inventory) {
        return new InventoryViewResponse(
                inventory.getId(),
                inventory.getStockAvailable(),
                inventory.getStockReserved(),
                inventory.getStockMinimum(),
                StockCalculator.getAvailableForSale(inventory));
    }

    private Inventory findInventoryForUpdate(UUID variantId) {
        return inventoryRepository.findByVariantIdForUpdate(variantId)
                .orElseThrow(InventoryNotFoundException::new);
    }

    private boolean verifyOwnership(Inventory inventory, UUID sellerKeycloakId) {
        ProductVariant variant = inventory.getVariant();
        return variant.getProduct().getSellerKeycloakId().equals(sellerKeycloakId);
    }
}