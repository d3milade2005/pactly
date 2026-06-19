-- Ensure pgcrypto is available for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Users
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) NOT NULL UNIQUE,
    display_name        VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- OAuth tokens
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

-- Contacts
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

-- Raw messages
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

-- Commitments (includes category column from previous V2)
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
     snoozed_until       TIMESTAMPTZ,
     category            VARCHAR(20) NOT NULL DEFAULT 'COMMITMENT'
);

-- Commitment participants
CREATE TABLE commitment_participants (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    commitment_id       UUID NOT NULL REFERENCES commitments(id) ON DELETE CASCADE,
    contact_id          UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    role                VARCHAR(20) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (commitment_id, contact_id, role)
);

-- Outbox events
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

-- Notification channels (from previous V3)
CREATE TABLE notification_channels (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel_type    VARCHAR(20) NOT NULL,   -- EMAIL, WHATSAPP, SMS, TELEGRAM, SLACK
    destination     TEXT NOT NULL,          -- phone number, email, webhook URL
    label           VARCHAR(100),           -- user-defined: "My WhatsApp", "Work Email"
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Keyword rules (from previous V3)
CREATE TABLE keyword_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,  -- "Job opportunities"
    keywords        TEXT[] NOT NULL,        -- ["interview", "offer letter"]
    match_all       BOOLEAN NOT NULL DEFAULT false, -- false = ANY keyword, true = ALL keywords
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Join table for keyword rules and channels
CREATE TABLE keyword_rule_channels (
    keyword_rule_id         UUID NOT NULL REFERENCES keyword_rules(id) ON DELETE CASCADE,
    notification_channel_id UUID NOT NULL REFERENCES notification_channels(id) ON DELETE CASCADE,
    PRIMARY KEY (keyword_rule_id, notification_channel_id)
);

-- Indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_raw_messages_fingerprint ON raw_messages(message_fingerprint);
CREATE INDEX IF NOT EXISTS idx_commitments_user_status ON commitments(user_id, status);
CREATE INDEX IF NOT EXISTS idx_commitments_due_date ON commitments(due_date) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events(created_at) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_contacts_user_email ON contacts(user_id, email);
CREATE INDEX IF NOT EXISTS idx_commitments_user_category ON commitments(user_id, category);
CREATE INDEX IF NOT EXISTS idx_keyword_rules_user_active ON keyword_rules(user_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_notification_channels_user_active ON notification_channels(user_id) WHERE is_active = true;
