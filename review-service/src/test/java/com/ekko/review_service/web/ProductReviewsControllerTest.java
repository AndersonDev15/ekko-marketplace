package com.ekko.review_service.web;

import com.ekko.review_service.config.SecurityConfig;
import com.ekko.review_service.controller.ProductReviewsController;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.response.ProductReviewsResponse;
import com.ekko.review_service.dto.response.ReviewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductReviewsController.class)
@Import(SecurityConfig.class)
class ProductReviewsControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewQueryService reviewQueryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET /product-reviews/{id}/reviews sin JWT -> 200 (endpoint público)")
    void getProductReviews_shouldReturn200WithoutJwt() throws Exception {
        // given
        ReviewResponse review = reviewResponse();
        ProductReviewsResponse response = new ProductReviewsResponse(
                new PageImpl<>(List.of(review)),
                new BigDecimal("4.5"),
                1L,
                Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 1L));
        when(reviewQueryService.getProductReviews(eq(PRODUCT_ID), any(Pageable.class))).thenReturn(response);

        // when
        // then
        mockMvc.perform(get("/product-reviews/{productId}/reviews", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.reviews.content[0].id").value(review.id().toString()));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewQueryService).getProductReviews(eq(PRODUCT_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET /product-reviews/{id}/reviews sin reviews -> 200 con promedio 0.0")
    void getProductReviews_shouldReturnZeroAverageWhenNoReviews() throws Exception {
        // given
        ProductReviewsResponse response = new ProductReviewsResponse(
                new PageImpl<>(List.of()),
                new BigDecimal("0.0"),
                0L,
                Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
        when(reviewQueryService.getProductReviews(eq(PRODUCT_ID), any(Pageable.class))).thenReturn(response);

        // when
        // then
        mockMvc.perform(get("/product-reviews/{productId}/reviews", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.totalReviews").value(0))
                .andExpect(jsonPath("$.reviews.empty").value(true));
    }

    @Test
    @DisplayName("GET /product-reviews/{id}/reviews respeta la paginación enviada")
    void getProductReviews_shouldPropagatePageableParameters() throws Exception {
        // given
        ProductReviewsResponse response = new ProductReviewsResponse(
                new PageImpl<>(List.of()),
                new BigDecimal("0.0"),
                0L,
                Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
        when(reviewQueryService.getProductReviews(eq(PRODUCT_ID), any(Pageable.class))).thenReturn(response);

        // when
        mockMvc.perform(get("/product-reviews/{productId}/reviews", PRODUCT_ID)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewQueryService).getProductReviews(eq(PRODUCT_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    private ReviewResponse reviewResponse() {
        return new ReviewResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                PRODUCT_ID,
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                5,
                "Great",
                "Nice",
                ReviewStatus.VISIBLE,
                true,
                List.of(),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                2L);
    }
}