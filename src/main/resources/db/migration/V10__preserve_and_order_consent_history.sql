DROP INDEX idx_consent_history_user_type_latest;

ALTER TABLE consent_history
    ADD COLUMN change_sequence BIGINT GENERATED ALWAYS AS IDENTITY;

ALTER TABLE consent_history
    DROP CONSTRAINT consent_history_user_id_fkey;

ALTER TABLE consent_history
    ADD CONSTRAINT fk_consent_history_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT;

CREATE INDEX idx_consent_history_user_type_latest
    ON consent_history(user_id, consent_type, change_sequence DESC);
