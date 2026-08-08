CREATE INDEX idx_orders_customer_id
ON orders(customer_id);

CREATE INDEX idx_order_items_order_id
ON order_items(order_id);

CREATE INDEX idx_order_status_history_order_id
ON order_status_history(order_id);

CREATE INDEX idx_order_payments_order_id
ON order_payments(order_id);

CREATE UNIQUE INDEX ux_order_payments_current
ON order_payments(order_id)
WHERE is_current = TRUE;