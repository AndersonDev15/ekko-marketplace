CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'SUCCEEDED',
    'FAILED',
    'CANCELLED',
    'REFUNDED',
    'DISPUTED'
);

CREATE TYPE transaction_type AS ENUM (
    'CHARGE',
    'CAPTURE',
    'REFUND',
    'VOID',
    'DISPUTE'
);

CREATE TYPE transaction_status AS ENUM (
    'PENDING',
    'SUCCEEDED',
    'FAILED'
);

CREATE TYPE refund_status AS ENUM (
    'PENDING',
    'SUCCEEDED',
    'FAILED',
    'CANCELLED'
);

CREATE TYPE vendor_account_status AS ENUM (
    'PENDING',
    'ACTIVE',
    'RESTRICTED',
    'DISABLED'
);

CREATE TYPE transfer_status AS ENUM (
    'PENDING',
    'SUCCEEDED',
    'FAILED'
);

CREATE TYPE payout_status AS ENUM (
    'PENDING',
    'IN_TRANSIT',
    'PAID',
    'FAILED',
    'CANCELLED'
);