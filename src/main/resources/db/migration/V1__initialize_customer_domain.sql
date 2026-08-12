CREATE TABLE app_users (
    id                    BIGSERIAL PRIMARY KEY,
    email                 VARCHAR(255),
    password_hash         VARCHAR(255),
    nickname              VARCHAR(50) NOT NULL,
    phone_number          VARCHAR(20),
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts SMALLINT NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    deleted_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN')),
    CONSTRAINT ck_app_users_login_method CHECK (email IS NOT NULL OR password_hash IS NULL)
);

CREATE TABLE social_accounts (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    provider         VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_social_account UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_social_provider CHECK (provider IN ('KAKAO', 'NAVER', 'GOOGLE', 'APPLE'))
);

CREATE INDEX idx_social_accounts_user_id ON social_accounts(user_id);

CREATE TABLE customer_profiles (
    user_id                    BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    hair_cautions              TEXT,
    preferred_designer        VARCHAR(100),
    daily_maintenance_minutes SMALLINT,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_profile_maintenance_minutes
        CHECK (daily_maintenance_minutes IS NULL OR daily_maintenance_minutes BETWEEN 0 AND 1440)
);

CREATE TABLE consent_histories (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    consent_type VARCHAR(30) NOT NULL,
    agreed       BOOLEAN NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_consent_type CHECK (
        consent_type IN ('TERMS', 'PRIVACY', 'AI_TRAINING', 'SERVICE_ANALYTICS', 'MARKETING', 'PUSH')
    )
);

CREATE INDEX idx_consent_histories_user_type_created
    ON consent_histories(user_id, consent_type, created_at DESC);

CREATE TABLE style_tags (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(50) NOT NULL UNIQUE,
    name       VARCHAR(50) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customer_style_preferences (
    user_id         BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    style_tag_id    BIGINT NOT NULL REFERENCES style_tags(id),
    preference_type VARCHAR(10) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, style_tag_id),
    CONSTRAINT ck_style_preference_type CHECK (preference_type IN ('PREFER', 'AVOID'))
);

CREATE TABLE treatment_records (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    performed_at            TIMESTAMPTZ NOT NULL,
    salon_name              VARCHAR(100),
    designer_name           VARCHAR(100),
    cut_description         VARCHAR(255),
    perm_description        VARCHAR(255),
    color_description       VARCHAR(255),
    products                TEXT,
    duration_minutes        SMALLINT,
    price                   INTEGER,
    satisfaction            SMALLINT,
    memo                    TEXT,
    next_treatment_cautions TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_treatment_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    CONSTRAINT ck_treatment_price CHECK (price IS NULL OR price >= 0),
    CONSTRAINT ck_treatment_satisfaction CHECK (satisfaction IS NULL OR satisfaction BETWEEN 1 AND 5)
);

CREATE INDEX idx_treatment_records_user_performed
    ON treatment_records(user_id, performed_at DESC, id DESC);
CREATE INDEX idx_treatment_records_user_salon
    ON treatment_records(user_id, salon_name);
CREATE INDEX idx_treatment_records_user_designer
    ON treatment_records(user_id, designer_name);

CREATE TABLE treatment_record_services (
    treatment_record_id BIGINT NOT NULL REFERENCES treatment_records(id) ON DELETE CASCADE,
    service_type        VARCHAR(20) NOT NULL,
    PRIMARY KEY (treatment_record_id, service_type),
    CONSTRAINT ck_treatment_service_type
        CHECK (service_type IN ('CUT', 'PERM', 'COLOR', 'CLINIC', 'EXTENSION', 'STYLING', 'OTHER'))
);

CREATE TABLE treatment_photos (
    id                  BIGSERIAL PRIMARY KEY,
    treatment_record_id BIGINT NOT NULL REFERENCES treatment_records(id) ON DELETE CASCADE,
    photo_type          VARCHAR(10) NOT NULL,
    object_key          VARCHAR(500) NOT NULL UNIQUE,
    content_type        VARCHAR(100) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    display_order       SMALLINT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_treatment_photo_order UNIQUE (treatment_record_id, photo_type, display_order),
    CONSTRAINT ck_treatment_photo_type CHECK (photo_type IN ('BEFORE', 'AFTER')),
    CONSTRAINT ck_treatment_photo_size CHECK (file_size_bytes > 0),
    CONSTRAINT ck_treatment_photo_order CHECK (display_order BETWEEN 0 AND 9)
);

CREATE INDEX idx_treatment_photos_record ON treatment_photos(treatment_record_id);

CREATE TABLE analysis_runs (
    id                  BIGSERIAL PRIMARY KEY,
    treatment_record_id BIGINT NOT NULL REFERENCES treatment_records(id) ON DELETE CASCADE,
    source_photo_id     BIGINT NOT NULL REFERENCES treatment_photos(id) ON DELETE CASCADE,
    version             INTEGER NOT NULL,
    status              VARCHAR(20) NOT NULL,
    confidence          NUMERIC(5, 4),
    failure_code        VARCHAR(50),
    failure_message     VARCHAR(500),
    overlay_object_key  VARCHAR(500),
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMPTZ,
    CONSTRAINT uk_analysis_record_version UNIQUE (treatment_record_id, version),
    CONSTRAINT ck_analysis_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_analysis_confidence CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1)
);

CREATE INDEX idx_analysis_runs_record_status
    ON analysis_runs(treatment_record_id, status, version DESC);

