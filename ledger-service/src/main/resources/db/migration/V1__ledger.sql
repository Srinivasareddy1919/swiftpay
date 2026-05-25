CREATE TABLE IF NOT EXISTS accounts (
    user_id   VARCHAR(64) PRIMARY KEY,
    balance   NUMERIC(18,4) NOT NULL DEFAULT 10000 CHECK (balance >= 0),
    currency  VARCHAR(3) NOT NULL DEFAULT 'USD',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  UUID NOT NULL,
    user_id         VARCHAR(64) NOT NULL,
    direction       VARCHAR(8) NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    amount          NUMERIC(18,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(transaction_id, user_id, direction)
);

CREATE INDEX IF NOT EXISTS idx_ledger_user ON ledger_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_ledger_tx   ON ledger_entries(transaction_id);
