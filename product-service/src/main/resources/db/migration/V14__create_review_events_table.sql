CREATE TABLE review_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);