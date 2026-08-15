-- Add ON DELETE CASCADE to review_helpful_votes FK so deleting a review
-- automatically cleans up its votes at DB level.

ALTER TABLE review_helpful_votes DROP CONSTRAINT fk_review_helpful_votes_review;

ALTER TABLE review_helpful_votes
    ADD CONSTRAINT fk_review_helpful_votes_review
    FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE;