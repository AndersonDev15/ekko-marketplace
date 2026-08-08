CREATE TYPE order_status AS ENUM (
    'PENDING',
    'PAID',
    'SHIPPED',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'COMPLETED',
    'FAILED'
);

CREATE TYPE changed_by_type AS ENUM (
    'SYSTEM',
    'CUSTOMER',
    'SELLER',
    'ADMIN'
);