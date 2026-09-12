package com.ekko.product_service.service;

import com.ekko.product_service.builder.BrandTestDataBuilder;
import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.request.CreateImageRequest;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.ForbiddenProductAccessException;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductImageRepository;
import com.ekko.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Transactional
class ImageServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ImageService imageService;

    @Autowired
    private ProductImageRepository imageRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private final UUID sellerId = SELLER_KEYCLOAK_ID;
    private final UUID otherSellerId = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    @MockitoBean
    private CloudinaryService cloudinaryService;

    // ------------------------------------------------------------ uploadImage

    @Test
    void uploadImage_subeACloudinaryYPersisteUrlYPublicId() {
        Product product = persistProduct(sellerId);
        MockMultipartFile file = new MockMultipartFile(
                "file", "iphone-16.jpg", "image/jpeg", new byte[]{1, 2, 3});
        CloudinaryService.UploadResult upload = new CloudinaryService.UploadResult(
                "products/iphone-16",
                "https://res.cloudinary.com/gqrn3sdp/image/upload/v1/products/iphone-16.jpg");
        when(cloudinaryService.upload(file, "products")).thenReturn(upload);

        ProductImageResponse response = imageService.uploadImage(product.getId(), file, false, sellerId);

        ProductImage saved = imageRepository.findById(response.id()).orElseThrow();
        assertEquals("https://res.cloudinary.com/gqrn3sdp/image/upload/v1/products/iphone-16.jpg", saved.getUrl());
        assertEquals("products/iphone-16", saved.getPublicId());
        assertEquals(true, saved.getIsPrimary());
    }

    // ---------------------------------------------------------------- addImage

    @Test
    void addImage_persisteLaImagenEnLaTablaProductImages() {
        Product product = persistProduct(sellerId);

        ProductImageResponse response = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        ProductImage saved = imageRepository.findById(response.id()).orElseThrow();
        assertNotNull(saved);
        assertEquals(product.getId(), saved.getProduct().getId());
        assertEquals("https://cdn.example.com/iphone-16.jpg", saved.getUrl());
    }

    @Test
    void addImage_laPrimeraImagenQuedaComoPrimariaEnBd() {
        Product product = persistProduct(sellerId);

        ProductImageResponse response = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        ProductImage saved = imageRepository.findById(response.id()).orElseThrow();
        assertEquals(true, saved.getIsPrimary());
    }

    @Test
    void addImage_unaNuevaPrimariaDesmarcaLaAnteriorEnBd() {
        Product product = persistProduct(sellerId);
        ProductImageResponse first = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);
        ProductImageResponse second = imageService.addImage(
                product.getId(), imageRequest(true), sellerId);

        ProductImage oldPrimary = imageRepository.findById(first.id()).orElseThrow();
        ProductImage newPrimary = imageRepository.findById(second.id()).orElseThrow();
        assertEquals(false, oldPrimary.getIsPrimary());
        assertEquals(true, newPrimary.getIsPrimary());
    }

    @Test
    void addImage_calculaYPersisteElSortOrderConsecutivo() {
        Product product = persistProduct(sellerId);

        ProductImageResponse first = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);
        ProductImageResponse second = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);
        ProductImageResponse third = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        assertEquals(0, imageRepository.findById(first.id()).orElseThrow().getSortOrder());
        assertEquals(1, imageRepository.findById(second.id()).orElseThrow().getSortOrder());
        assertEquals(2, imageRepository.findById(third.id()).orElseThrow().getSortOrder());
    }

    // ------------------------------------------------------------- deleteImage

    @Test
    void deleteImage_eliminaLaImagenFisicamenteDeLaTabla() {
        Product product = persistProduct(sellerId);
        ProductImageResponse image = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        imageService.deleteImage(product.getId(), image.id(), sellerId);

        assertTrue(imageRepository.findById(image.id()).isEmpty());
    }

    @Test
    void deleteImage_alBorrarLaPrimariaPromueveLaSiguienteEnBd() {
        Product product = persistProduct(sellerId);
        ProductImageResponse first = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);
        ProductImageResponse second = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        imageService.deleteImage(product.getId(), first.id(), sellerId);

        ProductImage promoted = imageRepository.findById(second.id()).orElseThrow();
        assertEquals(true, promoted.getIsPrimary());
        assertTrue(imageRepository.findById(first.id()).isEmpty());
    }

    @Test
    void deleteImage_imagenDeOtroSellerLanzaForbiddenProductAccessException() {
        Product product = persistProduct(sellerId);
        ProductImageResponse image = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        assertThrows(ForbiddenProductAccessException.class,
                () -> imageService.deleteImage(product.getId(), image.id(), otherSellerId));

        assertTrue(imageRepository.findById(image.id()).isPresent());
    }

    // ---------------------------------------------------------- setPrimaryImage

    @Test
    void setPrimaryImage_persisteElCambioDePrimariaEnBd() {
        Product product = persistProduct(sellerId);
        ProductImageResponse first = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);
        ProductImageResponse second = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        imageService.setPrimaryImage(product.getId(), second.id(), sellerId);

        ProductImage oldPrimary = imageRepository.findById(first.id()).orElseThrow();
        ProductImage newPrimary = imageRepository.findById(second.id()).orElseThrow();
        assertEquals(false, oldPrimary.getIsPrimary());
        assertEquals(true, newPrimary.getIsPrimary());
    }

    @Test
    void setPrimaryImage_sinPrimariaActualMarcaLaNuevaSinFallar() {
        Product product = persistProduct(sellerId);
        ProductImageResponse image = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);
        imageRepository.findById(image.id()).ifPresent(current -> {
            current.setIsPrimary(false);
            imageRepository.saveAndFlush(current);
        });
        Optional<ProductImage> none = imageRepository.findByProductIdAndIsPrimaryTrue(product.getId());
        assertTrue(none.isEmpty());

        imageService.setPrimaryImage(product.getId(), image.id(), sellerId);

        ProductImage saved = imageRepository.findById(image.id()).orElseThrow();
        assertEquals(true, saved.getIsPrimary());
    }

    // ----------------------------------------------------------- reorderImages

    @Test
    void reorderImages_persisteLosSortOrderEnElOrdenDado() {
        Product product = persistProduct(sellerId);
        ProductImageResponse a = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);
        ProductImageResponse b = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);
        ProductImageResponse c = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        imageService.reorderImages(product.getId(), List.of(c.id(), a.id(), b.id()), sellerId);

        List<ProductImage> ordered = imageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        assertEquals(3, ordered.size());
        assertEquals(c.id(), ordered.get(0).getId());
        assertEquals(a.id(), ordered.get(1).getId());
        assertEquals(b.id(), ordered.get(2).getId());
        assertEquals(0, ordered.get(0).getSortOrder());
        assertEquals(1, ordered.get(1).getSortOrder());
        assertEquals(2, ordered.get(2).getSortOrder());
    }

    // ------------------------------------------------- ownership con datos reales

    @Test
    void addImage_deOtroSellerLanzaForbiddenProductAccessException() {
        Product product = persistProduct(sellerId);

        assertThrows(ForbiddenProductAccessException.class,
                () -> imageService.addImage(product.getId(), imageRequest(false), otherSellerId));
    }

    @Test
    void setPrimaryImage_deOtroSellerLanzaForbiddenProductAccessException() {
        Product product = persistProduct(sellerId);
        ProductImageResponse image = imageService.addImage(
                product.getId(), imageRequest(false), sellerId);

        assertThrows(ForbiddenProductAccessException.class,
                () -> imageService.setPrimaryImage(product.getId(), image.id(), otherSellerId));

        ProductImage unchanged = imageRepository.findById(image.id()).orElseThrow();
        assertEquals(true, unchanged.getIsPrimary());
    }

    // ------------------------------------------------------------------- helpers

    private CreateImageRequest imageRequest(boolean isPrimary) {
        return new CreateImageRequest("https://cdn.example.com/iphone-16.jpg", isPrimary);
    }

    private Product persistProduct(UUID sellerKeycloakId) {
        Brand brand = BrandTestDataBuilder.aBrand()
                .withId(null)
                .withName("Brand-" + UUID.randomUUID().toString().substring(0, 8))
                .withSlug("brand-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        brand = brandRepository.save(brand);

        Category category = CategoryTestDataBuilder.aCategory()
                .withId(null)
                .withSlug("category-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        category = categoryRepository.save(category);

        Product product = ProductTestDataBuilder.aProduct()
                .withId(null)
                .withBrand(brand)
                .withCategory(category)
                .withSellerKeycloakId(sellerKeycloakId)
                .withName("iPhone 16")
                .withSlug("iphone-16-" + UUID.randomUUID().toString().substring(0, 8))
                .withStatus(ProductStatus.DRAFT)
                .build();
        return productRepository.save(product);
    }
}