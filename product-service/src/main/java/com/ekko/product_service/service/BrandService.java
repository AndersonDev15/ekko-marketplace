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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        if (brandRepository.existsByName(request.name())) {
            throw new DuplicateBrandNameException();
        }
        if (brandRepository.existsBySlug(request.slug())) {
            throw new DuplicateBrandSlugException();
        }

        Brand brand = new Brand();
        brand.setName(request.name());
        brand.setSlug(request.slug());
        brand.setLogoUrl(request.logoUrl());
        brand.setDescription(request.description());
        brand.setIsActive(true);
        brand.setCreatedAt(LocalDateTime.now());
        brand.setUpdatedAt(LocalDateTime.now());
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
        if (request.logoUrl() != null) {
            brand.setLogoUrl(request.logoUrl());
        }
        if (request.description() != null) {
            brand.setDescription(request.description());
        }
        brand.setUpdatedAt(LocalDateTime.now());
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