CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    login_id VARCHAR(20) UNIQUE,
    password VARCHAR(100),
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(13) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE social_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_social_accounts_provider_id UNIQUE (provider, provider_id)
);

CREATE INDEX idx_social_accounts_account_id ON social_accounts(account_id);
