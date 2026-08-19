package com.ekko.seller_service.service;

import com.ekko.seller_service.config.MinioProperties;
import com.ekko.seller_service.exception.MinioUploadException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    private static final MinioProperties PROPERTIES = new MinioProperties(
            "http://localhost:9000", "ekko", "ekko12345", "ekko-documents");

    @Mock
    private MinioClient minioClient;

    private MinioService minioService;

    @BeforeEach
    void setUp() {
        minioService = new MinioService(minioClient, PROPERTIES);
    }

    @Test
    void upload_valido_subirObjetoYDevuelveObjectKey() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "id_card.pdf", "application/pdf", "pdf-content".getBytes());

        String objectKey = minioService.upload(file);

        assertTrue(objectKey.startsWith("documents/"));
        assertTrue(objectKey.endsWith("/id_card.pdf"));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void upload_archivoVacio_lanzaMinioUploadException() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(MinioUploadException.class, () -> minioService.upload(empty));

        verifyNoInteractions(minioClient);
    }

    @Test
    void upload_falloDeMinio_lanzaMinioUploadException() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("connection refused"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "id_card.pdf", "application/pdf", "pdf-content".getBytes());

        assertThrows(MinioUploadException.class, () -> minioService.upload(file));
    }

    @Test
    void delete_eliminaObjeto() throws Exception {
        minioService.delete("documents/abc-123/id_card.pdf");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void delete_conKeyNulo_noHaceNada() {
        minioService.delete(null);

        verifyNoInteractions(minioClient);
    }

    @Test
    void generatePresignedUrl_devuelveUrlConExpiracion() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/ekko-documents/documents/abc/id_card.pdf?X-Amz-Signature=xyz");

        MinioService.PresignedUrl result = minioService.generatePresignedUrl("documents/abc/id_card.pdf");

        assertEquals("http://localhost:9000/ekko-documents/documents/abc/id_card.pdf?X-Amz-Signature=xyz", result.url());
        assertNotNull(result.expiresAt());
        assertTrue(result.expiresAt().isAfter(java.time.Instant.now()));
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void generatePresignedUrl_falloDeMinio_lanzaMinioUploadException() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThrows(MinioUploadException.class,
                () -> minioService.generatePresignedUrl("documents/abc/id_card.pdf"));
    }

    @Test
    void upload_conNombreInseguro_saneaElNombre() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a/b\\c?.pdf", "application/pdf", "pdf-content".getBytes());

        String objectKey = minioService.upload(file);

        assertTrue(objectKey.endsWith("/a_b_c_.pdf"));
    }
}