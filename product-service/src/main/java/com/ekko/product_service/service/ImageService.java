package com.ekko.product_service.service;

import com.ekko.product_service.dto.request.CreateImageRequest;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.exception.ImageNotFoundException;
import com.ekko.product_service.exception.InvalidImageOrderException;
import com.ekko.product_service.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ProductImageRepository imageRepository;
    private final ActiveProductOwnershipValidator activeProductOwnershipValidator;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public ProductImageResponse uploadImage(UUID productId, MultipartFile file, Boolean isPrimary,
                                            UUID sellerKeycloakId) {
        CloudinaryService.UploadResult upload = cloudinaryService.upload(file);
        return addImage(productId, new CreateImageRequest(upload.url(), isPrimary), sellerKeycloakId,
                upload.publicId());
    }

    @Transactional
    public ProductImageResponse addImage(UUID productId, CreateImageRequest request, UUID sellerKeycloakId) {
        return addImage(productId, request, sellerKeycloakId, null);
    }

    @Transactional
    public ProductImageResponse addImage(UUID productId, CreateImageRequest request, UUID sellerKeycloakId,
                                         String publicId) {
        Product product = activeProductOwnershipValidator.validate(productId, sellerKeycloakId);

        List<ProductImage> images = imageRepository.findByProductIdOrderBySortOrderAsc(productId);
        boolean isFirst = images.isEmpty();
        boolean isPrimary = Boolean.TRUE.equals(request.isPrimary()) || isFirst;

        if (isPrimary && !isFirst) {
            imageRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .ifPresent(currentPrimary -> {
                        currentPrimary.setIsPrimary(false);
                        imageRepository.saveAndFlush(currentPrimary);
                    });
        }

        int sortOrder = images.isEmpty() ? 0 : images.get(images.size() - 1).getSortOrder() + 1;

        ProductImage image = ProductImage.builder()
                .product(product)
                .url(request.url())
                .publicId(publicId)
                .isPrimary(isPrimary)
                .sortOrder(sortOrder)
                .createdAt(LocalDateTime.now())
                .build();
        imageRepository.save(image);

        return toResponse(image);
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId, UUID sellerKeycloakId) {
        activeProductOwnershipValidator.validate(productId, sellerKeycloakId);

        ProductImage image = findOwnedImage(productId, imageId);

        if (image.getPublicId() != null) {
            cloudinaryService.delete(image.getPublicId());
        }

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        imageRepository.delete(image);

        if (wasPrimary) {
            List<ProductImage> remaining = imageRepository.findByProductIdOrderBySortOrderAsc(productId);
            if (!remaining.isEmpty()) {
                ProductImage nextPrimary = remaining.get(0);
                nextPrimary.setIsPrimary(true);
                imageRepository.saveAndFlush(nextPrimary);
            }
        }
    }

    @Transactional
    public void setPrimaryImage(UUID productId, UUID imageId, UUID sellerKeycloakId) {
        activeProductOwnershipValidator.validate(productId, sellerKeycloakId);

        ProductImage image = findOwnedImage(productId, imageId);

        if (Boolean.TRUE.equals(image.getIsPrimary())) {
            return;
        }

        imageRepository.findByProductIdAndIsPrimaryTrue(productId)
                .ifPresent(currentPrimary -> {
                    currentPrimary.setIsPrimary(false);
                    imageRepository.saveAndFlush(currentPrimary);
                });

        image.setIsPrimary(true);
        imageRepository.save(image);
    }

    @Transactional
    public void reorderImages(UUID productId, List<UUID> orderedImageIds, UUID sellerKeycloakId) {
        activeProductOwnershipValidator.validate(productId, sellerKeycloakId);

        List<ProductImage> images = imageRepository.findByProductIdOrderBySortOrderAsc(productId);

        if (images.size() != orderedImageIds.size()
                || !new HashSet<>(orderedImageIds).equals(
                        images.stream().map(ProductImage::getId).collect(java.util.stream.Collectors.toSet()))) {
            throw new InvalidImageOrderException();
        }

        for (int index = 0; index < orderedImageIds.size(); index++) {
            UUID imageId = orderedImageIds.get(index);
            ProductImage image = images.stream()
                    .filter(i -> i.getId().equals(imageId))
                    .findFirst()
                    .orElseThrow(InvalidImageOrderException::new);
            image.setSortOrder(index);
        }

        imageRepository.saveAll(images);
    }

    private ProductImage findOwnedImage(UUID productId, UUID imageId) {
        return imageRepository.findById(imageId)
                .filter(i -> i.getProduct().getId().equals(productId))
                .orElseThrow(ImageNotFoundException::new);
    }

    private ProductImageResponse toResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getUrl(),
                image.getIsPrimary(),
                image.getSortOrder());
    }
}