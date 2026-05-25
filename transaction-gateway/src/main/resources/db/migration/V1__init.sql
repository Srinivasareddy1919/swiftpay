CREATE TABLE IF NOT EXISTS payments (
    transaction_id  UUID PRIMARY KEY,
    sender_id       VARCHAR(64)  NOT NULL,
    receiver_id     VARCHAR(64)  NOT NULL,
    amount          NUMERIC(18,4) NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)   NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payments_sender   ON payments(sender_id);
CREATE INDEX IF NOT EXISTS idx_payments_receiver ON payments(receiver_id);
CREATE INDEX IF NOT EXISTS idx_payments_status   ON payments(status);
