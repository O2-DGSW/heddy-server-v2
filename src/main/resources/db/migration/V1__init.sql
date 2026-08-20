-- V1: 계정·프로필 베이스라인 (users / user_profiles / hair_profiles / style_tags / user_style_preferences / consent_history)
-- PostgreSQL 16. 모든 PK/FK 는 UUID(v7, 애플리케이션 생성), 모든 시각은 TIMESTAMPTZ(UTC).

-- 회원 계정. email 이 식별자이며 소셜 로그인은 auth_provider + provider_subject 로 구분한다.
CREATE TABLE users (
    id                UUID         NOT NULL,
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255),
    nickname          VARCHAR(30)  NOT NULL,
    auth_provider     VARCHAR(20)  NOT NULL,
    provider_subject  VARCHAR(255),
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    login_fail_count  SMALLINT     NOT NULL DEFAULT 0,
    locked_until      TIMESTAMPTZ,
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_provider_subject UNIQUE (auth_provider, provider_subject),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DELETION_PENDING', 'DELETED')),
    CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('EMAIL', 'KAKAO', 'APPLE', 'GOOGLE')),
    CONSTRAINT ck_users_login_fail_count CHECK (login_fail_count >= 0),
    -- 자격증명 정합성. 이 CHECK 가 provider_subject 의 NULL 을 막아
    -- uq_users_provider_subject 가 소셜 계정 중복을 실제로 차단하게 한다
    -- (PostgreSQL 의 UNIQUE 는 NULL 을 서로 distinct 로 본다).
    CONSTRAINT ck_users_credential CHECK (
        (auth_provider = 'EMAIL' AND password_hash IS NOT NULL AND provider_subject IS NULL)
        OR (auth_provider <> 'EMAIL' AND provider_subject IS NOT NULL)
    )
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_created_at ON users (created_at);

COMMENT ON TABLE users IS '회원 계정';
COMMENT ON COLUMN users.password_hash IS '이메일 가입 시에만 존재. 소셜 가입은 NULL';
COMMENT ON COLUMN users.provider_subject IS '소셜 공급자 사용자 식별자. 이메일 가입은 NULL';
COMMENT ON COLUMN users.status IS 'ACTIVE / LOCKED / DELETION_PENDING / DELETED';
COMMENT ON COLUMN users.locked_until IS '로그인 5회 연속 실패 시 잠금 해제 시각';

