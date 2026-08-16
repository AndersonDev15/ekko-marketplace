package com.ekko.review_service.service;

import com.ekko.review_service.config.AbstractPostgresIntegrationTest;
import com.ekko.review_service.exception.HelpfulVoteAlreadyExistsException;
import com.ekko.review_service.exception.HelpfulVoteNotFoundException;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.exception.SelfHelpfulVoteException;
import com.ekko.review_service.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static com.ekko.review_service.support.ReviewTestDataBuilder.aReview;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelpfulVoteServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String ANOTHER_CUSTOMER_ID = "customer-2";

    @Autowired
    private HelpfulVoteService helpfulVoteService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("addVote persiste el voto en la BD")
    void addVote_shouldPersistVote() {
        // given
        UUID reviewId = reviewRepository.save(aReview().withCustomerId(ANOTHER_CUSTOMER_ID).build()).getId();

        // when
        helpfulVoteService.addVote(reviewId, CUSTOMER_ID);

        // then
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_helpful_votes WHERE review_id = ? AND customer_id = ?",
                Long.class, reviewId, CUSTOMER_ID);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("addVote doble lanza HelpfulVoteAlreadyExistsException por la constraint única real de la BD")
    void addVote_shouldThrowAlreadyExistsOnSecondVoteFromSameCustomer() {
        // given
        UUID reviewId = reviewRepository.save(aReview().withCustomerId(ANOTHER_CUSTOMER_ID).build()).getId();
        helpfulVoteService.addVote(reviewId, CUSTOMER_ID);

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.addVote(reviewId, CUSTOMER_ID))
                .isInstanceOf(HelpfulVoteAlreadyExistsException.class);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_helpful_votes WHERE review_id = ?", Long.class, reviewId);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("addVote lanza SelfHelpfulVoteException cuando el customer es el autor")
    void addVote_shouldThrowSelfVoteWhenCustomerIsAuthor() {
        // given
        UUID reviewId = reviewRepository.save(aReview().withCustomerId(CUSTOMER_ID).build()).getId();

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.addVote(reviewId, CUSTOMER_ID))
                .isInstanceOf(SelfHelpfulVoteException.class);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_helpful_votes WHERE review_id = ?", Long.class, reviewId);
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("addVote trata una review HIDDEN como inexistente")
    void addVote_shouldThrowReviewNotFoundForHiddenReview() {
        // given
        UUID reviewId = reviewRepository.save(aReview().withCustomerId(ANOTHER_CUSTOMER_ID).hidden().build()).getId();

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.addVote(reviewId, CUSTOMER_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("addVote lanza ReviewNotFoundException cuando la review no existe")
    void addVote_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.addVote(UUID.randomUUID(), CUSTOMER_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("removeVote elimina el voto existente")
    void removeVote_shouldDeleteExistingVote() {
        // given
        UUID reviewId = reviewRepository.save(aReview().withCustomerId(ANOTHER_CUSTOMER_ID).build()).getId();
        helpfulVoteService.addVote(reviewId, CUSTOMER_ID);

        // when
        helpfulVoteService.removeVote(reviewId, CUSTOMER_ID);

        // then
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_helpful_votes WHERE review_id = ?", Long.class, reviewId);
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("removeVote lanza HelpfulVoteNotFoundException cuando el customer no votó")
    void removeVote_shouldThrowNotFoundWhenCustomerDidNotVote() {
        // given
        UUID reviewId = reviewRepository.save(aReview().withCustomerId(ANOTHER_CUSTOMER_ID).build()).getId();

        // when
        // then
        assertThatThrownBy(() -> helpfulVoteService.removeVote(reviewId, CUSTOMER_ID))
                .isInstanceOf(HelpfulVoteNotFoundException.class);
    }

    @Test
    @DisplayName("la constraint única review_id+customer_id existe en la BD")
    void database_shouldEnforceUniqueReviewCustomerVoteConstraint() {
        // given
        UUID reviewId = reviewRepository.save(aReview().withCustomerId(ANOTHER_CUSTOMER_ID).build()).getId();
        jdbcTemplate.update("INSERT INTO review_helpful_votes (review_id, customer_id) VALUES (?, ?)",
                reviewId, CUSTOMER_ID);

        // when
        // then
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO review_helpful_votes (review_id, customer_id) VALUES (?, ?)",
                reviewId, CUSTOMER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}