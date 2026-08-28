-- AI 추천 결과 중 사용자가 보관한 후보 스타일. 공유는 이 행을 가리키므로 원본 추천이
-- 바뀌거나 사라져도 저장 당시의 이름·이미지·추천 이유를 그대로 유지한다.
CREATE TABLE saved_styles (
    saved_style_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    style_name VARCHAR(100) NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 내 저장 스타일 화면과 공유 대상 선택은 최신 저장순으로 조회한다.
CREATE INDEX idx_saved_styles_user_created
    ON saved_styles(user_id, created_at DESC, saved_style_id DESC);

-- V19에서는 saved_styles가 없어 의도적으로 생략했던 참조 무결성을 이제 연결한다.
ALTER TABLE share_saved_styles
    ADD CONSTRAINT fk_share_saved_styles_saved_style
    FOREIGN KEY (saved_style_id) REFERENCES saved_styles(saved_style_id);
