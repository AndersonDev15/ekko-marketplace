package com.ekko.review_service.service;

import java.util.UUID;

public interface HelpfulVoteService {

    void addVote(UUID reviewId, String customerId);

    void removeVote(UUID reviewId, String customerId);
}