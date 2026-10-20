-- GPU AR 서버의 /grooms name과 추천 UUID의 명시적 연결. 기존 추천 이력은 유지한다.
ALTER TABLE hairstyle_assets ADD COLUMN ar_groom_name VARCHAR(80);
ALTER TABLE hairstyle_assets ADD CONSTRAINT chk_hairstyle_ar_groom_name
    CHECK (ar_groom_name IS NULL OR
        (ar_mode = 'THREE_D' AND ar_groom_name ~ '^[a-z0-9][a-z0-9-]{0,79}$'));
CREATE UNIQUE INDEX uk_hairstyle_ar_groom_name ON hairstyle_assets(ar_groom_name)
    WHERE ar_groom_name IS NOT NULL;
