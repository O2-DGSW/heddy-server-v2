-- 시술기록과 첨부 사진. MS-02 저장 계층의 축으로, API 는 후속 이슈(#31)에서 붙인다.
--
-- service_types 를 조인 테이블이 아니라 JSONB 로 두기로 결론 내렸다. 목록 필터는
-- 단일 시술 종류 값 하나로 좁히는 containment 질의(`service_types @> '["CUT"]'`)가
-- 전부이고, 기록당 원소는 최대 7개(실사용 1~3개)라 정규화가 얻어줄 것이 없다.
-- GIN 인덱스가 이 질의를 커버하므로 필터 조회가 잦아도 조인이 유리해지지 않는다.
-- 조인 테이블은 행 수만 늘리고 쓰기·조회 양쪽에 조인 비용을 붙인다.
--
-- salon_name · designer_name 은 엔티티가 아니라 문자열 필드다(v2 축소 스코프).
-- appointment_id 는 예약 스키마가 아직 확정되지 않아 FK 없이 컬럼만 둔다. 예약 도메인이
-- 자리 잡으면 FK 추가 마이그레이션을 별도로 낸다.
--
-- user_id 에 ON DELETE CASCADE 를 걸지 않는다(files 와 같은 판단). 사용자 삭제가 사진 행을
-- 잃게 하면 file_id 를 알 수 없어 스토리지 객체 회수 경로가 끊긴다. 탈퇴 처리는 파일 정리와
-- 마찬가지로 순서를 제어해서 진행한다. 반대로 photos.record_id 는 CASCADE 다. 기록 없는
-- 사진은 아무 의미가 없고, 기록 삭제(API DELETE /treatment-records/{recordId})시 함께
-- 정리되는 게 맞다.
CREATE TABLE treatment_records (
    record_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    service_types JSONB NOT NULL,
    salon_name VARCHAR(50),
    designer_name VARCHAR(30),
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    satisfaction SMALLINT,
    price_amount BIGINT,
    price_currency VARCHAR(3),
    appointment_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 타임라인 조회(내 기록 최신순)가 기본 접근 경로다.
CREATE INDEX idx_treatment_records_user_performed
    ON treatment_records(user_id, performed_at DESC);

-- 시술 종류 필터 질의를 위한 역방향 인덱스.
CREATE INDEX idx_treatment_records_service_types
    ON treatment_records USING GIN (service_types);

CREATE TABLE treatment_record_photos (
    photo_id UUID PRIMARY KEY,
    record_id UUID NOT NULL REFERENCES treatment_records(record_id) ON DELETE CASCADE,
    file_id UUID NOT NULL REFERENCES files(file_id),
    image_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_treatment_record_photos_record_id ON treatment_record_photos(record_id);
CREATE INDEX idx_treatment_record_photos_file_id ON treatment_record_photos(file_id);
