-- 업로드 파이프라인의 파일 메타데이터. 공개 URL 은 저장하지 않고 object_key 만 보관한다.
--
-- upload_id 는 업로드 세션 식별자로 file_id 와 분리한다. presign 응답과 complete 요청이
-- 이 값을 쓰므로, 파일 레코드의 식별자와 같은 값이라고 전제하면 나중에 세션과 파일의
-- 수명이 갈릴 때(재시도로 세션만 새로 발급하는 경우) 바꿀 수 없다.
--
-- user_id 에 ON DELETE CASCADE 를 걸지 않는다. 사용자를 지울 때 행이 함께 사라지면
-- object_key 를 잃어 스토리지 객체를 회수할 방법이 없어진다. 탈퇴 처리가 파일을 DELETED 로
-- 전이시키고 객체를 회수한 뒤에야 사용자 행을 지울 수 있다.
CREATE TABLE files (
    file_id UUID PRIMARY KEY,
    upload_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id),
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    -- 아래 셋은 complete 시점에 실물을 검증하면서 채운다. PENDING 동안에는 비어 있다.
    sha256 CHAR(64),
    width INTEGER,
    height INTEGER,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_files_upload_id UNIQUE (upload_id),
    CONSTRAINT uk_files_object_key UNIQUE (object_key)
);

CREATE INDEX idx_files_user_id ON files(user_id);

-- 만료된 PENDING 세션을 훑는 정리 작업이 이 인덱스를 탄다.
CREATE INDEX idx_files_status_expires_at ON files(status, expires_at);
