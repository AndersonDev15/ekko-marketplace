CREATE TABLE seller_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL UNIQUE REFERENCES sellers(id) ON DELETE CASCADE,
    total_sales BIGINT DEFAULT 0,
    total_revenue DECIMAL(10,2) DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0,
    total_reviews BIGINT DEFAULT 0,
    active_products BIGINT DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);