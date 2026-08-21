package com.ekko.review_service.web;

import com.ekko.review_service.config.SecurityConfig;
import com.ekko.review_service.controller.ReviewController;
import com.ekko.review_service.dto.request.CreateReviewRequest;
import com.ekko.review_service.exception.HelpfulVoteAlreadyExistsException;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.exception.SelfHelpfulVoteException;
import com.ekko.review_service.service.HelpfulVoteService;
import com.ekko.review_service.service.ReviewCommandService;
import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.response.EligibleToReviewResponse;
import com.ekko.review_service.dto.response.ReviewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ekko.review_service.util.JwtTestUtils.customerAuth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REVIEW_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ORDER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ORDER_ITEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private ReviewCommandService reviewCommandService;

    @MockitoBean
    private ReviewQueryService reviewQueryService;

    @MockitoBean
    private HelpfulVoteService helpfulVoteService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ---------- createReview ----------

    @Test
    @DisplayName("POST /reviews con customer autenticado -> 201 y delega con el subject del JWT")
    void createReview_shouldReturn201AndDelegateWithJwtSubject() throws Exception {
        // given
        ReviewResponse response = reviewResponse();
        when(reviewCommandService.createReview(any(), eq(CUSTOMER_ID.toString()))).thenReturn(response);

        // when
        // then
        mockMvc.perform(post("/reviews")
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "orderId": "%s",
                                  "orderItemId": "%s",
                                  "rating": 5,
                                  "title": "Great",
                                  "comment": "Nice",
                                  "imageUrls": ["https://img/1"]
                                }
                                """.formatted(PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(REVIEW_ID.toString()));

        ArgumentCaptor<CreateReviewRequest> captor =
                ArgumentCaptor.forClass(CreateReviewRequest.class);
        verify(reviewCommandService).createReview(captor.capture(), eq(CUSTOMER_ID.toString()));
        assertThat(captor.getValue().rating()).isEqualTo(5);
    }

    @Test
    @DisplayName("POST /reviews sin JWT -> 401")
    void createReview_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "orderId": "%s",
                                  "orderItemId": "%s",
                                  "rating": 5
                                }
                                """.formatted(PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /reviews con rating fuera de rango -> 400 VALIDATION_ERROR")
    void createReview_shouldReturn400WhenRatingOutOfRange() throws Exception {
        mockMvc.perform(post("/reviews")
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "orderId": "%s",
                                  "orderItemId": "%s",
                                  "rating": 6
                                }
                                """.formatted(PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /reviews sin productId -> 400 VALIDATION_ERROR")
    void createReview_shouldReturn400WhenProductIdMissing() throws Exception {
        mockMvc.perform(post("/reviews")
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "orderItemId": "%s",
                                  "rating": 5
                                }
                                """.formatted(ORDER_ID, ORDER_ITEM_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /reviews propaga la excepción del servicio como 403 NOT_ELIGIBLE_TO_REVIEW")
    void createReview_shouldMapServiceExceptionTo403() throws Exception {
        // given
        when(reviewCommandService.createReview(any(), any()))
                .thenThrow(new com.ekko.review_service.exception.NotEligibleToReviewException());

        // when
        // then
        mockMvc.perform(post("/reviews")
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "orderId": "%s",
                                  "orderItemId": "%s",
                                  "rating": 5
                                }
                                """.formatted(PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOT_ELIGIBLE_TO_REVIEW"));
    }

    // ---------- updateReview ----------

    @Test
    @DisplayName("PATCH /reviews/{id} con customer autenticado -> 200")
    void updateReview_shouldReturn200() throws Exception {
        // given
        when(reviewCommandService.updateReview(eq(REVIEW_ID), any(), eq(CUSTOMER_ID.toString())))
                .thenReturn(reviewResponse());

        // when
        // then
        mockMvc.perform(patch("/reviews/{reviewId}", REVIEW_ID)
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 4, "title": "Updated", "comment": "Still fine"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REVIEW_ID.toString()));
    }

    @Test
    @DisplayName("PATCH /reviews/{id} propaga ownership violation como 403")
    void updateReview_shouldMapOwnershipViolationTo403() throws Exception {
        // given
        when(reviewCommandService.updateReview(eq(REVIEW_ID), any(), any()))
                .thenThrow(new com.ekko.review_service.exception.ReviewOwnershipException());

        // when
        // then
        mockMvc.perform(patch("/reviews/{reviewId}", REVIEW_ID)
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 4}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("REVIEW_OWNERSHIP_VIOLATION"));
    }

    // ---------- deleteReview ----------

    @Test
    @DisplayName("DELETE /reviews/{id} -> 204 y delega con el subject del JWT")
    void deleteReview_shouldReturn204() throws Exception {
        // when
        mockMvc.perform(delete("/reviews/{reviewId}", REVIEW_ID)
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID)))
                .andExpect(status().isNoContent());

        // then
        verify(reviewCommandService).deleteReview(REVIEW_ID, CUSTOMER_ID.toString());
    }

    // ---------- getReviewById (público) ----------

    @Test
    @DisplayName("GET /reviews/{id} sin JWT -> 200 (endpoint público)")
    void getReviewById_shouldReturn200WithoutJwt() throws Exception {
        // given
        when(reviewQueryService.getReviewById(REVIEW_ID)).thenReturn(reviewResponse());

        // when
        // then
        mockMvc.perform(get("/reviews/{reviewId}", REVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REVIEW_ID.toString()));
    }

    @Test
    @DisplayName("GET /reviews/{id} de una review inexistente -> 404 REVIEW_NOT_FOUND")
    void getReviewById_shouldMapNotFoundTo404() throws Exception {
        // given
        when(reviewQueryService.getReviewById(REVIEW_ID)).thenThrow(new ReviewNotFoundException());

        // when
        // then
        mockMvc.perform(get("/reviews/{reviewId}", REVIEW_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REVIEW_NOT_FOUND"));
    }

    // ---------- getMyReviews ----------

    @Test
    @DisplayName("GET /reviews/me sin JWT -> 401")
    void getMyReviews_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/reviews/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /reviews/me con customer -> 200 con sus reviews")
    void getMyReviews_shouldReturn200WithCustomerReviews() throws Exception {
        // given
        Page<ReviewResponse> page = new PageImpl<>(List.of(reviewResponse()));
        when(reviewQueryService.getMyReviews(eq(CUSTOMER_ID.toString()), any())).thenReturn(page);

        // when
        // then
        mockMvc.perform(get("/reviews/me").with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ---------- getEligibleToReview ----------

    @Test
    @DisplayName("GET /reviews/eligible con customer -> 200 con la lista de elegibles")
    void getEligibleToReview_shouldReturn200WithEligibleItems() throws Exception {
        // given
        when(reviewQueryService.getEligibleToReview(CUSTOMER_ID.toString()))
                .thenReturn(List.of(new EligibleToReviewResponse(ORDER_ID, ORDER_ITEM_ID, PRODUCT_ID)));

        // when
        // then
        mockMvc.perform(get("/reviews/eligible").with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderItemId").value(ORDER_ITEM_ID.toString()));
    }

    // ---------- helpful votes ----------

    @Test
    @DisplayName("POST /reviews/{id}/helpful-votes -> 204")
    void addHelpfulVote_shouldReturn204() throws Exception {
        mockMvc.perform(post("/reviews/{reviewId}/helpful-votes", REVIEW_ID)
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID)))
                .andExpect(status().isNoContent());

        verify(helpfulVoteService).addVote(REVIEW_ID, CUSTOMER_ID.toString());
    }

    @Test
    @DisplayName("POST /reviews/{id}/helpful-votes propio -> 403 SELF_HELPFUL_VOTE_NOT_ALLOWED")
    void addHelpfulVote_shouldMapSelfVoteTo403() throws Exception {
        // given
        org.mockito.Mockito.doThrow(new SelfHelpfulVoteException())
                .when(helpfulVoteService).addVote(REVIEW_ID, CUSTOMER_ID.toString());

        // when
        // then
        mockMvc.perform(post("/reviews/{reviewId}/helpful-votes", REVIEW_ID)
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("SELF_HELPFUL_VOTE_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("POST /reviews/{id}/helpful-votes duplicado -> 409 HELPFUL_VOTE_ALREADY_EXISTS")
    void addHelpfulVote_shouldMapAlreadyExistsTo409() throws Exception {
        // given
        org.mockito.Mockito.doThrow(new HelpfulVoteAlreadyExistsException())
                .when(helpfulVoteService).addVote(REVIEW_ID, CUSTOMER_ID.toString());

        // when
        // then
        mockMvc.perform(post("/reviews/{reviewId}/helpful-votes", REVIEW_ID)
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("HELPFUL_VOTE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("DELETE /reviews/{id}/helpful-votes -> 204")
    void removeHelpfulVote_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/reviews/{reviewId}/helpful-votes", REVIEW_ID)
                        .with(customerAuth(jwtAuthenticationConverter, CUSTOMER_ID)))
                .andExpect(status().isNoContent());

        verify(helpfulVoteService).removeVote(REVIEW_ID, CUSTOMER_ID.toString());
    }

    private ReviewResponse reviewResponse() {
        return new ReviewResponse(
                REVIEW_ID,
                PRODUCT_ID,
                ORDER_ID,
                ORDER_ITEM_ID,
                5,
                "Great",
                "Nice",
                com.ekko.review_service.enums.ReviewStatus.VISIBLE,
                true,
                List.of("https://img/1"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                2L);
    }
}