-- Heddy API v2 / 테이블 명세와 실제 저장 스키마의 정합화.
-- 기존 운영 컬럼은 하위 호환을 위해 유지하고, 명세가 요구하는 컬럼과 관계를 추가한다.

ALTER TABLE treatment_records
    ADD COLUMN timezone VARCHAR(50),
    ADD COLUMN cut_length VARCHAR(20),
    ADD COLUMN cut_shape VARCHAR(20),
    ADD COLUMN perm_type VARCHAR(20),
    ADD COLUMN color_name VARCHAR(30),
    ADD COLUMN products JSONB;

UPDATE treatment_records SET timezone = 'Asia/Seoul' WHERE timezone IS NULL;
ALTER TABLE treatment_records ALTER COLUMN timezone SET DEFAULT 'Asia/Seoul';
ALTER TABLE treatment_records ALTER COLUMN timezone SET NOT NULL;
ALTER TABLE treatment_records
    ALTER COLUMN duration_minutes TYPE INTEGER USING duration_minutes::INTEGER;

ALTER TABLE treatment_record_photos
    ALTER COLUMN image_type TYPE VARCHAR(10),
    ALTER COLUMN sort_order TYPE SMALLINT USING sort_order::SMALLINT;

ALTER TABLE analysis_jobs
    ADD COLUMN analysis_id UUID REFERENCES analysis_results(analysis_id) ON DELETE SET NULL,
    ADD COLUMN previous_record_id UUID REFERENCES treatment_records(record_id) ON DELETE SET NULL;

UPDATE analysis_jobs job
SET analysis_id = result.analysis_id
FROM analysis_results result
WHERE result.job_id = job.job_id;

ALTER TABLE analysis_jobs
    ALTER COLUMN attempt_count TYPE INTEGER USING attempt_count::INTEGER,
    ALTER COLUMN failure_message TYPE TEXT;

ALTER TABLE analysis_results ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUCCEEDED';
ALTER TABLE analysis_results RENAME COLUMN summary TO summary_comment;
ALTER TABLE analysis_results RENAME COLUMN evidence TO evidence_json;

ALTER TABLE analysis_results
    ALTER COLUMN color_uniformity_score DROP NOT NULL,
    ALTER COLUMN color_uniformity_grade DROP NOT NULL,
    ALTER COLUMN shape_symmetry_score DROP NOT NULL,
    ALTER COLUMN shape_symmetry_grade DROP NOT NULL,
    ALTER COLUMN volume_balance_score DROP NOT NULL,
    ALTER COLUMN volume_balance_grade DROP NOT NULL,
    ALTER COLUMN roughness_score DROP NOT NULL,
    ALTER COLUMN roughness_grade DROP NOT NULL;

CREATE FUNCTION link_analysis_result_to_job() RETURNS TRIGGER AS $$
BEGIN
    UPDATE analysis_jobs
    SET analysis_id = NEW.analysis_id
    WHERE job_id = NEW.job_id;
    UPDATE analysis_results result
    SET status = job.status
    FROM analysis_jobs job
    WHERE result.analysis_id = NEW.analysis_id
      AND job.job_id = NEW.job_id
      AND job.status IN ('SUCCEEDED', 'STALE');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_link_analysis_result_to_job
    AFTER INSERT ON analysis_results
    FOR EACH ROW EXECUTE FUNCTION link_analysis_result_to_job();

CREATE FUNCTION sync_analysis_result_status() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IN ('SUCCEEDED', 'STALE') THEN
        UPDATE analysis_results
        SET status = NEW.status
        WHERE job_id = NEW.job_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_analysis_result_status
    AFTER UPDATE OF status ON analysis_jobs
    FOR EACH ROW EXECUTE FUNCTION sync_analysis_result_status();

-- 기존 공용 색상 카탈로그는 유지하고 스타일별 지원 관계를 명세 이름으로 제공한다.
CREATE TABLE hairstyle_colors (
    color_id UUID NOT NULL REFERENCES hair_colors(color_id),
    hairstyle_id UUID NOT NULL REFERENCES hairstyle_assets(hairstyle_id) ON DELETE CASCADE,
    name VARCHAR(30) NOT NULL,
    hex_code CHAR(7) NOT NULL,
    sort_order SMALLINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (hairstyle_id, color_id)
);

ALTER TABLE hairstyle_assets
    ALTER COLUMN style_name TYPE VARCHAR(30),
    ALTER COLUMN category TYPE VARCHAR(20),
    ALTER COLUMN ar_mode TYPE VARCHAR(20),
    ALTER COLUMN asset_version TYPE VARCHAR(20);

CREATE INDEX idx_hairstyle_colors_color_id ON hairstyle_colors(color_id);

INSERT INTO hairstyle_colors (color_id, hairstyle_id, name, hex_code, sort_order)
SELECT color.color_id, style.hairstyle_id, color.name, color.hex_code, color.sort_order::SMALLINT
FROM hair_colors color
CROSS JOIN hairstyle_assets style;

CREATE TABLE ar_captures (
    capture_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    hairstyle_id UUID NOT NULL REFERENCES hairstyle_assets(hairstyle_id),
    color_id UUID NOT NULL REFERENCES hair_colors(color_id),
    file_id UUID NOT NULL REFERENCES files(file_id),
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ar_captures
    ADD CONSTRAINT fk_ar_captures_supported_color
    FOREIGN KEY (hairstyle_id, color_id)
    REFERENCES hairstyle_colors(hairstyle_id, color_id);

CREATE INDEX idx_ar_captures_user_captured
    ON ar_captures(user_id, captured_at DESC, capture_id DESC);

ALTER TABLE recommendation_reference_records
    RENAME COLUMN reference_reason_code TO reference_reason;
ALTER TABLE recommendation_reference_records
    ALTER COLUMN reference_reason TYPE VARCHAR(255);

ALTER TABLE recommendation_items
    ALTER COLUMN display_rank TYPE SMALLINT USING display_rank::SMALLINT,
    ALTER COLUMN management_difficulty TYPE VARCHAR(10);

ALTER TABLE saved_styles ALTER COLUMN memo TYPE TEXT;
