-- saved_styles 는 AI 추천 결과의 스냅샷만 담는다는 전제로 만들어져 image_url 과 reason 이
-- NOT NULL 이다. AR 에서 저장하는 후보에는 추천 이유가 없고, 이미지도 URL 문자열이 아니라
-- files 를 가리키는 capture_id 로 다뤄야 해 지금 구조로는 저장 경로를 만들 수 없다.
-- 레거시 스냅샷 행은 그대로 두고 제약만 푼다.
ALTER TABLE saved_styles ALTER COLUMN image_url DROP NOT NULL;
ALTER TABLE saved_styles ALTER COLUMN reason DROP NOT NULL;

-- 컬럼만 있고 가리킬 표가 없던 color_id 를 카탈로그에 연결한다. 기존 값은 전부 NULL 이다.
ALTER TABLE saved_styles
    ADD CONSTRAINT fk_saved_styles_color
    FOREIGN KEY (color_id) REFERENCES hair_colors(color_id);
