CREATE TABLE transactions (
    id VARCHAR(26) NOT NULL PRIMARY KEY,
    from_account_id VARCHAR(26) NOT NULL,
    to_account_id VARCHAR(26) NOT NULL,
    pix_key_id VARCHAR(26) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(15) NOT NULL,
    type VARCHAR(15) NOT NULL,
    source VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    failure_reason VARCHAR(255) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);