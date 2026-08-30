-- AI 모발 분석의 결과(result) 테이블. 작업(analysis_jobs, V22)이 상태만 갖고 결과값을 갖지
-- 않으므로 저장처를 따로 둔다. 오버레이 이미지는 후속 이슈(#98)의 별도 테이블이다.
--
-- 지표는 map 이 아니라 컬럼으로 편다. 지표 4종은 스펙이 고정한 집합이고, 비교 분석이
-- 지표별로 두 결과를 나란히 읽어야 해서 컬럼이어야 질의가 단순하다.
--
-- 점수는 NUMERIC(5,2) 다. 응답 예시가 82.4·76.2 같은 값을 쓰므로 정수 타입으로 두면 저장
-- 단계에서 값이 깎인다.
--
-- 등급(grade)은 AI 서버가 준 값을 그대로 저장하고 점수에서 다시 계산하지 않는다. 스펙의 등급
-- 기준표(90~100=HIGH)와 응답 예시(82.4=HIGH)가 서로 맞지 않아 계산식을 여기서 고르면 어느
-- 쪽이든 스펙과 어긋난다. 기준이 확정되면 AI 서버 한 곳만 고치면 된다.
--
-- model_version 은 필수다. 모델이 바뀌면 같은 사진이라도 점수가 달라져, 어느 모델이 낸
-- 점수인지 모르면 과거 결과와 비교하는 기능 자체가 성립하지 않는다.
CREATE TABLE analysis_results (
    analysis_id UUID PRIMARY KEY,
    job_id UUID NOT NULL UNIQUE REFERENCES analysis_jobs(job_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id),
    record_id UUID NOT NULL REFERENCES treatment_records(record_id) ON DELETE CASCADE,
    photo_id UUID REFERENCES treatment_record_photos(photo_id) ON DELETE SET NULL,
    color_uniformity_score NUMERIC(5, 2) NOT NULL,
    color_uniformity_grade VARCHAR(10) NOT NULL,
    shape_symmetry_score NUMERIC(5, 2) NOT NULL,
    shape_symmetry_grade VARCHAR(10) NOT NULL,
    volume_balance_score NUMERIC(5, 2) NOT NULL,
    volume_balance_grade VARCHAR(10) NOT NULL,
    roughness_score NUMERIC(5, 2) NOT NULL,
    roughness_grade VARCHAR(10) NOT NULL,
    confidence_score NUMERIC(5, 2) NOT NULL,
    confidence_grade VARCHAR(10) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    summary VARCHAR(500),
    evidence JSONB,
    analyzed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 작업 하나에 결과는 하나다(job_id UNIQUE). 콜백이 중복 도착해도 완료된 결과를 덮어쓰지 않게
-- 막는 마지막 방어선이다 — 멱등 처리는 애플리케이션이 하지만 DB 도 같은 계약을 알아야 한다.

-- 기록의 최신 분석 결과 조회가 기본 접근 경로다. 정렬 키는 작업 테이블과 같은 모양으로 둔다.
CREATE INDEX idx_analysis_results_record_analyzed
    ON analysis_results(record_id, analyzed_at DESC, analysis_id DESC);
