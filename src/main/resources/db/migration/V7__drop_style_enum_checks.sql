-- 문자열 enum 은 애플리케이션에서 검증한다. 값 추가 시 스키마 변경이 필요하지 않도록
-- 초기 스키마에 포함된 고정 CHECK 제약조건을 제거한다.
ALTER TABLE style_tags
    DROP CONSTRAINT ck_style_tags_category;

ALTER TABLE user_style_preferences
    DROP CONSTRAINT ck_user_style_preferences_type;
