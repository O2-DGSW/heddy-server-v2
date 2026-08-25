ALTER TABLE users DROP CONSTRAINT ck_users_auth_identity;

ALTER TABLE users ADD CONSTRAINT ck_users_auth_identity CHECK (
    status IN ('DELETION_PENDING', 'DELETED')
    OR (auth_provider = 'EMAIL' AND email IS NOT NULL AND password_hash IS NOT NULL)
    OR (auth_provider <> 'EMAIL' AND provider_subject IS NOT NULL)
);

CREATE UNIQUE INDEX uk_account_deletion_requests_active_user
    ON account_deletion_requests(user_id)
    WHERE status IN ('REQUESTED', 'PROCESSING');

CREATE TABLE used_reauthentication_tokens (
    token_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_used_reauthentication_tokens_used_at
    ON used_reauthentication_tokens(used_at);
