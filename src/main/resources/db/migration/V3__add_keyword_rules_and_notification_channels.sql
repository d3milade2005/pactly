
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


CREATE TABLE keyword_rule_channels (
    keyword_rule_id         UUID NOT NULL REFERENCES keyword_rules(id) ON DELETE CASCADE,
    notification_channel_id UUID NOT NULL REFERENCES notification_channels(id) ON DELETE CASCADE,
    PRIMARY KEY (keyword_rule_id, notification_channel_id)
);


CREATE INDEX idx_keyword_rules_user_active
    ON keyword_rules(user_id)
    WHERE is_active = true;

CREATE INDEX idx_notification_channels_user_active
    ON notification_channels(user_id)
    WHERE is_active = true;