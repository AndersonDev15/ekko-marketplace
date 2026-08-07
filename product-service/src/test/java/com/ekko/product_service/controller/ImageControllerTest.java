package com.ekko.product_service.controller;

import com.ekko.product_service.config.SecurityConfig;
import com.ekko.product_service.dto.request.CreateImageRequest;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.exception.ForbiddenProductAccessException;
import com.ekko.product_service.exception.ImageNotFoundException;
import com.ekko.product_service.exception.InvalidImageOrderException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.service.ImageService;
import com.ekko.product_service.util.JwtTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static com.ekko.product_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ------------------------------------------------------ POST /seller/products/{id}/images

    @Test
    void postImage_devuelve201YProductImageResponse() throws Exception {
        ProductImageResponse response = productImageResponse(true, 0);
        when(imageService.addImage(eq(PRODUCT_ID), any(CreateImageRequest.class), eq(SELLER_ID)))
                .thenReturn(response);

        mockMvc.perform(post(baseUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://cdn.example.com/images/iphone-16.jpg",
                                  "isPrimary": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(IMAGE_ID.toString()))
                .andExpect(jsonPath("$.url").value("https://cdn.example.com/images/iphone-16.jpg"))
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andExpect(jsonPath("$.sortOrder").value(0));

        verify(imageService).addImage(eq(PRODUCT_ID), any(CreateImageRequest.class), eq(SELLER_ID));
    }

    @Test
    void postImage_devuelve401SinJWT() throws Exception {
        mockMvc.perform(post(baseUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://cdn.example.com/images/iphone-16.jpg",
                                  "isPrimary": true
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postImage_devuelve403ConRolIncorrecto() throws Exception {
        mockMvc.perform(post(baseUrl())
                        .with(customerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://cdn.example.com/images/iphone-16.jpg",
                                  "isPrimary": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void postImage_imageServiceLanzaProductNotFoundExceptionDevuelve404() throws Exception {
        when(imageService.addImage(eq(PRODUCT_ID), any(CreateImageRequest.class), eq(SELLER_ID)))
                .thenThrow(ProductNotFoundException.class);

        mockMvc.perform(post(baseUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://cdn.example.com/images/iphone-16.jpg",
                                  "isPrimary": true
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void postImage_imageServiceLanzaForbiddenProductAccessExceptionDevuelve403() throws Exception {
        when(imageService.addImage(eq(PRODUCT_ID), any(CreateImageRequest.class), eq(SELLER_ID)))
                .thenThrow(ForbiddenProductAccessException.class);

        mockMvc.perform(post(baseUrl())
                        .with(sellerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://cdn.example.com/images/iphone-16.jpg",
                                  "isPrimary": true
                                }
                                """))
                .andExpect(status().isForbidden());
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
        return authentication(new JwtAuthenticationToken(JwtTestUtils.sellerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_SELLER"))));
    }

    private RequestPostProcessor customerAuth() {
        return authentication(new JwtAuthenticationToken(JwtTestUtils.customerJwt(),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private ProductImageResponse productImageResponse(Boolean isPrimary, int sortOrder) {
        return new ProductImageResponse(
                IMAGE_ID,
                "https://cdn.example.com/images/iphone-16.jpg",
                isPrimary,
                sortOrder);
    }
}