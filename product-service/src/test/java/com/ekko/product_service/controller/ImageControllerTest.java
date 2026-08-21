package com.ekko.product_service.controller;

import com.ekko.product_service.config.SecurityConfig;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.exception.ForbiddenProductAccessException;
import com.ekko.product_service.exception.ImageNotFoundException;
import com.ekko.product_service.exception.ImageUploadException;
import com.ekko.product_service.exception.InvalidImageOrderException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.service.ImageService;
import com.ekko.product_service.util.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
@Import(SecurityConfig.class)
class ImageControllerTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID IMAGE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = SELLER_KEYCLOAK_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------ POST /seller/products/{id}/images (multipart)

    @Test
    void postImageMultipart_devuelve201YProductImageResponse() throws Exception {
        ProductImageResponse response = productImageResponse(true, 0);
        when(imageService.uploadImage(eq(PRODUCT_ID), any(), eq(true), eq(SELLER_ID)))
                .thenReturn(response);

        mockMvc.perform(multipart(baseUrl())
                        .file(new MockMultipartFile("file", "iphone-16.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .param("isPrimary", "true")
                        .with(sellerAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(IMAGE_ID.toString()))
                .andExpect(jsonPath("$.url").value("https://cdn.example.com/images/iphone-16.jpg"))
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andExpect(jsonPath("$.sortOrder").value(0));

        verify(imageService).uploadImage(eq(PRODUCT_ID), any(), eq(true), eq(SELLER_ID));
    }

    @Test
    void postImageMultipart_sinIsPrimaryEnviaNullAlService() throws Exception {
        when(imageService.uploadImage(eq(PRODUCT_ID), any(), isNull(), eq(SELLER_ID)))
                .thenReturn(productImageResponse(false, 0));

        mockMvc.perform(multipart(baseUrl())
                        .file(new MockMultipartFile("file", "iphone-16.jpg", "image/jpeg", new byte[]{1}))
                        .with(sellerAuth()))
                .andExpect(status().isCreated());

        verify(imageService).uploadImage(eq(PRODUCT_ID), any(), isNull(), eq(SELLER_ID));
    }

    @Test
    void postImageMultipart_devuelve401SinJWT() throws Exception {
        mockMvc.perform(multipart(baseUrl())
                        .file(new MockMultipartFile("file", "iphone-16.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postImageMultipart_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(multipart(baseUrl())
                        .file(new MockMultipartFile("file", "iphone-16.jpg", "image/jpeg", new byte[]{1}))
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void postImageMultipart_imageServiceLanzaProductNotFoundExceptionDevuelve404() throws Exception {
        when(imageService.uploadImage(eq(PRODUCT_ID), any(), any(), eq(SELLER_ID)))
                .thenThrow(ProductNotFoundException.class);

        mockMvc.perform(multipart(baseUrl())
                        .file(new MockMultipartFile("file", "iphone-16.jpg", "image/jpeg", new byte[]{1}))
                        .with(sellerAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postImageMultipart_imageServiceLanzaForbiddenProductAccessExceptionDevuelve403() throws Exception {
        when(imageService.uploadImage(eq(PRODUCT_ID), any(), any(), eq(SELLER_ID)))
                .thenThrow(ForbiddenProductAccessException.class);

        mockMvc.perform(multipart(baseUrl())
                        .file(new MockMultipartFile("file", "iphone-16.jpg", "image/jpeg", new byte[]{1}))
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void postImageMultipart_imageServiceLanzaImageUploadExceptionDevuelve502() throws Exception {
        when(imageService.uploadImage(eq(PRODUCT_ID), any(), any(), eq(SELLER_ID)))
                .thenThrow(ImageUploadException.class);

        mockMvc.perform(multipart(baseUrl())
                        .file(new MockMultipartFile("file", "iphone-16.jpg", "image/jpeg", new byte[]{1}))
                        .with(sellerAuth()))
                .andExpect(status().isBadGateway());

        verify(imageService, never()).addImage(any(), any(), any());
    }

    // ------------------------------------------------------ DELETE /seller/products/{id}/images/{imageId}

    @Test
    void deleteImage_devuelve204() throws Exception {
        mockMvc.perform(delete(imageUrl())
                        .with(sellerAuth()))
                .andExpect(status().isNoContent());

        verify(imageService).deleteImage(eq(PRODUCT_ID), eq(IMAGE_ID), eq(SELLER_ID));
    }

    @Test
    void deleteImage_devuelve401SinJWT() throws Exception {
        mockMvc.perform(delete(imageUrl()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteImage_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(delete(imageUrl())
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteImage_imageServiceLanzaImageNotFoundExceptionDevuelve404() throws Exception {
        doThrow(ImageNotFoundException.class)
                .when(imageService).deleteImage(eq(PRODUCT_ID), eq(IMAGE_ID), eq(SELLER_ID));

        mockMvc.perform(delete(imageUrl())
                        .with(sellerAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteImage_imageServiceLanzaForbiddenProductAccessExceptionDevuelve403() throws Exception {
        doThrow(ForbiddenProductAccessException.class)
                .when(imageService).deleteImage(eq(PRODUCT_ID), eq(IMAGE_ID), eq(SELLER_ID));

        mockMvc.perform(delete(imageUrl())
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ PATCH /seller/products/{id}/images/{imageId}/primary

    @Test
    void setPrimaryImage_devuelve204() throws Exception {
        mockMvc.perform(patch(primaryUrl())
                        .with(sellerAuth()))
                .andExpect(status().isNoContent());

        verify(imageService).setPrimaryImage(eq(PRODUCT_ID), eq(IMAGE_ID), eq(SELLER_ID));
    }

    @Test
    void setPrimaryImage_devuelve401SinJWT() throws Exception {
        mockMvc.perform(patch(primaryUrl()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setPrimaryImage_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(patch(primaryUrl())
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void setPrimaryImage_imageServiceLanzaImageNotFoundExceptionDevuelve404() throws Exception {
        doThrow(ImageNotFoundException.class)
                .when(imageService).setPrimaryImage(eq(PRODUCT_ID), eq(IMAGE_ID), eq(SELLER_ID));

        mockMvc.perform(patch(primaryUrl())
                        .with(sellerAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void setPrimaryImage_imageServiceLanzaForbiddenProductAccessExceptionDevuelve403() throws Exception {
        doThrow(ForbiddenProductAccessException.class)
                .when(imageService).setPrimaryImage(eq(PRODUCT_ID), eq(IMAGE_ID), eq(SELLER_ID));

        mockMvc.perform(patch(primaryUrl())
                        .with(sellerAuth()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ PATCH /seller/products/{id}/images/reorder

    @Test
    void reorderImages_devuelve204() throws Exception {
        mockMvc.perform(patch(reorderUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageIds": ["%s", "%s"]
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isNoContent());

        verify(imageService).reorderImages(eq(PRODUCT_ID), any(), eq(SELLER_ID));
    }

    @Test
    void reorderImages_devuelve401SinJWT() throws Exception {
        mockMvc.perform(patch(reorderUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageIds": ["%s"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reorderImages_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(patch(reorderUrl())
                        .with(customerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageIds": ["%s"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void reorderImages_imageServiceLanzaInvalidImageOrderExceptionDevuelve400() throws Exception {
        doThrow(InvalidImageOrderException.class)
                .when(imageService).reorderImages(eq(PRODUCT_ID), any(), eq(SELLER_ID));

        mockMvc.perform(patch(reorderUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageIds": ["%s", "%s"]
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorderImages_lanzaProductNotFoundExceptionDevuelve404() throws Exception {
        doThrow(ProductNotFoundException.class)
                .when(imageService).reorderImages(eq(PRODUCT_ID), any(), eq(SELLER_ID));

        mockMvc.perform(patch(reorderUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageIds": ["%s"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void reorderImages_bodyConJsonInvalidoDevuelve400SinLlamarAlService() throws Exception {
        mockMvc.perform(patch(reorderUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageIds": "un-array-requerido"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(imageService, never()).reorderImages(any(), any(), any());
    }

    // ------------------------------------------------------------------- helpers

    private String baseUrl() {
        return "/seller/products/" + PRODUCT_ID + "/images";
    }

    private String imageUrl() {
        return baseUrl() + "/" + IMAGE_ID;
    }

    private String primaryUrl() {
        return imageUrl() + "/primary";
    }

    private String reorderUrl() {
        return baseUrl() + "/reorder";
    }

    private RequestPostProcessor sellerAuth() {
        return JwtTestUtils.sellerAuth(jwtAuthenticationConverter);
    }

    private RequestPostProcessor customerAuth() {
        return JwtTestUtils.customerAuth(jwtAuthenticationConverter);
    }

    private ProductImageResponse productImageResponse(Boolean isPrimary, int sortOrder) {
        return new ProductImageResponse(
                IMAGE_ID,
                "https://cdn.example.com/images/iphone-16.jpg",
                isPrimary,
                sortOrder);
    }
}