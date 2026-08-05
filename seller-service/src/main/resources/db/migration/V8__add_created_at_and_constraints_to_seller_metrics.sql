ALTER TABLE seller_metrics
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE seller_metrics
ALTER COLUMN total_sales SET NOT NULL;

ALTER TABLE seller_metrics
ALTER COLUMN total_revenue SET NOT NULL;

ALTER TABLE seller_metrics
ALTER COLUMN average_rating SET NOT NULL;

ALTER TABLE seller_metrics
ALTER COLUMN total_reviews SET NOT NULL;

ALTER TABLE seller_metrics
ALTER COLUMN active_products SET NOT NULL;