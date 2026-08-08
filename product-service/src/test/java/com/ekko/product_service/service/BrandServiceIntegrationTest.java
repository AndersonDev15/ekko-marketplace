package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.request.CreateBrandRequest;
import com.ekko.product_service.dto.request.UpdateBrandRequest;
import com.ekko.product_service.dto.response.BrandResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.exception.BrandNotFoundException;
import com.ekko.product_service.exception.DuplicateBrandNameException;
import com.ekko.product_service.exception.DuplicateBrandSlugException;
import com.ekko.product_service.repository.BrandRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class BrandServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandRepository brandRepository;

    // ------------------------------------------------------------- createBrand

    @Test
    void createBrand_persisteMarcaActiva() {
        BrandResponse response = brandService.createBrand(
                new CreateBrandRequest("Samsung", "samsung", "https://cdn.example.com/samsung.png", "Samsung brand"));

        assertEquals("Samsung", response.name());
        assertEquals("samsung", response.slug());
        assertTrue(response.isActive());
        assertTrue(brandRepository.existsByName("Samsung"));
    }

    @Test
    void createBrand_nombreDuplicadoLanzaDuplicateName() {
        persistBrand("Samsung", "samsung");
        brandRepository.flush();

        assertThrows(DuplicateBrandNameException.class, () ->
                brandService.createBrand(new CreateBrandRequest("Samsung", "samsung-x", null, null)));
    }

    @Test
    void createBrand_slugDuplicadoLanzaDuplicateSlug() {
        persistBrand("Samsung", "samsung");
        brandRepository.flush();

        assertThrows(DuplicateBrandSlugException.class, () ->
                brandService.createBrand(new CreateBrandRequest("Samsung-X", "samsung", null, null)));
    }

    // ------------------------------------------------------------- updateBrand

    @Test
    void updateBrand_actualizaCampos() {
        Brand brand = persistBrand("Original", "original");
        brandRepository.flush();

        BrandResponse response = brandService.updateBrand(brand.getId(),
                new UpdateBrandRequest("Actualizado", "actualizado", null, "nueva desc"));

        assertEquals("Actualizado", response.name());
        assertEquals("actualizado", response.slug());
        assertEquals("nueva desc", response.description());
    }

    @Test
    void updateBrand_noExisteLanzaNotFound() {
        assertThrows(BrandNotFoundException.class, () ->
                brandService.updateBrand(UUID.randomUUID(),
                        new UpdateBrandRequest("X", "x", null, null)));
    }

    @Test
    void updateBrand_nombreDuplicadoEnOtraMarcaLanzaDuplicateName() {
        persistBrand("Otro", "otro");
        Brand brand = persistBrand("Original", "original");
        brandRepository.flush();

        assertThrows(DuplicateBrandNameException.class, () ->
                brandService.updateBrand(brand.getId(),
                        new UpdateBrandRequest("Otro", null, null, null)));
    }

    @Test
    void updateBrand_mismoNombreNoLanzaDuplicado() {
        Brand brand = persistBrand("Apple", "apple");
        brandRepository.flush();

        BrandResponse response = brandService.updateBrand(brand.getId(),
                new UpdateBrandRequest("Apple", null, null, null));

        assertEquals("Apple", response.name());
    }

    // ------------------------------------------------------------- deactivateBrand

    @Test
    void deactivateBrand_desactivaMarca() {
        Brand brand = persistBrand("Nokia", "nokia");
        brandRepository.flush();

        brandService.deactivateBrand(brand.getId());

        Brand updated = brandRepository.findById(brand.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
    }

    @Test
    void deactivateBrand_noExisteLanzaNotFound() {
        assertThrows(BrandNotFoundException.class, () ->
                brandService.deactivateBrand(UUID.randomUUID()));
    }

    // ------------------------------------------------------------- getActiveBrands

    @Test
    void getActiveBrands_devuelveSoloActivas() {
        persistBrand("Activa", "activa");
        Brand inactiva = persistBrand("Inactiva", "inactiva");
        inactiva.setIsActive(false);
        brandRepository.save(inactiva);
        brandRepository.flush();

        List<BrandResponse> response = brandService.getActiveBrands();

        assertTrue(response.stream().anyMatch(b -> b.name().equals("Activa")));
        assertTrue(response.stream().noneMatch(b -> b.name().equals("Inactiva")));
    }

    // --------------------------------------------------------------- helpers

    private Brand persistBrand(String name, String slug) {
        return brandRepository.save(BrandTestDataBuilder.aBrand()
                .withId(null)
                .withName(name)
                .withSlug(slug)
                .build());
    }
}