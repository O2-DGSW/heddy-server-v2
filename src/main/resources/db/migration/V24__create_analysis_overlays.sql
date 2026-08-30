-- 분석 결과를 사진 위에 겹쳐 보여주는 오버레이 이미지. 결과(analysis_results, V23)와 생애주기가
-- 달라 결과 행에 컬럼으로 붙이지 않는다 — 종류가 늘 때 결과 테이블을 건드리지 않아야 한다.
--
-- 이미지는 공개 URL 이 아니라 file_id 로 참조한다. 오버레이 PNG 는 비공개 버킷에 올라가고
-- (purpose=ANALYSIS_OVERLAY_INTERNAL) 조회 때 짧은 만료 URL 을 발급한다 — 저장된 URL 은
-- 만료돼도 행에 남아 언제든 새는 값이 된다(스펙 19절, 시술기록 사진과 같은 규칙).
CREATE TABLE analysis_overlays (
    overlay_id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES analysis_results(analysis_id) ON DELETE CASCADE,
    overlay_type VARCHAR(30) NOT NULL,
    file_id UUID NOT NULL REFERENCES files(file_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 한 결과에 같은 종류의 오버레이는 하나다. 콜백이 중복 도착해도 같은 종류가 두 번 쌓이지
    -- 않게 DB 가 막는다 — 결과의 job_id UNIQUE 와 같은 이유의 방어선이다.
    CONSTRAINT uq_analysis_overlays_type UNIQUE (analysis_id, overlay_type)
);

-- 결과 조회가 오버레이를 함께 읽는 유일한 경로다. UNIQUE 제약의 인덱스가 analysis_id 를
-- 선두로 두므로 별도 인덱스를 두지 않는다.

CREATE INDEX idx_analysis_overlays_file_id ON analysis_overlays(file_id);
