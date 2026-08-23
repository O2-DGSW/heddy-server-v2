-- 업로드 파이프라인의 파일 메타데이터. 공개 URL 은 저장하지 않고 object_key 만 보관한다.
-- PENDING 행이 곧 업로드 세션이며, upload_id 는 file_id 와 같은 값이다.
--
-- owner_id 에 ON DELETE CASCADE 를 걸지 않는다. 사용자를 지울 때 행이 함께 사라지면
-- object_key 를 잃어 S3 객체를 회수할 방법이 없어진다. 탈퇴 처리가 파일을 DELETED 로
-- 전이시키고 객체를 회수한 뒤에야 사용자 행을 지울 수 있다.
CREATE TABLE files (
    file_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(user_id),
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(10) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    byte_size BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_files_object_key UNIQUE (object_key)
);

CREATE INDEX idx_files_owner_id ON files(owner_id);

-- 만료된 PENDING 세션과 미연결 READY 파일을 훑는 정리 작업이 이 인덱스를 탄다.
CREATE INDEX idx_files_status_created_at ON files(status, created_at);
