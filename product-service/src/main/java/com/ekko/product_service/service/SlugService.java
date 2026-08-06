package com.ekko.product_service.service;

import com.ekko.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlugService {

    private final ProductRepository productRepository;

    public String generate(String name) {
        if (name == null) {
            return "";
        }
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]+", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    public String generateUnique(String name, UUID sellerKeycloakId) {
        String base = generate(name);
        String slug = base;
        int suffix = 2;
        while (productRepository.existsBySellerKeycloakIdAndSlug(sellerKeycloakId, slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }
}