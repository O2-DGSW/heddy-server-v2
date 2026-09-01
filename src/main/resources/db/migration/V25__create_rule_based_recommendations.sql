-- 카탈로그 파일은 특정 사용자 소유가 아니다. 기존 행은 USER로 보존하고 SYSTEM만 user_id를 비운다.
ALTER TABLE files ADD COLUMN owner_type VARCHAR(10) NOT NULL DEFAULT 'USER';
ALTER TABLE files ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE files ADD CONSTRAINT chk_files_owner
    CHECK ((owner_type = 'USER' AND user_id IS NOT NULL)
        OR (owner_type = 'SYSTEM' AND user_id IS NULL));

-- 규칙 기반 추천용 스타일 카탈로그. 이미지 URL은 저장하지 않고 files의 object_key를 간접 참조한다.
CREATE TABLE hairstyle_assets (
    hairstyle_id UUID PRIMARY KEY,
    style_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    thumbnail_file_id UUID REFERENCES files(file_id),
    base_file_id UUID REFERENCES files(file_id),
    mask_file_id UUID REFERENCES files(file_id),
    ar_mode VARCHAR(30) NOT NULL DEFAULT 'NONE',
    anchor_config JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    asset_version VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hairstyle_assets_active ON hairstyle_assets(active, hairstyle_id);

CREATE TABLE hairstyle_recommendation_profiles (
    hairstyle_id UUID PRIMARY KEY REFERENCES hairstyle_assets(hairstyle_id) ON DELETE CASCADE,
    service_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    compatible_hair_lengths JSONB NOT NULL DEFAULT '[]'::jsonb,
    compatible_hair_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    compatible_hair_thicknesses JSONB NOT NULL DEFAULT '[]'::jsonb,
    compatible_hair_conditions JSONB NOT NULL DEFAULT '[]'::jsonb,
    contraindicated_hair_conditions JSONB NOT NULL DEFAULT '[]'::jsonb,
    minimum_hair_length VARCHAR(30),
    estimated_daily_care_minutes INTEGER NOT NULL,
    management_difficulty VARCHAR(20) NOT NULL,
    chemical_stress_level VARCHAR(20) NOT NULL,
    editorial_priority INTEGER NOT NULL DEFAULT 0,
    metadata_version VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hairstyle_care_minutes_nonnegative
        CHECK (estimated_daily_care_minutes >= 0)
);

CREATE TABLE hairstyle_style_tags (
    hairstyle_id UUID NOT NULL REFERENCES hairstyle_assets(hairstyle_id) ON DELETE CASCADE,
    style_tag_id UUID NOT NULL REFERENCES style_tags(style_tag_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (hairstyle_id, style_tag_id)
);

CREATE INDEX idx_hairstyle_style_tags_tag ON hairstyle_style_tags(style_tag_id, hairstyle_id);

-- 기존 스냅샷 저장 스타일을 보존하면서 새 카탈로그 식별자를 선택적으로 연결한다.
-- 레거시 style_name/image_url/reason은 공유 기능 마이그레이션 전까지 제거하지 않는다.
ALTER TABLE saved_styles ADD COLUMN hairstyle_id UUID REFERENCES hairstyle_assets(hairstyle_id);
ALTER TABLE saved_styles ADD COLUMN color_id UUID;
ALTER TABLE saved_styles ADD COLUMN capture_id UUID REFERENCES files(file_id);
ALTER TABLE saved_styles ADD COLUMN memo VARCHAR(500);
CREATE INDEX idx_saved_styles_user_hairstyle
    ON saved_styles(user_id, hairstyle_id) WHERE hairstyle_id IS NOT NULL;

CREATE TABLE recommendation_runs (
    recommendation_run_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    strategy VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    input_snapshot_json JSONB NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_recommendation_strategy CHECK (strategy IN ('RULE_BASED_V1')),
    CONSTRAINT chk_recommendation_status CHECK (status IN ('ACTIVE', 'STALE'))
);

CREATE INDEX idx_recommendation_runs_user_latest
    ON recommendation_runs(user_id, generated_at DESC, recommendation_run_id DESC);
CREATE INDEX idx_recommendation_runs_reuse
    ON recommendation_runs(user_id, strategy, input_hash, status);

CREATE TABLE recommendation_items (
    recommendation_item_id UUID PRIMARY KEY,
    recommendation_run_id UUID NOT NULL REFERENCES recommendation_runs(recommendation_run_id) ON DELETE CASCADE,
    hairstyle_id UUID NOT NULL REFERENCES hairstyle_assets(hairstyle_id),
    color_id UUID,
    display_rank INTEGER NOT NULL,
    score NUMERIC(5,2) NOT NULL,
    score_breakdown_json JSONB NOT NULL,
    reasons_json JSONB NOT NULL,
    management_difficulty VARCHAR(20) NOT NULL,
    estimated_daily_care_minutes INTEGER NOT NULL,
    CONSTRAINT uk_recommendation_items_rank UNIQUE (recommendation_run_id, display_rank),
    CONSTRAINT chk_recommendation_item_rank CHECK (display_rank BETWEEN 1 AND 3),
    CONSTRAINT chk_recommendation_item_score CHECK (score BETWEEN 0 AND 100)
);

CREATE INDEX idx_recommendation_items_run ON recommendation_items(recommendation_run_id, display_rank);

CREATE TABLE recommendation_reference_records (
    recommendation_item_id UUID NOT NULL REFERENCES recommendation_items(recommendation_item_id) ON DELETE CASCADE,
    record_id UUID NOT NULL REFERENCES treatment_records(record_id) ON DELETE CASCADE,
    reference_reason_code VARCHAR(60) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recommendation_item_id, record_id)
);
