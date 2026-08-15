
-- Review Service - Initial Schema


-- ENUMS


CREATE TYPE review_status AS ENUM (
    'VISIBLE',
    'HIDDEN'
);


-- ELIGIBLE REVIEWS


CREATE TABLE eligible_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    product_id UUID NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_eligible_reviews_order_item_customer
        UNIQUE (order_item_id, customer_id)
);

CREATE INDEX idx_eligible_reviews_customer_id
    ON eligible_reviews(customer_id);

CREATE INDEX idx_eligible_reviews_product_id
    ON eligible_reviews(product_id);


-- REVIEWS


CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL,

    rating INT NOT NULL,

    title VARCHAR(255),
    comment TEXT,

    status review_status NOT NULL DEFAULT 'VISIBLE',

    is_verified_purchase BOOLEAN NOT NULL DEFAULT TRUE,

    reviewed_by VARCHAR(36),
    reviewed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_reviews_rating
        CHECK (rating >= 1 AND rating <= 5),

    CONSTRAINT uk_reviews_order_item_customer
        UNIQUE (order_item_id, customer_id)
);

CREATE INDEX idx_reviews_product_id
    ON reviews(product_id);

CREATE INDEX idx_reviews_customer_id
    ON reviews(customer_id);

CREATE INDEX idx_reviews_status
    ON reviews(status);


-- REVIEW IMAGES


CREATE TABLE review_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    review_id UUID NOT NULL,

    url VARCHAR(500) NOT NULL,

    sort_order INT DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_review_images_review
        FOREIGN KEY (review_id)
        REFERENCES reviews(id)
);


-- REVIEW HELPFUL VOTES


CREATE TABLE review_helpful_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    review_id UUID NOT NULL,
    customer_id VARCHAR(36) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_review_helpful_votes_review
        FOREIGN KEY (review_id)
        REFERENCES reviews(id),

    CONSTRAINT uk_review_helpful_votes_review_customer
        UNIQUE (review_id, customer_id)
);

CREATE INDEX idx_review_helpful_votes_review_id
    ON review_helpful_votes(review_id);