CREATE TABLE analysis_metrics (
    id              BIGSERIAL PRIMARY KEY,
    analysis_run_id BIGINT NOT NULL REFERENCES analysis_runs(id) ON DELETE CASCADE,
    metric_type     VARCHAR(30) NOT NULL,
    metric_value    NUMERIC(12, 4) NOT NULL,
    unit            VARCHAR(20),
    evidence        VARCHAR(500) NOT NULL,
    confidence      NUMERIC(5, 4) NOT NULL,
    CONSTRAINT uk_analysis_metric UNIQUE (analysis_run_id, metric_type),
    CONSTRAINT ck_analysis_metric_type CHECK (
        metric_type IN ('COLOR_UNIFORMITY', 'VOLUME_BALANCE', 'SHAPE', 'ROUGHNESS_SIGN')
    ),
    CONSTRAINT ck_analysis_metric_confidence CHECK (confidence BETWEEN 0 AND 1)
);

CREATE TABLE ar_styles (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    asset_object_key   VARCHAR(500) NOT NULL UNIQUE,
    maintenance_level  VARCHAR(10) NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    popularity_score   NUMERIC(8, 4) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ar_style_maintenance CHECK (maintenance_level IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE TABLE ar_style_tags (
    ar_style_id  BIGINT NOT NULL REFERENCES ar_styles(id) ON DELETE CASCADE,
    style_tag_id BIGINT NOT NULL REFERENCES style_tags(id),
    PRIMARY KEY (ar_style_id, style_tag_id)
);

CREATE TABLE ar_style_colors (
    id          BIGSERIAL PRIMARY KEY,
    ar_style_id BIGINT NOT NULL REFERENCES ar_styles(id) ON DELETE CASCADE,
    name        VARCHAR(50) NOT NULL,
    hex_value   CHAR(7) NOT NULL,
    UNIQUE (ar_style_id, id),
    UNIQUE (ar_style_id, hex_value)
);

CREATE TABLE saved_style_candidates (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    ar_style_id        BIGINT NOT NULL REFERENCES ar_styles(id),
    ar_style_color_id  BIGINT NOT NULL,
    capture_object_key VARCHAR(500),
    memo               VARCHAR(500),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_candidate_style_color
        FOREIGN KEY (ar_style_id, ar_style_color_id) REFERENCES ar_style_colors(ar_style_id, id),
    CONSTRAINT uk_saved_style_candidate UNIQUE (user_id, ar_style_id, ar_style_color_id)
);

CREATE INDEX idx_saved_style_candidates_user ON saved_style_candidates(user_id, created_at DESC);

CREATE TABLE recommendation_snapshots (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    source     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    CONSTRAINT ck_recommendation_source CHECK (source IN ('PERSONALIZED', 'POPULAR', 'CACHED'))
);

CREATE INDEX idx_recommendation_snapshots_user_created
    ON recommendation_snapshots(user_id, created_at DESC);

CREATE TABLE recommendation_items (
    recommendation_snapshot_id BIGINT NOT NULL REFERENCES recommendation_snapshots(id) ON DELETE CASCADE,
    ar_style_id                 BIGINT NOT NULL REFERENCES ar_styles(id),
    rank                        SMALLINT NOT NULL,
    score                       NUMERIC(10, 4) NOT NULL,
    reason                      VARCHAR(500) NOT NULL,
    PRIMARY KEY (recommendation_snapshot_id, ar_style_id),
    CONSTRAINT uk_recommendation_rank UNIQUE (recommendation_snapshot_id, rank),
    CONSTRAINT ck_recommendation_rank CHECK (rank BETWEEN 1 AND 3)
);

CREATE TABLE record_shares (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash               VARCHAR(255) NOT NULL UNIQUE,
    share_photos             BOOLEAN NOT NULL DEFAULT FALSE,
    share_treatment_details  BOOLEAN NOT NULL DEFAULT FALSE,
    share_satisfaction       BOOLEAN NOT NULL DEFAULT FALSE,
    share_cautions           BOOLEAN NOT NULL DEFAULT FALSE,
    saved_style_candidate_id BIGINT REFERENCES saved_style_candidates(id) ON DELETE SET NULL,
    expires_at               TIMESTAMPTZ NOT NULL,
    revoked_at               TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_record_shares_user_created ON record_shares(user_id, created_at DESC);
CREATE INDEX idx_record_shares_expires ON record_shares(expires_at) WHERE revoked_at IS NULL;

CREATE TABLE shared_treatment_records (
    share_id            BIGINT NOT NULL REFERENCES record_shares(id) ON DELETE CASCADE,
    treatment_record_id BIGINT NOT NULL REFERENCES treatment_records(id) ON DELETE CASCADE,
    PRIMARY KEY (share_id, treatment_record_id)
);

CREATE TABLE reservations (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    reserved_at              TIMESTAMPTZ NOT NULL,
    salon_name               VARCHAR(100) NOT NULL,
    designer_name            VARCHAR(100),
    planned_treatment        VARCHAR(500),
    saved_style_candidate_id BIGINT REFERENCES saved_style_candidates(id) ON DELETE SET NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_reservation_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELED'))
);

CREATE INDEX idx_reservations_user_reserved ON reservations(user_id, reserved_at DESC);

CREATE TABLE device_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    platform   VARCHAR(10) NOT NULL,
    token      VARCHAR(500) NOT NULL UNIQUE,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_device_platform CHECK (platform IN ('IOS', 'ANDROID'))
);

CREATE INDEX idx_device_tokens_user_active ON device_tokens(user_id, active);

CREATE TABLE notifications (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    type           VARCHAR(20) NOT NULL,
    title          VARCHAR(100) NOT NULL,
    body           VARCHAR(500) NOT NULL,
    target_path    VARCHAR(500),
    read_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_notification_type CHECK (type IN ('RESERVATION', 'ANALYSIS', 'SHARE_EXPIRY'))
);

CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

CREATE TABLE outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts       INTEGER NOT NULL DEFAULT 0,
    available_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_outbox_events_dispatch
    ON outbox_events(status, available_at, id);
