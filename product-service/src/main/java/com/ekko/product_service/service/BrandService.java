package com.ekko.product_service.service;

import com.ekko.product_service.dto.request.CreateBrandRequest;
import com.ekko.product_service.dto.request.UpdateBrandRequest;
import com.ekko.product_service.dto.response.BrandResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.exception.BrandNotFoundException;
import com.ekko.product_service.exception.DuplicateBrandNameException;
import com.ekko.product_service.exception.DuplicateBrandSlugException;
import com.ekko.product_service.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request, MultipartFile logo) {
        if (brandRepository.existsByName(request.name())) {
            throw new DuplicateBrandNameException();
        }
        if (brandRepository.existsBySlug(request.slug())) {
            throw new DuplicateBrandSlugException();
        }

        Brand.BrandBuilder builder = Brand.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());

        if (logo != null && !logo.isEmpty()) {
            CloudinaryService.UploadResult upload = cloudinaryService.upload(logo, "brands");
            builder.logoUrl(upload.url())
                    .logoPublicId(upload.publicId());
        }


        Brand brand = builder.build();
        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse updateBrand(UUID brandId, UpdateBrandRequest request) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(BrandNotFoundException::new);

        if (request.name() != null && !request.name().equals(brand.getName())
                && brandRepository.existsByNameAndIdNot(request.name(), brandId)) {
            throw new DuplicateBrandNameException();
        }
        if (request.slug() != null && !request.slug().equals(brand.getSlug())
                && brandRepository.existsBySlugAndIdNot(request.slug(), brandId)) {
            throw new DuplicateBrandSlugException();
        }

        if (request.name() != null) {
            brand.setName(request.name());
        }
        if (request.slug() != null) {
            brand.setSlug(request.slug());
        }
        if (request.description() != null) {
            brand.setDescription(request.description());
        }
        brand.setUpdatedAt(LocalDateTime.now());
        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse updateLogo(UUID brandId, MultipartFile logo) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(BrandNotFoundException::new);

        if (brand.getLogoPublicId() != null) {
            cloudinaryService.delete(brand.getLogoPublicId());
        }

        CloudinaryService.UploadResult upload = cloudinaryService.upload(logo, "brands");

        brand.setLogoUrl(upload.url());
        brand.setLogoPublicId(upload.publicId());

        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public void deactivateBrand(UUID brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(BrandNotFoundException::new);
        brand.setIsActive(false);
        brand.setUpdatedAt(LocalDateTime.now());
        brandRepository.save(brand);
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        return brandRepository.findAllByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getLogoUrl(),
                brand.getDescription(),
                brand.getIsActive());
    }
}