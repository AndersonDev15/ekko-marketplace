CREATE TYPE seller_status AS ENUM (
    'ACTIVE',
    'SUSPENDED',
    'PENDING_REVIEW'
);

CREATE TYPE document_type AS ENUM (
    'ID_CARD',
    'RUT',
    'BUSINESS_LICENSE',
    'BANK_CERTIFICATE'
);

CREATE TYPE document_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
);

CREATE TYPE bank_account_type AS ENUM (
    'SAVINGS',
    'CHECKING'
);