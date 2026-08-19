package com.ekko.product_service.service;

import com.ekko.product_service.builder.ProductImageTestDataBuilder;
import com.ekko.product_service.builder.ProductTestDataBuilder;
import com.ekko.product_service.dto.request.CreateImageRequest;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.exception.ForbiddenProductAccessException;
import com.ekko.product_service.exception.ImageNotFoundException;
import com.ekko.product_service.exception.ImageUploadException;
import com.ekko.product_service.exception.InvalidImageOrderException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.repository.ProductImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ProductImageRepository imageRepository;

    @Mock
    private ActiveProductOwnershipValidator activeProductOwnershipValidator;

    @Mock
    private CloudinaryService cloudinaryService;

    private ImageService imageService;

    private final UUID productId = UUID.randomUUID();
    private final UUID imageId = UUID.randomUUID();
    private final UUID sellerId = SELLER_KEYCLOAK_ID;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(imageRepository, activeProductOwnershipValidator, cloudinaryService);
    }

    // ---------------------------------------------------------------- ownership

    @Test
    void addImage_propagaProductNotFoundExceptionCuandoElProductoNoExiste() {
        when(activeProductOwnershipValidator.validate(productId, sellerId))
                .thenThrow(ProductNotFoundException.class);

        assertThrows(ProductNotFoundException.class,
                () -> imageService.addImage(productId, imageRequest(false), sellerId));

        verify(imageRepository, never()).findByProductIdOrderBySortOrderAsc(productId);
    }

    @Test
    void addImage_propagaForbiddenProductAccessExceptionCuandoNoEsElDueno() {
        when(activeProductOwnershipValidator.validate(productId, sellerId))
                .thenThrow(ForbiddenProductAccessException.class);

        assertThrows(ForbiddenProductAccessException.class,
                () -> imageService.addImage(productId, imageRequest(false), sellerId));

        verify(imageRepository, never()).findByProductIdOrderBySortOrderAsc(productId);
    }

    @Test
    void deleteImage_propagaProductNotFoundExceptionCuandoElProductoNoExiste() {
        when(activeProductOwnershipValidator.validate(productId, sellerId))
                .thenThrow(ProductNotFoundException.class);

        assertThrows(ProductNotFoundException.class,
                () -> imageService.deleteImage(productId, imageId, sellerId));

        verify(imageRepository, never()).findById(imageId);
    }

    @Test
    void deleteImage_propagaForbiddenProductAccessExceptionCuandoNoEsElDueno() {
        when(activeProductOwnershipValidator.validate(productId, sellerId))
                .thenThrow(ForbiddenProductAccessException.class);

        assertThrows(ForbiddenProductAccessException.class,
                () -> imageService.deleteImage(productId, imageId, sellerId));

        verify(imageRepository, never()).findById(imageId);
    }

    @Test
    void setPrimaryImage_propagaProductNotFoundExceptionCuandoElProductoNoExiste() {
        when(activeProductOwnershipValidator.validate(productId, sellerId))
                .thenThrow(ProductNotFoundException.class);

        assertThrows(ProductNotFoundException.class,
                () -> imageService.setPrimaryImage(productId, imageId, sellerId));

        verify(imageRepository, never()).findById(imageId);
    }

    @Test
    void setPrimaryImage_propagaForbiddenProductAccessExceptionCuandoNoEsElDueno() {
        when(activeProductOwnershipValidator.validate(productId, sellerId))
                .thenThrow(ForbiddenProductAccessException.class);

        assertThrows(ForbiddenProductAccessException.class,
                () -> imageService.setPrimaryImage(productId, imageId, sellerId));

        verify(imageRepository, never()).findById(imageId);
    }

    @Test
    void reorderImages_propagaProductNotFoundExceptionCuandoElProductoNoExiste() {
        when(activeProductOwnershipValidator.validate(productId, sellerId))
                .thenThrow(ProductNotFoundException.class);

        assertThrows(ProductNotFoundException.class,
                () -> imageService.reorderImages(productId, List.of(imageId), sellerId));

        verify(imageRepository, never()).findByProductIdOrderBySortOrderAsc(productId);
    }

    @Test
    void reorderImages_propagaForbiddenProductAccessExceptionCuandoNoEsElDueno() {
        when(activeProductOwnershipValidator.validate(productId, sellerId))
                .thenThrow(ForbiddenProductAccessException.class);

        assertThrows(ForbiddenProductAccessException.class,
                () -> imageService.reorderImages(productId, List.of(imageId), sellerId));

        verify(imageRepository, never()).findByProductIdOrderBySortOrderAsc(productId);
    }

    // ---------------------------------------------------------------- addImage

    @Test
    void addImage_creaLaPrimeraImagenDelProducto() {
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());

        imageService.addImage(productId, imageRequest(false), sellerId);

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        ProductImage saved = imageCaptor.getValue();
        assertEquals(ownedProduct().getId(), saved.getProduct().getId());
        assertEquals("https://cdn.example.com/iphone-16.jpg", saved.getUrl());
        assertEquals(0, saved.getSortOrder());
    }

    @Test
    void addImage_laPrimeraImagenQuedaAutomaticamenteComoPrincipal() {
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());

        imageService.addImage(productId, imageRequest(false), sellerId);

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        assertTrue(imageCaptor.getValue().getIsPrimary());
    }

    @Test
    void addImage_siRequestIsPrimaryEsTrueLaNuevaImagenQuedaComoPrincipal() {
        ProductImage currentPrimary = ownedImage(imageId, true, 0);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(currentPrimary));
        when(imageRepository.findByProductIdAndIsPrimaryTrue(productId))
                .thenReturn(Optional.of(currentPrimary));

        imageService.addImage(productId, imageRequest(true), sellerId);

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        assertTrue(imageCaptor.getValue().getIsPrimary());
        assertTrue(imageCaptor.getValue().getSortOrder() > currentPrimary.getSortOrder());
    }

    @Test
    void addImage_cuandoLaNuevaEsPrincipalLaAnteriorDejaDeSerlo() {
        ProductImage currentPrimary = ownedImage(imageId, true, 0);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(currentPrimary));
        when(imageRepository.findByProductIdAndIsPrimaryTrue(productId))
                .thenReturn(Optional.of(currentPrimary));

        imageService.addImage(productId, imageRequest(true), sellerId);

        assertFalse(currentPrimary.getIsPrimary());
        verify(imageRepository).saveAndFlush(currentPrimary);
    }

    @Test
    void addImage_calculaElSortOrderParaLaPrimeraImagen() {
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());

        imageService.addImage(productId, imageRequest(false), sellerId);

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        assertEquals(0, imageCaptor.getValue().getSortOrder());
    }

    @Test
    void addImage_calculaElSortOrderCuandoYaExistenImagenes() {
        ProductImage last = ownedImage(imageId, false, 5);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of(last));

        imageService.addImage(productId, imageRequest(false), sellerId);

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        assertEquals(6, imageCaptor.getValue().getSortOrder());
    }

    @Test
    void addImage_devuelveProductImageResponse() {
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());

        ProductImageResponse response = imageService.addImage(productId, imageRequest(true), sellerId);

        assertEquals("https://cdn.example.com/iphone-16.jpg", response.url());
        assertEquals(true, response.isPrimary());
        assertEquals(0, response.sortOrder());
    }

    @Test
    void addImage_persisteElPublicIdDeCloudinary() {
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());

        imageService.addImage(productId, imageRequest(false), sellerId, "products/iphone-16");

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        assertEquals("products/iphone-16", imageCaptor.getValue().getPublicId());
    }

    @Test
    void addImage_sinPublicIdPersisteNull() {
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());

        imageService.addImage(productId, imageRequest(false), sellerId);

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        assertEquals(null, imageCaptor.getValue().getPublicId());
    }

    // ------------------------------------------------------------ uploadImage

    @Test
    void uploadImage_llamaACloudinaryYPersisteLaImagen() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "iphone-16.jpg", "image/jpeg", new byte[]{1, 2, 3});
        CloudinaryService.UploadResult upload = new CloudinaryService.UploadResult(
                "products/iphone-16",
                "https://res.cloudinary.com/gqrn3sdp/image/upload/v1/products/iphone-16.jpg");
        when(cloudinaryService.upload(file)).thenReturn(upload);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());

        imageService.uploadImage(productId, file, false, sellerId);

        verify(cloudinaryService).upload(file);
        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        ProductImage saved = imageCaptor.getValue();
        assertEquals("https://res.cloudinary.com/gqrn3sdp/image/upload/v1/products/iphone-16.jpg", saved.getUrl());
        assertEquals("products/iphone-16", saved.getPublicId());
        assertEquals(true, saved.getIsPrimary());
    }

    @Test
    void uploadImage_propagaErrorDeCloudinarySinPersistir() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "iphone-16.jpg", "image/jpeg", new byte[]{1});
        when(cloudinaryService.upload(file)).thenThrow(ImageUploadException.class);

        assertThrows(ImageUploadException.class,
                () -> imageService.uploadImage(productId, file, false, sellerId));

        verify(activeProductOwnershipValidator, never()).validate(any(), any());
        verify(imageRepository, never()).save(any());
    }

    // ------------------------------------------------------------- deleteImage

    @Test
    void deleteImage_eliminaUnaImagenNoPrincipal() {
        ProductImage image = ownedImage(imageId, false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        imageService.deleteImage(productId, imageId, sellerId);

        verify(imageRepository).delete(image);
    }

    @Test
    void deleteImage_conPublicIdBorraLaImagenDeCloudinary() {
        ProductImage image = ownedImage(imageId, false, 1);
        image.setPublicId("products/iphone-16");
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        imageService.deleteImage(productId, imageId, sellerId);

        verify(cloudinaryService).delete("products/iphone-16");
        verify(imageRepository).delete(image);
    }

    @Test
    void deleteImage_sinPublicIdNoLlamaACloudinary() {
        ProductImage image = ownedImage(imageId, false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        imageService.deleteImage(productId, imageId, sellerId);

        verify(cloudinaryService, never()).delete(any());
        verify(imageRepository).delete(image);
    }

    @Test
    void deleteImage_imagenNoPrincipalNoPromueveNada() {
        ProductImage image = ownedImage(imageId, false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        imageService.deleteImage(productId, imageId, sellerId);

        verify(imageRepository).delete(image);
        verify(imageRepository, never()).findByProductIdOrderBySortOrderAsc(productId);
        verify(imageRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteImage_imagenPrincipalPromueveLaSiguiente() {
        ProductImage primary = ownedImage(imageId, true, 0);
        ProductImage next = ownedImage(UUID.randomUUID(), false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(primary));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of(next));

        imageService.deleteImage(productId, imageId, sellerId);

        verify(imageRepository).delete(primary);
        assertTrue(next.getIsPrimary());
        verify(imageRepository).saveAndFlush(next);
    }

    @Test
    void deleteImage_imagenPrincipalSinImagenesRestantesNoPromueve() {
        ProductImage primary = ownedImage(imageId, true, 0);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(primary));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());

        imageService.deleteImage(productId, imageId, sellerId);

        verify(imageRepository).delete(primary);
        verify(imageRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteImage_lanzaImageNotFoundExceptionSiLaImagenNoExiste() {
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        assertThrows(ImageNotFoundException.class,
                () -> imageService.deleteImage(productId, imageId, sellerId));
    }

    @Test
    void deleteImage_lanzaImageNotFoundExceptionSiLaImagenNoPerteneceAlProducto() {
        ProductImage otherProductImage = ProductImageTestDataBuilder.anImage()
                .withId(imageId)
                .withProduct(ProductTestDataBuilder.aProduct().withId(UUID.randomUUID()).build())
                .withIsPrimary(false)
                .build();
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(otherProductImage));

        assertThrows(ImageNotFoundException.class,
                () -> imageService.deleteImage(productId, imageId, sellerId));

        verify(imageRepository, never()).delete(any());
    }

    // ---------------------------------------------------------- setPrimaryImage

    @Test
    void setPrimaryImage_cambiaCorrectamenteLaImagenPrincipal() {
        ProductImage target = ownedImage(imageId, false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(target));

        imageService.setPrimaryImage(productId, imageId, sellerId);

        assertTrue(target.getIsPrimary());
        verify(imageRepository).save(target);
    }

    @Test
    void setPrimaryImage_desmarcaLaPrincipalAnterior() {
        ProductImage currentPrimary = ownedImage(UUID.randomUUID(), true, 0);
        ProductImage target = ownedImage(imageId, false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(target));
        when(imageRepository.findByProductIdAndIsPrimaryTrue(productId))
                .thenReturn(Optional.of(currentPrimary));

        imageService.setPrimaryImage(productId, imageId, sellerId);

        assertFalse(currentPrimary.getIsPrimary());
        verify(imageRepository).saveAndFlush(currentPrimary);
        assertTrue(target.getIsPrimary());
        verify(imageRepository).save(target);
    }

    @Test
    void setPrimaryImage_noHaceNadaSiLaImagenYaEsPrincipal() {
        ProductImage target = ownedImage(imageId, true, 0);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(target));

        imageService.setPrimaryImage(productId, imageId, sellerId);

        verify(imageRepository, never()).findByProductIdAndIsPrimaryTrue(productId);
        verify(imageRepository, never()).saveAndFlush(any());
        verify(imageRepository, never()).save(any());
    }

    @Test
    void setPrimaryImage_sinPrimariaActualSoloMarcaLaNueva() {
        ProductImage target = ownedImage(imageId, false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(target));
        when(imageRepository.findByProductIdAndIsPrimaryTrue(productId)).thenReturn(Optional.empty());

        imageService.setPrimaryImage(productId, imageId, sellerId);

        assertTrue(target.getIsPrimary());
        verify(imageRepository).save(target);
        verify(imageRepository, never()).saveAndFlush(any());
    }

    @Test
    void setPrimaryImage_lanzaImageNotFoundExceptionSiLaImagenNoExiste() {
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        assertThrows(ImageNotFoundException.class,
                () -> imageService.setPrimaryImage(productId, imageId, sellerId));
    }

    @Test
    void setPrimaryImage_lanzaImageNotFoundExceptionSiLaImagenNoPerteneceAlProducto() {
        ProductImage otherProductImage = ProductImageTestDataBuilder.anImage()
                .withId(imageId)
                .withProduct(ProductTestDataBuilder.aProduct().withId(UUID.randomUUID()).build())
                .withIsPrimary(false)
                .build();
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(otherProductImage));

        assertThrows(ImageNotFoundException.class,
                () -> imageService.setPrimaryImage(productId, imageId, sellerId));

        verify(imageRepository, never()).save(any());
    }

    // ----------------------------------------------------------- reorderImages

    @Test
    void reorderImages_reordenaCorrectamenteLasImagenes() {
        ProductImage a = ownedImage(UUID.randomUUID(), true, 0);
        ProductImage b = ownedImage(UUID.randomUUID(), false, 1);
        ProductImage c = ownedImage(UUID.randomUUID(), false, 2);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(a, b, c));

        imageService.reorderImages(productId, List.of(c.getId(), a.getId(), b.getId()), sellerId);

        ArgumentCaptor<List<ProductImage>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(imageRepository).saveAll(listCaptor.capture());
        List<ProductImage> saved = listCaptor.getValue();
        assertEquals(0, saved.stream().filter(i -> i.getId().equals(c.getId())).findFirst().orElseThrow().getSortOrder());
        assertEquals(1, saved.stream().filter(i -> i.getId().equals(a.getId())).findFirst().orElseThrow().getSortOrder());
        assertEquals(2, saved.stream().filter(i -> i.getId().equals(b.getId())).findFirst().orElseThrow().getSortOrder());
    }

    @Test
    void reorderImages_actualizaLosSortOrder() {
        ProductImage a = ownedImage(UUID.randomUUID(), false, 0);
        ProductImage b = ownedImage(UUID.randomUUID(), false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(a, b));

        imageService.reorderImages(productId, List.of(b.getId(), a.getId()), sellerId);

        ArgumentCaptor<List<ProductImage>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(imageRepository).saveAll(listCaptor.capture());
        List<ProductImage> saved = listCaptor.getValue();
        assertEquals(0, saved.stream().filter(i -> i.getId().equals(b.getId())).findFirst().orElseThrow().getSortOrder());
        assertEquals(1, saved.stream().filter(i -> i.getId().equals(a.getId())).findFirst().orElseThrow().getSortOrder());
    }

    @Test
    void reorderImages_lanzaInvalidImageOrderExceptionSiFaltanImagenes() {
        ProductImage a = ownedImage(UUID.randomUUID(), false, 0);
        ProductImage b = ownedImage(UUID.randomUUID(), false, 1);
        ProductImage c = ownedImage(UUID.randomUUID(), false, 2);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(a, b, c));

        assertThrows(InvalidImageOrderException.class,
                () -> imageService.reorderImages(productId, List.of(a.getId(), b.getId()), sellerId));

        verify(imageRepository, never()).saveAll(any());
    }

    @Test
    void reorderImages_lanzaInvalidImageOrderExceptionSiSobranImagenes() {
        ProductImage a = ownedImage(UUID.randomUUID(), false, 0);
        ProductImage b = ownedImage(UUID.randomUUID(), false, 1);
        ProductImage c = ownedImage(UUID.randomUUID(), false, 2);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(a, b));

        assertThrows(InvalidImageOrderException.class,
                () -> imageService.reorderImages(
                        productId, List.of(a.getId(), b.getId(), c.getId()), sellerId));

        verify(imageRepository, never()).saveAll(any());
    }

    @Test
    void reorderImages_lanzaInvalidImageOrderExceptionSiHayIdsDuplicados() {
        ProductImage a = ownedImage(UUID.randomUUID(), false, 0);
        ProductImage b = ownedImage(UUID.randomUUID(), false, 1);
        ProductImage c = ownedImage(UUID.randomUUID(), false, 2);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(a, b, c));

        assertThrows(InvalidImageOrderException.class,
                () -> imageService.reorderImages(
                        productId, List.of(a.getId(), a.getId(), b.getId()), sellerId));

        verify(imageRepository, never()).saveAll(any());
    }

    @Test
    void reorderImages_lanzaInvalidImageOrderExceptionSiHayIdsQueNoPertenecenAlProducto() {
        ProductImage a = ownedImage(UUID.randomUUID(), false, 0);
        ProductImage b = ownedImage(UUID.randomUUID(), false, 1);
        when(activeProductOwnershipValidator.validate(productId, sellerId)).thenReturn(ownedProduct());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(a, b));

        assertThrows(InvalidImageOrderException.class,
                () -> imageService.reorderImages(
                        productId, List.of(a.getId(), b.getId(), UUID.randomUUID()), sellerId));

        verify(imageRepository, never()).saveAll(any());
    }

    // ------------------------------------------------------------------- helpers

    private Product ownedProduct() {
        return ProductTestDataBuilder.aProduct().withId(productId).build();
    }

    private ProductImage ownedImage(UUID id, Boolean isPrimary, int sortOrder) {
        return ProductImageTestDataBuilder.anImage()
                .withId(id)
                .withProduct(ownedProduct())
                .withIsPrimary(isPrimary)
                .withSortOrder(sortOrder)
                .build();
    }

    private CreateImageRequest imageRequest(boolean isPrimary) {
        return new CreateImageRequest("https://cdn.example.com/iphone-16.jpg", isPrimary);
    }
}