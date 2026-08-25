-- 공유 링크와 선택 항목 조인 테이블 3개. MS-06 저장 계층이며 API 는 후속 이슈(#49~#51)에서 붙는다.
--
-- status 는 ACTIVE / REVOKED 둘뿐이다. 만료는 상태가 아니라 expires_at 과의 비교로 판정한다.
-- 상태로 만들면 만료 시점마다 갱신 작업이 필요해지는데, 매 요청 검증이 스펙(19절)이 요구하는
-- 방식이기도 해 두 표현을 중복으로 둘 이유가 없다.
--
-- share_saved_styles.saved_style_id 에는 FK 가 없다. 후보 스타일 도메인(saved_styles)이 아직
-- 스키마가 없어 treatment_records.appointment_id 의 전례처럼 컬럼만 둔다. 후보 도메인이 자리
-- 잡으면 FK 추가 마이그레이션을 별도로 낸다.
--
-- 토큰은 원문을 저장하지 않고 SHA-256 해시만 저장한다(스펙 11.2·19절). 공개 조회는 해시 대조로만
-- 이루어지므로 DB 유출 시에도 링크를 복원할 수 없다. UNIQUE 제약이 곧 조회 인덱스다.
-- 타입은 VARCHAR 다. Hibernate 는 String 을 VARCHAR 로 검증하므로 CHAR 면 ddl-auto: validate 가
-- 실패한다(V3·V11 이 같은 까닭으로 CHAR 를 고친 전례).
CREATE TABLE shares (
    share_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 내 공유 목록(최신순)이 기본 접근 경로다.
CREATE INDEX idx_shares_user_created ON shares(user_id, created_at DESC, share_id DESC);

CREATE TABLE share_records (
    share_id UUID NOT NULL REFERENCES shares(share_id) ON DELETE CASCADE,
    record_id UUID NOT NULL REFERENCES treatment_records(record_id),
    PRIMARY KEY (share_id, record_id)
);

-- 기록 삭제·재구성 시 해당 기록을 참조하는 공유를 찾을 때 쓴다.
CREATE INDEX idx_share_records_record_id ON share_records(record_id);

CREATE TABLE share_fields (
    share_id UUID NOT NULL REFERENCES shares(share_id) ON DELETE CASCADE,
    field_type VARCHAR(30) NOT NULL,
    PRIMARY KEY (share_id, field_type)
);

CREATE TABLE share_saved_styles (
    share_id UUID NOT NULL REFERENCES shares(share_id) ON DELETE CASCADE,
    saved_style_id UUID NOT NULL,
    PRIMARY KEY (share_id, saved_style_id)
);

CREATE INDEX idx_share_saved_styles_saved_style_id ON share_saved_styles(saved_style_id);
