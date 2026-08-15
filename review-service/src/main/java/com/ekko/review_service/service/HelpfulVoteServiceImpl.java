package com.ekko.review_service.service;

import com.ekko.review_service.entity.Review;
import com.ekko.review_service.entity.ReviewHelpfulVote;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.exception.HelpfulVoteAlreadyExistsException;
import com.ekko.review_service.exception.HelpfulVoteNotFoundException;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.exception.SelfHelpfulVoteException;
import com.ekko.review_service.repository.ReviewHelpfulVoteRepository;
import com.ekko.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HelpfulVoteServiceImpl implements HelpfulVoteService {

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository reviewHelpfulVoteRepository;

    @Override
    @Transactional
    public void addVote(UUID reviewId, String customerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        // A hidden review must not be distinguishable from a non-existent one for voters.
        if (review.getStatus() == ReviewStatus.HIDDEN) {
            throw new ReviewNotFoundException();
        }

        if (review.getCustomerId().equals(customerId)) {
            throw new SelfHelpfulVoteException();
        }

        try {
            reviewHelpfulVoteRepository.saveAndFlush(ReviewHelpfulVote.builder()
                    .review(review)
                    .customerId(customerId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new HelpfulVoteAlreadyExistsException();
        }
    }

    @Override
    @Transactional
    public void removeVote(UUID reviewId, String customerId) {
        ReviewHelpfulVote vote = reviewHelpfulVoteRepository.findByReviewIdAndCustomerId(reviewId, customerId)
                .orElseThrow(HelpfulVoteNotFoundException::new);

        reviewHelpfulVoteRepository.delete(vote);
    }
}