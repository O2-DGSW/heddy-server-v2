-- 헤어 컬러 공용 카탈로그. hairstyle_assets 와 짝을 이루며 추천·저장·AR 이 함께 참조한다.
-- 지금까지 color_id 는 가리킬 대상이 없는 UUID 컬럼이라 아무도 채우지 못했다.
CREATE TABLE hair_colors (
    color_id UUID PRIMARY KEY,
    -- 표시명이 바뀌어도 흔들리지 않는 식별자. 클라이언트 분기와 AR 매핑이 이 값을 쓴다.
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    -- 색상 칩의 점 색. #RRGGBB 형태만 허용해 화면이 그대로 쓸 수 있게 한다.
    hex_code VARCHAR(7) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    -- 쓰지 않게 된 색은 지우지 않고 내린다. 이미 저장된 후보가 가리키고 있기 때문이다.
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hair_colors_code UNIQUE (code),
    CONSTRAINT chk_hair_colors_hex CHECK (hex_code ~ '^#[0-9A-F]{6}$')
);

-- 팔레트는 활성 색을 노출 순서대로 통째로 읽는다.
CREATE INDEX idx_hair_colors_active_order ON hair_colors(active, sort_order, color_id);

-- 식별자는 환경 간 동일해야 한다(V6__seed_style_tags.sql 과 같은 방식).
INSERT INTO hair_colors (color_id, code, name, hex_code, sort_order) VALUES
    ('c0100000-0000-4000-8000-000000000001', 'NATURAL_BLACK',    '내추럴 블랙',   '#1C1C1C', 1),
    ('c0100000-0000-4000-8000-000000000002', 'SOFT_BLACK',       '소프트 블랙',   '#2B2622', 2),
    ('c0100000-0000-4000-8000-000000000003', 'DARK_BROWN',       '다크 브라운',   '#3B2A20', 3),
    ('c0100000-0000-4000-8000-000000000004', 'CHOCOLATE_BROWN',  '초콜릿 브라운', '#4A3123', 4),
    ('c0100000-0000-4000-8000-000000000005', 'ASH_BROWN',        '애쉬 브라운',   '#6B5B4E', 5),
    ('c0100000-0000-4000-8000-000000000006', 'MILK_TEA_BROWN',   '밀크티 브라운', '#A98A6B', 6),
    ('c0100000-0000-4000-8000-000000000007', 'ASH_GRAY',         '애쉬 그레이',   '#7C7B79', 7),
    ('c0100000-0000-4000-8000-000000000008', 'ORANGE_BROWN',     '오렌지 브라운', '#8C4A22', 8),
    ('c0100000-0000-4000-8000-000000000009', 'WINE_RED',         '와인 레드',     '#6B2233', 9),
    ('c0100000-0000-4000-8000-00000000000a', 'BLONDE',           '블론드',        '#C9A227', 10);

-- 카탈로그가 생겼으니 지금까지 비워 둔 참조 무결성을 연결한다. 기존 행의 color_id 는
-- 전부 NULL 이라 이 시점이 가장 싸다. saved_styles 는 도메인 확장과 함께 연결한다.
ALTER TABLE recommendation_items
    ADD CONSTRAINT fk_recommendation_items_color
    FOREIGN KEY (color_id) REFERENCES hair_colors(color_id);
