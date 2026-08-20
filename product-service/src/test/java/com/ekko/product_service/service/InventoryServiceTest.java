package com.ekko.product_service.service;

import com.ekko.product_service.builder.InventoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.builder.ProductVariantTestDataBuilder;
import com.ekko.product_service.dto.response.InventoryViewResponse;
import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.exception.InsufficientStockException;
import com.ekko.product_service.exception.InvalidStockAdjustmentException;
import com.ekko.product_service.exception.InvalidStockOperationException;
import com.ekko.product_service.exception.InventoryNotFoundException;
import com.ekko.product_service.exception.InventoryOwnershipException;
import com.ekko.product_service.exception.ProductNotAvailableException;
import com.ekko.product_service.messaging.ProductEventPublisher;
import com.ekko.product_service.messaging.dto.publish.InventoryLowStockEvent;
import com.ekko.product_service.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductService productService;

    @Mock
    private ProductEventPublisher productEventPublisher;

    private InventoryService inventoryService;

    private final UUID variantId = UUID.randomUUID();
    private final UUID sellerId = SELLER_KEYCLOAK_ID;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, productService, productEventPublisher);
    }

    // ------------------------------------------------------------- reserveStock

    @Test
    void reserveStock_autenticacionDeProductoPrimero() {
        doThrow(ProductNotAvailableException.class)
                .when(productService).verifyPurchasable(variantId);

        assertThrows(ProductNotAvailableException.class,
                () -> inventoryService.reserveStock(variantId, 2));

        verify(productService).verifyPurchasable(variantId);
    }

    @Test
    void reserveStock_inventoryNotFound() {
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class,
                () -> inventoryService.reserveStock(variantId, 2));
    }

    @Test
    void reserveStock_stockInsuficiente() {
        Inventory inventory = inventory(product(), variantId, 10, 0);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class,
                () -> inventoryService.reserveStock(variantId, 11));
    }

    @Test
    void reserveStock_restaStockReservadoDisponible() {
        Inventory inventory = inventory(product(), variantId, 10, 0);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        inventoryService.reserveStock(variantId, 3);

        assertEquals(3L, inventory.getStockReserved());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveStock_vendibleEsStockDisponibleMenosReservado() {
        Inventory inventory = inventory(product(), variantId, 10, 4);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class,
                () -> inventoryService.reserveStock(variantId, 7));
    }

    @Test
    void reserveStock_stockBajo_publicaEvento() {
        Product product = product();
        Inventory inventory = inventory(product, variantId, 3, 0);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        inventoryService.reserveStock(variantId, 1);

        ArgumentCaptor<InventoryLowStockEvent> captor = ArgumentCaptor.forClass(InventoryLowStockEvent.class);
        verify(productEventPublisher).publishInventoryLowStock(captor.capture());
        InventoryLowStockEvent event = captor.getValue();
        assertEquals(product.getId(), event.productId());
        assertEquals(variantId, event.variantId());
        assertEquals(product.getSellerKeycloakId(), event.sellerKeycloakId());
        assertEquals(3L, event.currentStock());
        assertEquals(3L, event.minimumStock());
        assertNotNull(event.checkedAt());
    }

    @Test
    void reserveStock_stockNormal_noPublicaEvento() {
        Inventory inventory = inventory(product(), variantId, 10, 0);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        inventoryService.reserveStock(variantId, 2);

        verify(productEventPublisher, never()).publishInventoryLowStock(any());
    }

    // ------------------------------------------------------------- confirmStock

    @Test
    void confirmStock_descuentaDisponibleYReservado() {
        Inventory inventory = inventory(product(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        inventoryService.confirmStock(variantId, 3);

        assertEquals(7L, inventory.getStockAvailable());
        assertEquals(5L, inventory.getStockReserved());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void confirmStock_cantidadMayorQueReservadaLanza() {
        Inventory inventory = inventory(product(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        assertThrows(InvalidStockOperationException.class,
                () -> inventoryService.confirmStock(variantId, 9));
    }

    @Test
    void confirmStock_noExisteLanzaNotFound() {
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class,
                () -> inventoryService.confirmStock(variantId, 1));
    }

    // ------------------------------------------------------------- releaseStock

    @Test
    void releaseStock_liberaReserva() {
        Inventory inventory = inventory(product(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        inventoryService.releaseStock(variantId, 3);

        assertEquals(5L, inventory.getStockReserved());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void releaseStock_cantidadMayorQueReservadaLananza() {
        Inventory inventory = inventory(product(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        assertThrows(InvalidStockOperationException.class,
                () -> inventoryService.releaseStock(variantId, 9));
    }

    // ------------------------------------------------------------- adjustStock

    @Test
    void adjustStock_actualizaCantidadDisponible() {
        Inventory inventory = inventory(product(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        InventoryViewResponse response = inventoryService.adjustStock(variantId, 15, sellerId);

        assertEquals(15L, inventory.getStockAvailable());
        assertEquals(15L, response.stockAvailable());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void adjustStock_noEsDuenoLanazaOwnership() {
        Inventory inventory = inventory(freeProduct(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        assertThrows(InventoryOwnershipException.class,
                () -> inventoryService.adjustStock(variantId, 15, sellerId));
    }

    @Test
    void adjustStock_negativoLaLanza() {
        Inventory inventory = inventory(product(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        assertThrows(InvalidStockAdjustmentException.class,
                () -> inventoryService.adjustStock(variantId, -1, sellerId));
    }

    @Test
    void adjustStock_menorQueReservaLaLanza() {
        Inventory inventory = inventory(product(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        assertThrows(InvalidStockAdjustmentException.class,
                () -> inventoryService.adjustStock(variantId, 7, sellerId));
    }

    @Test
    void adjustStock_stockBajo_publicaEvento() {
        Product product = product();
        Inventory inventory = inventory(product, variantId, 5, 0);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        inventoryService.adjustStock(variantId, 2, sellerId);

        ArgumentCaptor<InventoryLowStockEvent> captor = ArgumentCaptor.forClass(InventoryLowStockEvent.class);
        verify(productEventPublisher).publishInventoryLowStock(captor.capture());
        InventoryLowStockEvent event = captor.getValue();
        assertEquals(product.getId(), event.productId());
        assertEquals(variantId, event.variantId());
        assertEquals(product.getSellerKeycloakId(), event.sellerKeycloakId());
        assertEquals(2L, event.currentStock());
        assertEquals(3L, event.minimumStock());
        assertNotNull(event.checkedAt());
    }

    @Test
    void adjustStock_stockNormal_noPublicaEvento() {
        Inventory inventory = inventory(product(), variantId, 10, 8);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        inventoryService.adjustStock(variantId, 15, sellerId);

        verify(productEventPublisher, never()).publishInventoryLowStock(any());
    }

    // ------------------------------------------------------------- getInventory

    @Test
    void getInventory_devuelveVendibleCalculado() {
        Inventory inventory = inventory(product(), variantId, 10, 4);
        when(inventoryRepository.findByVariantId(variantId)).thenReturn(Optional.of(inventory));

        InventoryViewResponse response = inventoryService.getInventory(variantId, sellerId);

        assertEquals(10L, response.stockAvailable());
        assertEquals(4L, response.stockReserved());
        assertEquals(6L, response.availableForSale());
    }

    @Test
    void getInventory_notDuenoLanzOwnership() {
        Inventory inventory = inventory(freeProduct(), variantId, 10, 4);
        when(inventoryRepository.findByVariantId(variantId)).thenReturn(Optional.of(inventory));

        assertThrows(InventoryOwnershipException.class,
                () -> inventoryService.getInventory(variantId, sellerId));
    }

    @Test
    void getInventory_noExisteLanzaNotFound() {
        when(inventoryRepository.findByVariantId(variantId)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class,
                () -> inventoryService.getInventory(variantId, sellerId));
    }

    // ------------------------------------------------------------- helpers

    private Inventory inventory(Product product, UUID id, long available, long reserved) {
        ProductVariant variant = ProductVariantTestDataBuilder.aVariant()
                .withId(id)
                .withProduct(product)
                .build();
        return InventoryTestDataBuilder.anInventory()
                .withVariant(variant)
                .withStockAvailable(available)
                .withStockReserved(reserved)
                .build();
    }

    private Product product() {
        return ProductTestDataBuilder.aProduct()
                .withSellerKeycloakId(sellerId)
                .build();
    }

    private Product freeProduct() {
        return ProductTestDataBuilder.aProduct()
                .withSellerKeycloakId(UUID.randomUUID())
                .build();
    }
}