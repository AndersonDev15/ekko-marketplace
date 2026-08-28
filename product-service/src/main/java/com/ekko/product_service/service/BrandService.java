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
    private final SlugService slugService;

    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        if (brandRepository.existsByName(request.name())) {
            throw new DuplicateBrandNameException();
        }
        String slug = generateUniqueSlug(request.name());

        Brand.BrandBuilder builder = Brand.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());



        Brand brand = builder.build();
        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse updateBrand(UUID brandId, UpdateBrandRequest request) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(BrandNotFoundException::new);

        if (request.name() != null
                && !request.name().equals(brand.getName())
                && brandRepository.existsByNameAndIdNot(request.name(), brandId)) {
            throw new DuplicateBrandNameException();
        }

        if (request.name() != null
                && !request.name().equals(brand.getName())) {

            String slug = generateUniqueSlug(request.name(), brandId);

            brand.setName(request.name());
            brand.setSlug(slug);
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

    private String generateUniqueSlug(String name) {

        String baseSlug = slugService.generate(name);
        String slug = baseSlug;
        int suffix = 2;

        while (brandRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }
    private String generateUniqueSlug(String name, UUID brandId) {
        String baseSlug = slugService.generate(name);
        String slug = baseSlug;
        int suffix = 2;

        while (brandRepository.existsBySlugAndIdNot(slug, brandId)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
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