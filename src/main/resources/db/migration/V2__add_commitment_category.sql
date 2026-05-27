-- Adds category to distinguish commitments, events, and alerts.
-- Defaults to COMMITMENT so existing rows are unaffected.

ALTER TABLE commitments
    ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'COMMITMENT';

CREATE INDEX idx_commitments_user_category
    ON commitments(user_id, category);