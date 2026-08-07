package com.ekko.product_service.service;

import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.builder.ProductVariantTestDataBuilder;
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
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.exception.VariantNotFoundException;
import com.ekko.product_service.repository.InventoryRepository;
import com.ekko.product_service.repository.ProductVariantAttributeRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_NAME;
import static com.ekko.product_service.util.TestConstants.ATTRIBUTE_VALUE;
import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.product_service.util.TestConstants.VARIANT_PRICE;
import static com.ekko.product_service.util.TestConstants.VARIANT_SKU;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VariantServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductVariantAttributeRepository variantAttributeRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OwnershipValidator ownershipValidator;

    @Mock
    private VariantMapper variantMapper;

    private VariantService variantService;

    private final UUID productId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();
    private final UUID sellerId = SELLER_KEYCLOAK_ID;

    @BeforeEach
    void setUp() {
        ActiveProductOwnershipValidator activeValidator =
                new ActiveProductOwnershipValidator(ownershipValidator);
        variantService = new VariantService(
                variantRepository,
                variantAttributeRepository,
                inventoryRepository,
                activeValidator,
                variantMapper);
    }

    // ---------------------------------------------------------------- createVariant

    @Test
    void createVariant_creaUnaVarianteConAtributos() {
        CreateVariantAttributeRequest attr =
                new CreateVariantAttributeRequest(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        CreateVariantRequest request =
                new CreateVariantRequest(VARIANT_SKU, VARIANT_PRICE, null, "EUR", List.of(attr));
        Product product = ownedProduct();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toResponse(any(), eq(0L))).thenReturn(mock(VariantResponse.class));

        variantService.createVariant(productId, request, sellerId);

        ArgumentCaptor<ProductVariant> variantCaptor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).saveAndFlush(variantCaptor.capture());
        ProductVariant savedVariant = variantCaptor.getValue();
        assertEquals(VARIANT_SKU, savedVariant.getSku());
        assertEquals(VARIANT_PRICE, savedVariant.getPrice());
        assertEquals("EUR", savedVariant.getCurrency());
        assertEquals(true, savedVariant.getIsActive());
        assertEquals(product, savedVariant.getProduct());

        ArgumentCaptor<ProductVariantAttribute> attrCaptor =
                ArgumentCaptor.forClass(ProductVariantAttribute.class);
        verify(variantAttributeRepository).save(attrCaptor.capture());
        ProductVariantAttribute savedAttr = attrCaptor.getValue();
        assertEquals(ATTRIBUTE_NAME, savedAttr.getName());
        assertEquals(ATTRIBUTE_VALUE, savedAttr.getValue());
        assertNotNull(savedAttr.getVariant());
    }

    @Test
    void createVariant_creaInventarioInicialConLosValoresEsperados() {
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, null, null, null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toResponse(any(), eq(0L))).thenReturn(mock(VariantResponse.class));

        variantService.createVariant(productId, request, sellerId);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        Inventory inventory = captor.getValue();
        assertEquals(0L, inventory.getStockAvailable());
        assertEquals(0L, inventory.getStockReserved());
        assertEquals(5L, inventory.getStockMinimum());
        assertNotNull(inventory.getVariant());
    }

    @Test
    void createVariant_usaLaMonedaEnviada() {
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, null, "EUR", null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toResponse(any(), eq(0L))).thenReturn(mock(VariantResponse.class));

        variantService.createVariant(productId, request, sellerId);

        ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).saveAndFlush(captor.capture());
        assertEquals("EUR", captor.getValue().getCurrency());
    }

    @Test
    void createVariant_usaUSDCuandoCurrencyEsNull() {
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, null, null, null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toResponse(any(), eq(0L))).thenReturn(mock(VariantResponse.class));

        variantService.createVariant(productId, request, sellerId);

        ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).saveAndFlush(captor.capture());
        assertEquals("USD", captor.getValue().getCurrency());
    }

    @Test
    void createVariant_usaUSDCCuandoCurrencyEsBlank() {
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, null, "  ", null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toResponse(any(), eq(0L))).thenReturn(mock(VariantResponse.class));

        variantService.createVariant(productId, request, sellerId);

        ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).saveAndFlush(captor.capture());
        assertEquals("USD", captor.getValue().getCurrency());
    }

    @Test
    void createVariant_usaElSkuEnviado() {
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, null, null, null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toResponse(any(), eq(0L))).thenReturn(mock(VariantResponse.class));

        variantService.createVariant(productId, request, sellerId);

        ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).saveAndFlush(captor.capture());
        assertEquals(VARIANT_SKU, captor.getValue().getSku());
        verify(variantRepository).existsBySku(VARIANT_SKU);
    }

    @Test
    void createVariant_generaSkuCuandoRequestNull() {
        CreateVariantRequest request = new CreateVariantRequest(
                null, VARIANT_PRICE, null, null, null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toResponse(any(), eq(0L))).thenReturn(mock(VariantResponse.class));

        variantService.createVariant(productId, request, sellerId);

        ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).saveAndFlush(captor.capture());
        assertTrue(captor.getValue().getSku().startsWith("EKKO-"));
        assertTrue(captor.getValue().getSku().matches("EKKO-[0-9A-F]{8}"));
    }

    @Test
    void createVariant_lanzaDuplicateSkuExceptionSiElSkuYaExiste() {
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, null, null, null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.existsBySku(VARIANT_SKU)).thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> variantService.createVariant(productId, request, sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void createVariant_lanzaInvalidDiscountPriceExceptionCuandoDiscountEsMayorOIgualQuePrecio() {
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, new BigDecimal("1000.00"), null, null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());

        assertThrows(InvalidDiscountPriceException.class,
                () -> variantService.createVariant(productId, request, sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void createVariant_devuelveVariantResponseDelMapper() {
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, null, null, null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        VariantResponse response = mock(VariantResponse.class);
        when(variantMapper.toResponse(any(ProductVariant.class), eq(0L))).thenReturn(response);

        VariantResponse result = variantService.createVariant(productId, request, sellerId);

        assertSame(response, result);
    }

    // ---------------------------------------------------------------- updateVariant

    @Test
    void updateVariant_actualizaElPrecio() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toSummary(any())).thenReturn(mock(VariantSummaryResponse.class));

        variantService.updateVariant(productId, variantId,
                new UpdateVariantRequest(null, new BigDecimal("1000.00"), null), sellerId);

        assertEquals(new BigDecimal("1000.00"), variant.getPrice());
        verify(variantRepository).saveAndFlush(variant);
    }

    @Test
    void updateVariant_actualizaElDescuento() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toSummary(any())).thenReturn(mock(VariantSummaryResponse.class));

        variantService.updateVariant(productId, variantId,
                new UpdateVariantRequest(null, null, new BigDecimal("150.00")), sellerId);

        assertEquals(new BigDecimal("150.00"), variant.getDiscountPrice());
        verify(variantRepository).saveAndFlush(variant);
    }

    @Test
    void updateVariant_actualizaElSku() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toSummary(any())).thenReturn(mock(VariantSummaryResponse.class));

        variantService.updateVariant(productId, variantId,
                new UpdateVariantRequest("NEW-SKU", null, null), sellerId);

        assertEquals("NEW-SKU", variant.getSku());
        verify(variantRepository).existsBySkuAndIdNot("NEW-SKU", variantId);
        verify(variantRepository).saveAndFlush(variant);
    }

    @Test
    void updateVariant_noModificaCamposNull() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toSummary(any())).thenReturn(mock(VariantSummaryResponse.class));

        variantService.updateVariant(productId, variantId,
                new UpdateVariantRequest(null, null, null), sellerId);

        assertEquals(VARIANT_SKU, variant.getSku());
        assertEquals(VARIANT_PRICE, variant.getPrice());
        verify(variantRepository).saveAndFlush(variant);
    }

    @Test
    void updateVariant_lanzaDuplicateSkuExceptionSiElNuevoSkuYaExiste() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsBySkuAndIdNot("NEW-SKU", variantId)).thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> variantService.updateVariant(productId, variantId,
                        new UpdateVariantRequest("NEW-SKU", null, null), sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateVariant_lanzaInvalidDiscountPriceExceptionSiElDescuentoEsInvalido() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        assertThrows(InvalidDiscountPriceException.class,
                () -> variantService.updateVariant(productId, variantId,
                        new UpdateVariantRequest(null, null, new BigDecimal("1000.00")), sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateVariant_devuelveVariantSummaryResponse() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        VariantSummaryResponse summary = mock(VariantSummaryResponse.class);
        when(variantMapper.toSummary(variant)).thenReturn(summary);

        VariantSummaryResponse result = variantService.updateVariant(productId, variantId,
                new UpdateVariantRequest(null, null, null), sellerId);

        assertSame(summary, result);
    }

    // ------------------------------------------------------------ deactivateVariant

    @Test
    void deactivateVariant_desactivaLaVariante() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                productId, variantId)).thenReturn(1L);
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toSummary(any())).thenReturn(mock(VariantSummaryResponse.class));

        variantService.deactivateVariant(productId, variantId, sellerId);

        assertEquals(false, variant.getIsActive());
        verify(variantRepository).saveAndFlush(variant);
    }

    @Test
    void deactivateVariant_actualizaUpdatedAt() {
        ProductVariant variant = ownedVariant();
        variant.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                productId, variantId)).thenReturn(1L);
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantMapper.toSummary(any())).thenReturn(mock(VariantSummaryResponse.class));

        variantService.deactivateVariant(productId, variantId, sellerId);

        assertNotNull(variant.getUpdatedAt());
    }

    @Test
    void deactivateVariant_lanzaLastActiveVariantExceptionSiEsLaUltimaActiva() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                productId, variantId)).thenReturn(0L);

        assertThrows(LastActiveVariantException.class,
                () -> variantService.deactivateVariant(productId, variantId, sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void deactivateVariant_devuelveVariantSummaryResponse() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                productId, variantId)).thenReturn(1L);
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        VariantSummaryResponse summary = mock(VariantSummaryResponse.class);
        when(variantMapper.toSummary(any())).thenReturn(summary);

        VariantSummaryResponse result = variantService.deactivateVariant(productId, variantId, sellerId);

        assertSame(summary, result);
    }

    // ------------------------------------------------------------- softDeleteVariant

    @Test
    void softDeleteVariant_marcaDeletedAt() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                productId, variantId)).thenReturn(1L);
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        variantService.softDeleteVariant(productId, variantId, sellerId);

        assertNotNull(variant.getDeletedAt());
        verify(variantRepository).saveAndFlush(variant);
    }

    @Test
    void softDeleteVariant_desactivaLaVariante() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                productId, variantId)).thenReturn(1L);
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        variantService.softDeleteVariant(productId, variantId, sellerId);

        assertEquals(false, variant.getIsActive());
    }

    @Test
    void softDeleteVariant_actualizaUpdatedAt() {
        ProductVariant variant = ownedVariant();
        variant.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                productId, variantId)).thenReturn(1L);
        when(variantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        variantService.softDeleteVariant(productId, variantId, sellerId);

        assertNotNull(variant.getUpdatedAt());
    }

    @Test
    void softDeleteVariant_lanzaLastActiveVariantExceptionSiEsLaUltimaActiva() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                productId, variantId)).thenReturn(0L);

        assertThrows(LastActiveVariantException.class,
                () -> variantService.softDeleteVariant(productId, variantId, sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    // ---------------------------------------------------------------- casos adicionales

    @Test
    void createVariant_lanzaProductNotFoundExceptionCuandoElProductoEstaEliminado() {
        Product product = ProductTestDataBuilder.aProduct()
                .withId(productId)
                .withDeletedAt(LocalDateTime.now())
                .build();
        CreateVariantRequest request = new CreateVariantRequest(
                VARIANT_SKU, VARIANT_PRICE, null, null, null);
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);

        assertThrows(ProductNotFoundException.class,
                () -> variantService.createVariant(productId, request, sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateVariant_lanzaVariantNotFoundExceptionCuandoLaVarianteNoExiste() {
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.empty());

        assertThrows(VariantNotFoundException.class,
                () -> variantService.updateVariant(productId, variantId,
                        new UpdateVariantRequest(null, null, null), sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateVariant_lanzaVariantNotFoundExceptionCuandoLaVarianteEstaEliminada() {
        Product product = ownedProduct();
        ProductVariant variant = ProductVariantTestDataBuilder.aVariant()
                .withProduct(product)
                .withIsActive(true)
                .build();
        variant.setDeletedAt(LocalDateTime.now());
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(product);
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        assertThrows(VariantNotFoundException.class,
                () -> variantService.updateVariant(productId, variantId,
                        new UpdateVariantRequest(null, null, null), sellerId));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateVariant_lanzaDuplicateSkuExceptionCuandoSaveAndFlushLanzaDataIntegrityViolation() {
        ProductVariant variant = ownedVariant();
        when(ownershipValidator.validate(productId, sellerId)).thenReturn(variant.getProduct());
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.saveAndFlush(variant)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(DuplicateSkuException.class,
                () -> variantService.updateVariant(productId, variantId,
                        new UpdateVariantRequest("NEW-SKU", null, null), sellerId));
    }

    // ------------------------------------------------------------------- helpers

    private Product ownedProduct() {
        return ProductTestDataBuilder.aProduct().withId(productId).build();
    }

    private ProductVariant ownedVariant() {
        return ProductVariantTestDataBuilder.aVariant()
                .withId(variantId)
                .withProduct(ownedProduct())
                .withSku(VARIANT_SKU)
                .withPrice(VARIANT_PRICE)
                .withIsActive(true)
                .build();
    }
}