-- 회원 부가 프로필. users 와 1:1.
CREATE TABLE user_profiles (
    id                 UUID        NOT NULL,
    user_id            UUID        NOT NULL,
    phone              VARCHAR(20),
    preferred_designer VARCHAR(50),
    hair_cautions      VARCHAR(500),
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ,
    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT uq_user_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON TABLE user_profiles IS '회원 부가 프로필 (users 와 1:1)';
COMMENT ON COLUMN user_profiles.hair_cautions IS '시술 시 주의사항 자유 입력';

-- 모발 프로필. users 와 1:1.
CREATE TABLE hair_profiles (
    id                          UUID        NOT NULL,
    user_id                     UUID        NOT NULL,
    hair_type                   VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    hair_condition              VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    hair_length                 VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    hair_thickness              VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    available_care_time_minutes SMALLINT,
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ,
    CONSTRAINT pk_hair_profiles PRIMARY KEY (id),
    CONSTRAINT uq_hair_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_hair_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_hair_profiles_hair_type CHECK (hair_type IN ('STRAIGHT', 'WAVY', 'CURLY', 'COILY', 'UNKNOWN')),
    CONSTRAINT ck_hair_profiles_hair_condition CHECK (hair_condition IN ('DRY', 'NORMAL', 'OILY', 'TREATED', 'UNKNOWN')),
    CONSTRAINT ck_hair_profiles_hair_length CHECK (hair_length IN ('VERY_SHORT', 'SHORT', 'MEDIUM', 'LONG', 'VERY_LONG', 'UNKNOWN')),
    CONSTRAINT ck_hair_profiles_hair_thickness CHECK (hair_thickness IN ('THIN', 'NORMAL', 'THICK', 'UNKNOWN')),
    CONSTRAINT ck_hair_profiles_care_time CHECK (available_care_time_minutes IS NULL OR available_care_time_minutes >= 0)
);

COMMENT ON TABLE hair_profiles IS '모발 프로필 (users 와 1:1)';
COMMENT ON COLUMN hair_profiles.available_care_time_minutes IS '하루 관리 가능 시간(분)';

-- 스타일 태그 마스터.
CREATE TABLE style_tags (
    id         UUID        NOT NULL,
    tag_name   VARCHAR(30) NOT NULL,
    tag_type   VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_style_tags PRIMARY KEY (id),
    CONSTRAINT uq_style_tags_type_name UNIQUE (tag_type, tag_name),
    CONSTRAINT ck_style_tags_tag_type CHECK (tag_type IN ('TREATMENT', 'COLOR', 'MOOD'))
);

-- tag_type 단독 인덱스는 두지 않는다. uq_style_tags_type_name 의 leftmost prefix 가 같은 역할을 한다.

COMMENT ON TABLE style_tags IS '스타일 태그 마스터';
COMMENT ON COLUMN style_tags.tag_type IS 'TREATMENT / COLOR / MOOD';

-- 회원별 선호·제외 태그. 태그당 1행.
CREATE TABLE user_style_preferences (
    id              UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    style_tag_id    UUID        NOT NULL,
    preference_type VARCHAR(10) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ,
    CONSTRAINT pk_user_style_preferences PRIMARY KEY (id),
    CONSTRAINT uq_user_style_preferences_user_tag UNIQUE (user_id, style_tag_id),
    CONSTRAINT fk_user_style_preferences_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_style_preferences_style_tag FOREIGN KEY (style_tag_id) REFERENCES style_tags (id),
    CONSTRAINT ck_user_style_preferences_type CHECK (preference_type IN ('PREFER', 'EXCLUDE'))
);

CREATE INDEX idx_user_style_preferences_user_type ON user_style_preferences (user_id, preference_type);
CREATE INDEX idx_user_style_preferences_style_tag_id ON user_style_preferences (style_tag_id);

COMMENT ON TABLE user_style_preferences IS '회원별 선호·제외 스타일 태그';
COMMENT ON COLUMN user_style_preferences.preference_type IS 'PREFER / EXCLUDE. 같은 태그를 양쪽에 둘 수 없어 UNIQUE(user_id, style_tag_id)';

-- 약관·동의 이력. append-only — 변경 시 UPDATE 하지 않고 새 행을 추가한다.
CREATE TABLE consent_history (
    id             UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    consent_type   VARCHAR(30) NOT NULL,
    agreed         BOOLEAN     NOT NULL,
    policy_version VARCHAR(20) NOT NULL,
    source         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_consent_history PRIMARY KEY (id),
    CONSTRAINT fk_consent_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_consent_history_consent_type CHECK (consent_type IN (
        'TERMS_OF_SERVICE', 'PRIVACY_POLICY', 'AI_TRAINING',
        'SERVICE_ANALYTICS', 'PUSH_NOTIFICATION', 'MARKETING_NOTIFICATION')),
    CONSTRAINT ck_consent_history_source CHECK (source IN ('SIGNUP', 'SETTINGS', 'ADMIN', 'SYSTEM'))
);

CREATE INDEX idx_consent_history_user_type_created_at ON consent_history (user_id, consent_type, created_at DESC);

COMMENT ON TABLE consent_history IS '약관·동의 이력 (append-only). 현재 상태는 (user_id, consent_type) 별 최신 행이다';
COMMENT ON COLUMN consent_history.policy_version IS '동의 시점의 약관 버전';
COMMENT ON COLUMN consent_history.source IS '동의가 기록된 경로 — SIGNUP / SETTINGS / ADMIN / SYSTEM';
