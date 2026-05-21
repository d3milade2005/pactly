CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) NOT NULL UNIQUE,
    display_name        VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE oauth_tokens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider            VARCHAR(50) NOT NULL,           -- GMAIL, CALENDAR
    access_token        TEXT NOT NULL,
    refresh_token       TEXT NOT NULL,
    token_expiry        TIMESTAMPTZ NOT NULL,
    scopes              TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, provider)
);


CREATE TABLE contacts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email               VARCHAR(255) NOT NULL,
    display_name        VARCHAR(255),
    relationship_score  NUMERIC(3,2) NOT NULL DEFAULT 0.50, -- 0.00 to 1.00
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, email)
);

CREATE TABLE raw_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source              VARCHAR(50) NOT NULL,           -- GMAIL
    external_id         VARCHAR(255) NOT NULL,          -- Gmail message ID
    message_fingerprint VARCHAR(64) NOT NULL UNIQUE,    -- SHA-256(source + external_id + user_id)
    subject             TEXT,
    sender_email        VARCHAR(255) NOT NULL,
    recipient_emails    TEXT[],
    body_snippet        TEXT,
    received_at         TIMESTAMPTZ NOT NULL,
    processing_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PROCESSED, FAILED, SKIPPED
    processed_at        TIMESTAMPTZ,
    failure_reason      TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE commitments (
     id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     source_message_id   UUID REFERENCES raw_messages(id) ON DELETE SET NULL,
     description         TEXT NOT NULL,
     direction           VARCHAR(20) NOT NULL,
     status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
     due_date            TIMESTAMPTZ,
     due_date_confidence VARCHAR(20),
     confidence_score    NUMERIC(3,2) NOT NULL,          -- 0.00 to 1.00
     extraction_source   VARCHAR(20) NOT NULL DEFAULT 'LLM', -- LLM, RULE, MANUAL
     raw_trigger_text    TEXT,                           -- exact sentence that triggered extraction
     urgency_score       NUMERIC(3,2),
     priority_score      NUMERIC(3,2),
     created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
     updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
     fulfilled_at        TIMESTAMPTZ,
     snoozed_until       TIMESTAMPTZ
);


CREATE TABLE commitment_participants (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    commitment_id       UUID NOT NULL REFERENCES commitments(id) ON DELETE CASCADE,
    contact_id          UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    role                VARCHAR(20) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (commitment_id, contact_id, role)
);


CREATE TABLE outbox_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type      VARCHAR(100) NOT NULL,          -- Commitment, RawMessage
    aggregate_id        UUID NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    payload             JSONB NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, DISPATCHED, FAILED
    retry_count         INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    dispatched_at       TIMESTAMPTZ
);


-- Idempotency check on every Gmail poll
CREATE UNIQUE INDEX idx_raw_messages_fingerprint
    ON raw_messages(message_fingerprint);

-- Dashboard query: user's commitments by status
CREATE INDEX idx_commitments_user_status
    ON commitments(user_id, status);

-- Nudge scheduler: find overdue/upcoming commitments
CREATE INDEX idx_commitments_due_date
    ON commitments(due_date)
    WHERE status = 'PENDING';

-- Outbox poller: find unprocessed events
CREATE INDEX idx_outbox_pending
    ON outbox_events(created_at)
    WHERE status = 'PENDING';

-- Contact lookup during extraction
CREATE INDEX idx_contacts_user_email
    ON contacts(user_id, email);