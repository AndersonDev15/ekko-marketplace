package com.ekko.product_service.service;

import com.ekko.product_service.exception.ImageUploadException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final String IMAGES_FOLDER = "products";

    private final Cloudinary cloudinary;

    public UploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("Image file is required");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", IMAGES_FOLDER,
                            "resource_type", "image",
                            "overwrite", true));

            return new UploadResult(
                    (String) result.get("public_id"),
                    (String) result.get("secure_url"));
        } catch (Exception e) {
            throw new ImageUploadException("Failed to upload image to Cloudinary", e);
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new ImageUploadException("Failed to delete image from Cloudinary", e);
        }
    }

    public record UploadResult(String publicId, String url) {
    }
}