package com.ekko.seller_service.service;

import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

@Service
@RequiredArgsConstructor
public class SellerSlugService {

    private final SellerRepository sellerRepository;

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

    public String generateUnique(String name) {
        String base = generate(name);
        String slug = base;
        int suffix = 2;
        while (sellerRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }
}
