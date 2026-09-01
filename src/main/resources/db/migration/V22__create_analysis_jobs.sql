-- AI 모발 분석의 작업(job) 테이블. 분석 API 6개 중 요청·상태 조회·재시도 3개가 이 테이블
-- 위에서 동작한다. 결과값(지표·등급·요약)과 오버레이는 후속 이슈(#97·#98)의 별도 테이블이다.
--
-- 상태머신은 서버가 소유한다. AI 서버는 결과만 돌려주고 어떤 전이가 유효한지는 서버가
-- 판단한다 — 외부 호출자가 상태를 직접 지정하면 완료된 작업이 다시 진행 중으로 되돌아가는
-- 것을 막을 수 없다.
--
-- user_id 를 기록에서 매번 조인하지 않고 여기 둔다. `GET /analysis-jobs/{jobId}` 는 남의
-- 작업을 없는 작업과 같은 404 로 답해야 하고 질의 횟수도 같아야 하는데(#31 컨벤션), 그러려면
-- 소유자 조건이 작업 조회 한 번에 함께 실려야 한다. 분석 어댑터가 시술기록 테이블을 직접
-- 읽는 쪽은 도메인 경계를 넘는다.
CREATE TABLE analysis_jobs (
    job_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    record_id UUID NOT NULL REFERENCES treatment_records(record_id) ON DELETE CASCADE,
    photo_id UUID REFERENCES treatment_record_photos(photo_id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL,
    progress SMALLINT NOT NULL DEFAULT 0,
    attempt_count SMALLINT NOT NULL DEFAULT 1,
    failure_code VARCHAR(50),
    failure_message VARCHAR(500),
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 같은 사진에 진행 중인 작업이 둘 이상 생기면 콜백이 어느 작업의 결과인지 갈리지 않는다.
-- 애플리케이션 검사만으로는 동시 요청을 막을 수 없어 부분 UNIQUE 인덱스로 DB 가 막는다.
-- 끝난 작업(SUCCEEDED/FAILED/UNAVAILABLE/STALE)은 이력으로 남아야 하므로 제약 밖이다.
CREATE UNIQUE INDEX idx_analysis_jobs_photo_in_progress
    ON analysis_jobs(photo_id)
    WHERE status IN ('PENDING', 'PROCESSING');

-- 기록의 최신 분석 조회(`GET /treatment-records/{recordId}/analyses/latest`, 목록 배지)가
-- 기본 접근 경로다. 같은 시각의 행이 갈리도록 job_id 를 정렬 키에 함께 둔다.
CREATE INDEX idx_analysis_jobs_record_created
    ON analysis_jobs(record_id, created_at DESC, job_id DESC);
