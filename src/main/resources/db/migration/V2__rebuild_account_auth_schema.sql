ALTER TABLE social_accounts RENAME TO legacy_social_accounts;
ALTER TABLE accounts RENAME TO legacy_accounts;

CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    auth_provider VARCHAR(20) NOT NULL,
    provider_subject VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    login_fail_count SMALLINT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_provider_subject UNIQUE (auth_provider, provider_subject),
    CONSTRAINT ck_users_auth_identity CHECK (
        (auth_provider = 'EMAIL' AND email IS NOT NULL AND password_hash IS NOT NULL)
        OR (auth_provider <> 'EMAIL' AND provider_subject IS NOT NULL)
    )
);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    nickname VARCHAR(30) NOT NULL,
    phone VARCHAR(20) UNIQUE,
    preferred_designer VARCHAR(30),
    hair_cautions TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
    refresh_token_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    device_id VARCHAR(100),
    platform VARCHAR(10),
    app_version VARCHAR(20),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    rotated_to UUID REFERENCES refresh_tokens(refresh_token_id),
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account_deletion_requests (
    deletion_request_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE consent_history (
    consent_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    consent_type VARCHAR(30) NOT NULL,
    granted BOOLEAN NOT NULL,
    policy_version VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_account_deletion_requests_user_id ON account_deletion_requests(user_id);
CREATE INDEX idx_consent_history_user_changed ON consent_history(user_id, changed_at DESC);
