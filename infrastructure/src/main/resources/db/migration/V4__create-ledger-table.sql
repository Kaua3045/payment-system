CREATE TABLE ledger_reservations
(
    id             VARCHAR(26)              NOT NULL PRIMARY KEY,
    transaction_id VARCHAR(26)              NOT NULL,
    account_id     VARCHAR(26)              NOT NULL,
    amount         NUMERIC(19, 4)           NOT NULL,
    status         VARCHAR(20)              NOT NULL, -- PENDING, CONFIRMED, CANCELLED
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ledger_res_account_id FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE TABLE ledger_entries
(
    id             VARCHAR(26)              NOT NULL PRIMARY KEY,
    account_id     VARCHAR(26)              NOT NULL,
    transaction_id VARCHAR(26)              NOT NULL,
    amount         NUMERIC(19, 4)           NOT NULL,
    type           VARCHAR(10)              NOT NULL, -- DEBIT, CREDIT
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ledger_account_id FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_ledger_transaction_id FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);

CREATE TABLE ledger_snapshots
(
    account_id       VARCHAR(26)              NOT NULL PRIMARY KEY,
    last_ledger_id   VARCHAR(26)              NOT NULL,
    balance          NUMERIC(19, 4)           NOT NULL,
    reserved_balance NUMERIC(19, 4)           NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ledger_snapshots_account_id FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_ledger_entries_account_id_id ON ledger_entries (account_id, id);

CREATE INDEX idx_ledger_res_account_status_expires ON ledger_reservations (account_id, status, expires_at);

CREATE UNIQUE INDEX uq_ledger_res_transaction_id ON ledger_reservations (transaction_id);