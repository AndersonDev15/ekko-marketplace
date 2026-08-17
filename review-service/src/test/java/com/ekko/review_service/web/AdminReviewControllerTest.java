package com.ekko.review_service.web;

import com.ekko.review_service.config.SecurityConfig;
import com.ekko.review_service.controller.AdminReviewController;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.service.ReviewCommandService;
import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.request.AdminUpdateContentRequest;
import com.ekko.review_service.dto.response.ReviewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ekko.review_service.util.JwtTestUtils.adminAuth;
import static com.ekko.review_service.util.JwtTestUtils.customerAuth;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReviewController.class)
@Import(SecurityConfig.class)
class AdminReviewControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REVIEW_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewCommandService reviewCommandService;

    @MockitoBean
    private ReviewQueryService reviewQueryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ---------- getAllReviews ----------

    @Test
    @DisplayName("GET /admin/reviews sin JWT -> 401")
    void getAllReviews_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/admin/reviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/reviews con customer -> 403")
    void getAllReviews_shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(get("/admin/reviews").with(customerAuth(CUSTOMER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/reviews con admin -> 200 y delega los filtros")
    void getAllReviews_shouldReturn200WithFilters() throws Exception {
        // given
        Page<ReviewResponse> page = new PageImpl<>(List.of(reviewResponse()));
        when(reviewQueryService.adminGetAllReviews(
                eq(ReviewStatus.HIDDEN), eq(PRODUCT_ID), eq("customer-1"), any()))
                .thenReturn(page);

        // when
        // then
        mockMvc.perform(get("/admin/reviews")
                        .with(adminAuth(ADMIN_ID))
                        .param("status", "HIDDEN")
                        .param("productId", PRODUCT_ID.toString())
                        .param("customerId", "customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(REVIEW_ID.toString()));

        verify(reviewQueryService).adminGetAllReviews(
                eq(ReviewStatus.HIDDEN), eq(PRODUCT_ID), eq("customer-1"), any());
    }

    // ---------- updateReviewStatus ----------

    @Test
    @DisplayName("PATCH /admin/reviews/{id}/status con admin -> 200 y delega con el subject del JWT")
    void updateReviewStatus_shouldReturn200AndDelegateWithJwtSubject() throws Exception {
        // given
        when(reviewCommandService.adminUpdateStatus(eq(REVIEW_ID), eq(ReviewStatus.HIDDEN), eq(ADMIN_ID.toString())))
                .thenReturn(reviewResponse());

        // when
        // then
        mockMvc.perform(patch("/admin/reviews/{reviewId}/status", REVIEW_ID)
                        .with(adminAuth(ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus": "HIDDEN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REVIEW_ID.toString()));

        verify(reviewCommandService).adminUpdateStatus(eq(REVIEW_ID), eq(ReviewStatus.HIDDEN), eq(ADMIN_ID.toString()));
    }

    @Test
    @DisplayName("PATCH /admin/reviews/{id}/status sin newStatus -> 400 VALIDATION_ERROR")
    void updateReviewStatus_shouldReturn400WhenStatusMissing() throws Exception {
        mockMvc.perform(patch("/admin/reviews/{reviewId}/status", REVIEW_ID)
                        .with(adminAuth(ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH /admin/reviews/{id}/status con customer -> 403")
    void updateReviewStatus_shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(patch("/admin/reviews/{reviewId}/status", REVIEW_ID)
                        .with(customerAuth(CUSTOMER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus": "HIDDEN"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ---------- updateReviewContent ----------

    @Test
    @DisplayName("PATCH /admin/reviews/{id}/content con admin -> 200")
    void updateReviewContent_shouldReturn200() throws Exception {
        // given
        when(reviewCommandService.adminUpdateContent(eq(REVIEW_ID), any(AdminUpdateContentRequest.class), eq(ADMIN_ID.toString())))
                .thenReturn(reviewResponse());

        // when
        // then
        mockMvc.perform(patch("/admin/reviews/{reviewId}/content", REVIEW_ID)
                        .with(adminAuth(ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 1, "title": "Fixed", "comment": "Corrected"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REVIEW_ID.toString()));
    }

    @Test
    @DisplayName("PATCH /admin/reviews/{id}/content con rating inválido -> 400 VALIDATION_ERROR")
    void updateReviewContent_shouldReturn400WhenRatingInvalid() throws Exception {
        mockMvc.perform(patch("/admin/reviews/{reviewId}/content", REVIEW_ID)
                        .with(adminAuth(ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 0, "title": "Fixed", "comment": "Corrected"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // ---------- deleteReview ----------

    @Test
    @DisplayName("DELETE /admin/reviews/{id} con admin -> 204")
    void deleteReview_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/admin/reviews/{reviewId}", REVIEW_ID)
                        .with(adminAuth(ADMIN_ID)))
                .andExpect(status().isNoContent());

        verify(reviewCommandService).adminDeleteReview(REVIEW_ID);
    }

    private ReviewResponse reviewResponse() {
        return new ReviewResponse(
                REVIEW_ID,
                PRODUCT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
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