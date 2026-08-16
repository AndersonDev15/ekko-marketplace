package com.ekko.review_service.service;

import com.ekko.review_service.entity.Review;
import com.ekko.review_service.entity.ReviewHelpfulVote;
import com.ekko.review_service.exception.HelpfulVoteAlreadyExistsException;
import com.ekko.review_service.exception.HelpfulVoteNotFoundException;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.exception.SelfHelpfulVoteException;
import com.ekko.review_service.repository.ReviewHelpfulVoteRepository;
import com.ekko.review_service.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static com.ekko.review_service.support.ReviewTestDataBuilder.aReview;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpfulVoteServiceImplTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final UUID REVIEW_ID = UUID.randomUUID();

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewHelpfulVoteRepository reviewHelpfulVoteRepository;

    @InjectMocks
    private HelpfulVoteServiceImpl helpfulVoteService;

    // ---------- addVote ----------

    @Test
    @DisplayName("addVote persiste el voto cuando la review existe, está visible y no es propia")
    void addVote_shouldPersistVoteWhenReviewVisibleAndNotOwn() {
        // given
        Review review = aReview().withId(REVIEW_ID).withCustomerId("customer-2").build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        helpfulVoteService.addVote(REVIEW_ID, CUSTOMER_ID);

        // then
        verify(reviewHelpfulVoteRepository).saveAndFlush(org.mockito.ArgumentMatchers.any(ReviewHelpfulVote.class));
    }

    @Test
    @DisplayName("addVote lanza ReviewNotFoundException cuando la review no existe")
    void addVote_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // given
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.addVote(REVIEW_ID, CUSTOMER_ID))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(reviewHelpfulVoteRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(ReviewHelpfulVote.class));
    }

    @Test
    @DisplayName("addVote trata una review HIDDEN como inexistente (no es distinguible para el votante)")
    void addVote_shouldThrowReviewNotFoundWhenReviewIsHidden() {
        // given
        Review review = aReview().withId(REVIEW_ID).hidden().build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.addVote(REVIEW_ID, CUSTOMER_ID))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(reviewHelpfulVoteRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(ReviewHelpfulVote.class));
    }

    @Test
    @DisplayName("addVote lanza SelfHelpfulVoteException cuando el customer es el autor de la review")
    void addVote_shouldThrowSelfHelpfulVoteWhenCustomerIsAuthor() {
        // given
        Review review = aReview().withId(REVIEW_ID).withCustomerId(CUSTOMER_ID).build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.addVote(REVIEW_ID, CUSTOMER_ID))
                .isInstanceOf(SelfHelpfulVoteException.class);
        verify(reviewHelpfulVoteRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(ReviewHelpfulVote.class));
    }

    @Test
    @DisplayName("addVote convierte una violación de unicidad de BD en HelpfulVoteAlreadyExistsException")
    void addVote_shouldTranslateDataIntegrityViolationToAlreadyExists() {
        // given
        Review review = aReview().withId(REVIEW_ID).withCustomerId("customer-2").build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(reviewHelpfulVoteRepository)
                .saveAndFlush(org.mockito.ArgumentMatchers.any(ReviewHelpfulVote.class));

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.addVote(REVIEW_ID, CUSTOMER_ID))
                .isInstanceOf(HelpfulVoteAlreadyExistsException.class);
    }

    // ---------- removeVote ----------

    @Test
    @DisplayName("removeVote elimina el voto existente del customer")
    void removeVote_shouldDeleteExistingVote() {
        // given
        ReviewHelpfulVote vote = ReviewHelpfulVote.builder().id(UUID.randomUUID()).build();
        when(reviewHelpfulVoteRepository.findByReviewIdAndCustomerId(REVIEW_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(vote));

        // when
        helpfulVoteService.removeVote(REVIEW_ID, CUSTOMER_ID);

        // then
        verify(reviewHelpfulVoteRepository).delete(vote);
    }

    @Test
    @DisplayName("removeVote lanza HelpfulVoteNotFoundException cuando el customer no votó esa review")
    void removeVote_shouldThrowHelpfulVoteNotFoundWhenCustomerDidNotVote() {
        // given
        when(reviewHelpfulVoteRepository.findByReviewIdAndCustomerId(REVIEW_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.removeVote(REVIEW_ID, CUSTOMER_ID))
                .isInstanceOf(HelpfulVoteNotFoundException.class);
    }
}