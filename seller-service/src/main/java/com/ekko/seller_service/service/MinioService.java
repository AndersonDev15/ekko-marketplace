package com.ekko.seller_service.service;

import com.ekko.seller_service.config.MinioProperties;
import com.ekko.seller_service.exception.MinioUploadException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioService {

    private static final String DOCUMENTS_FOLDER = "documents";
    private static final int PRESIGNED_EXPIRY_MINUTES = 15;

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MinioUploadException("Document file is required");
        }

        String objectKey = DOCUMENTS_FOLDER + "/" + UUID.randomUUID() + "/" + safeFileName(file.getOriginalFilename());

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            return objectKey;
        } catch (Exception e) {
            throw new MinioUploadException("Failed to upload document to MinIO", e);
        }
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new MinioUploadException("Failed to delete document from MinIO", e);
        }
    }

    public PresignedUrl generatePresignedUrl(String objectKey) {
        Instant expiresAt = Instant.now().plus(PRESIGNED_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .expiry(PRESIGNED_EXPIRY_MINUTES, TimeUnit.MINUTES)
                            .build());

            return new PresignedUrl(url, expiresAt);
        } catch (Exception e) {
            throw new MinioUploadException("Failed to generate download URL for document", e);
        }
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "document";
        }
        String name = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.isBlank() ? "document" : name;
    }

    public record PresignedUrl(String url, Instant expiresAt) {
    }
}