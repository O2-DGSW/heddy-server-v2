CREATE INDEX idx_consent_history_user_type_latest
    ON consent_history(user_id, consent_type, changed_at DESC, consent_id DESC);
