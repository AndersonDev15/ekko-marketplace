package com.ekko.product_service.service;

import com.ekko.product_service.exception.ImageUploadException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    private static final String PUBLIC_ID = "products/iphone-16";
    private static final String SECURE_URL = "https://res.cloudinary.com/gqrn3sdp/image/upload/v1/products/iphone-16.jpg";

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        cloudinaryService = new CloudinaryService(cloudinary);
    }

    @Test
    void upload_devuelvePublicIdYUrl() throws Exception {
        Map<Object, Object> result = new HashMap<>();
        result.put("public_id", PUBLIC_ID);
        result.put("secure_url", SECURE_URL);
        when(uploader.upload(any(), any())).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile(
                "file", "iphone-16.jpg", "image/jpeg", new byte[]{1, 2, 3});

        CloudinaryService.UploadResult upload = cloudinaryService.upload(file, "products");

        assertEquals(PUBLIC_ID, upload.publicId());
        assertEquals(SECURE_URL, upload.url());
    }

    @Test
    void upload_archivoVacioLanzaImageUploadException() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(ImageUploadException.class, () -> cloudinaryService.upload(empty, "products"));
    }

    @Test
    void upload_falloDeCloudinaryLanzaImageUploadException() throws Exception {
        when(uploader.upload(any(), any())).thenThrow(new RuntimeException("boom"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "iphone-16.jpg", "image/jpeg", new byte[]{1});

        assertThrows(ImageUploadException.class, () -> cloudinaryService.upload(file, "products"));
    }

    @Test
    void delete_destruyeElPublicIdEnCloudinary() throws Exception {
        cloudinaryService.delete(PUBLIC_ID);

        verify(uploader).destroy(PUBLIC_ID, Map.of());
    }

    @Test
    void delete_falloDeCloudinaryLanzaImageUploadException() throws Exception {
        when(uploader.destroy(any(), any())).thenThrow(new RuntimeException("boom"));

        assertThrows(ImageUploadException.class, () -> cloudinaryService.delete(PUBLIC_ID));
    }
}