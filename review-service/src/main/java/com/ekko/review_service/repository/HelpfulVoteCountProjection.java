package com.ekko.review_service.repository;

import java.util.UUID;

public interface HelpfulVoteCountProjection {

    UUID getReviewId();

    long getCount();
}