package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.dto.request.CreateBrandRequest;
import com.ekko.product_service.dto.request.UpdateBrandRequest;
import com.ekko.product_service.dto.response.BrandResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.exception.BrandNotFoundException;
import com.ekko.product_service.exception.DuplicateBrandNameException;
import com.ekko.product_service.exception.DuplicateBrandSlugException;
import com.ekko.product_service.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    private BrandService brandService;

    private final UUID brandId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        brandService = new BrandService(brandRepository);
    }

    // ------------------------------------------------------------- createBrand

    @Test
    void createBrand_creaMarcaActiva() {
        when(brandRepository.existsByName("Apple")).thenReturn(false);
        when(brandRepository.existsBySlug("apple")).thenReturn(false);
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.createBrand(createRequest());

        verify(brandRepository).save(any());
        assertTrue(response.isActive());
        assertEquals("Apple", response.name());
        assertEquals("apple", response.slug());
    }

    @Test
    void createBrand_nombreDuplicadoLanzaDuplicateName() {
        when(brandRepository.existsByName("Apple")).thenReturn(true);

        assertThrows(DuplicateBrandNameException.class,
                () -> brandService.createBrand(createRequest()));
    }

    @Test
    void createBrand_slugDuplicadoLanzaDuplicateSlug() {
        when(brandRepository.existsByName("Apple")).thenReturn(false);
        when(brandRepository.existsBySlug("apple")).thenReturn(true);

        assertThrows(DuplicateBrandSlugException.class,
                () -> brandService.createBrand(createRequest()));
    }

    // ------------------------------------------------------------- updateBrand

    @Test
    void updateBrand_actualizaCampos() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameAndIdNot("Samsung", brandId)).thenReturn(false);
        when(brandRepository.existsBySlugAndIdNot("samsung", brandId)).thenReturn(false);
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.updateBrand(brandId,
                new UpdateBrandRequest("Samsung", "samsung", null, null));

        assertEquals("Samsung", response.name());
        assertEquals("samsung", response.slug());
    }

    @Test
    void updateBrand_noExisteLanzaNotFound() {
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        assertThrows(BrandNotFoundException.class, () ->
                brandService.updateBrand(brandId, new UpdateBrandRequest(null, null, null, null)));
    }

    @Test
    void updateBrand_nombreDuplicadoEnOtraMarcaLanzaDuplicateName() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameAndIdNot("AppleX", brandId)).thenReturn(true);

        assertThrows(DuplicateBrandNameException.class, () ->
                brandService.updateBrand(brandId, new UpdateBrandRequest("AppleX", null, null, null)));
    }

    @Test
    void updateBrand_mismoNombreNoLanzaDuplicate() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.updateBrand(brandId,
                new UpdateBrandRequest("Apple", null, null, null));

        assertEquals("Apple", response.name());
    }

    @Test
    void updateBrand_slugDuplicadoEnOtraMarcaLanzaDuplicateSlug() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.existsBySlugAndIdNot("appleX", brandId)).thenReturn(true);

        assertThrows(DuplicateBrandSlugException.class, () ->
                brandService.updateBrand(brandId, new UpdateBrandRequest(null, "appleX", null, null)));
    }

    @Test
    void updateBrand_camposNulosNoModifican() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.updateBrand(brandId,
                new UpdateBrandRequest(null, null, null, null));

        assertEquals("Apple", response.name());
        assertEquals("apple", response.slug());
    }

    // ------------------------------------------------------------- deactivateBrand

    @Test
    void deactivateBrand_desactivaMarca() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));

        brandService.deactivateBrand(brandId);

        assertFalse(brand.getIsActive());
        verify(brandRepository).save(brand);
    }

    @Test
    void deactivateBrand_noExisteLanzaNotFound() {
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        assertThrows(BrandNotFoundException.class, () -> brandService.deactivateBrand(brandId));
    }

    // ------------------------------------------------------------- getActiveBrands

    @Test
    void getActiveBrands_devuelveSoloActivas() {
        Brand active = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findAllByIsActiveTrue()).thenReturn(List.of(active));

        List<BrandResponse> response = brandService.getActiveBrands();

        assertEquals(1, response.size());
        assertEquals(brandId, response.get(0).id());
    }

    // --------------------------------------------------------------- helpers

    private CreateBrandRequest createRequest() {
        return new CreateBrandRequest("Apple", "apple",
                "https://cdn.example.com/apple.png", "Apple brand");
    }
}