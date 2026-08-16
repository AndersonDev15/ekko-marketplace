-- Add ON DELETE CASCADE to review_images FK so deleting a review
-- also cleans up its images at DB level (mirrors V2 for helpful votes).

ALTER TABLE review_images DROP CONSTRAINT fk_review_images_review;

ALTER TABLE review_images
    ADD CONSTRAINT fk_review_images_review
    FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE;