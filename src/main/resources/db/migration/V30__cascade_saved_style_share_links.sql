-- 후보가 삭제되면 공유 링크는 유지하고 해당 후보 연결만 함께 제거한다.
ALTER TABLE share_saved_styles
    DROP CONSTRAINT IF EXISTS fk_share_saved_styles_saved_style;

ALTER TABLE share_saved_styles
    ADD CONSTRAINT fk_share_saved_styles_saved_style
    FOREIGN KEY (saved_style_id) REFERENCES saved_styles(saved_style_id) ON DELETE CASCADE;
