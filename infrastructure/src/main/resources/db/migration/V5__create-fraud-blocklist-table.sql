CREATE TABLE fraud_blocklist
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    blocked_value VARCHAR(255)             NOT NULL,
    reason        VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_fraud_blocklist_blocked_value UNIQUE (blocked_value)
);

CREATE INDEX idx_fraud_blocklist_expires_at
    ON fraud_blocklist (expires_at);