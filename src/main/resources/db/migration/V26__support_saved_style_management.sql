-- 후보 스타일 메모 수정 API를 지원한다. V25가 먼저 적용된 환경에는 이미 memo가 있으므로
-- IF NOT EXISTS로 개발/운영 브랜치의 마이그레이션 순서 차이를 흡수한다.
ALTER TABLE saved_styles
    ADD COLUMN IF NOT EXISTS memo VARCHAR(500);

-- 사용자가 후보를 삭제하면 기존 공유의 후보 연결도 함께 제거한다. 공유 자체와 다른 항목은
-- 유지하되, 삭제된 후보가 공개 조회에서 다시 노출되지는 않는다.
ALTER TABLE share_saved_styles
    DROP CONSTRAINT IF EXISTS fk_share_saved_styles_saved_style;

ALTER TABLE share_saved_styles
    ADD CONSTRAINT fk_share_saved_styles_saved_style
    FOREIGN KEY (saved_style_id) REFERENCES saved_styles(saved_style_id) ON DELETE CASCADE;
