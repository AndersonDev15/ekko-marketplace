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
import com.ekko.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Spy
    private SlugService slugService = new SlugService(mock(ProductRepository.class));

    @InjectMocks
    private BrandService brandService;

    private final UUID brandId = UUID.randomUUID();

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
    void createBrand_slugDuplicadoGeneraSlugUnico() {
        when(brandRepository.existsByName("Apple")).thenReturn(false);
        when(brandRepository.existsBySlug("apple")).thenReturn(true, false);
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.createBrand(createRequest());

        assertTrue(response.isActive());
        assertEquals("apple-2", response.slug());
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
                new UpdateBrandRequest("Samsung", "samsung"));

        assertEquals("Samsung", response.name());
        assertEquals("samsung", response.slug());
    }

    @Test
    void updateBrand_noExisteLanzaNotFound() {
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        assertThrows(BrandNotFoundException.class, () ->
                brandService.updateBrand(brandId, new UpdateBrandRequest(null, null)));
    }

    @Test
    void updateBrand_nombreDuplicadoEnOtraMarcaLanzaDuplicateName() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameAndIdNot("AppleX", brandId)).thenReturn(true);

        assertThrows(DuplicateBrandNameException.class, () ->
                brandService.updateBrand(brandId, new UpdateBrandRequest("AppleX", null)));
    }

    @Test
    void updateBrand_mismoNombreNoLanzaDuplicate() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.updateBrand(brandId,
                new UpdateBrandRequest("Apple", null));

        assertEquals("Apple", response.name());
    }

@Test
    void updateBrand_slugDuplicadoEnOtraMarcaGeneraSlugUnico() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        // Simulate that "apple-new" slug already exists for another brand
        when(brandRepository.existsBySlugAndIdNot("apple-new", brandId)).thenReturn(true);
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Change name to trigger slug generation
        BrandResponse response = brandService.updateBrand(brandId,
                new UpdateBrandRequest("Apple New", "desc"));

        // The service generates unique slugs - verify it's different from base
        assertNotEquals("apple-new", response.slug());
        assertTrue(response.slug().startsWith("apple-new"));
    }

    @Test
    void updateBrand_camposNulosNoModifican() {
        Brand brand = BrandTestDataBuilder.aBrand().withId(brandId).build();
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse response = brandService.updateBrand(brandId,
                new UpdateBrandRequest(null, null));

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
        return new CreateBrandRequest("Apple", "Apple brand");
    }
}