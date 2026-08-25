-- 취소된 업로드 세션의 스토리지 객체 회수를 추적한다.
--
-- 취소는 행을 DELETED 로 전이하고 객체를 지우지만, 발급된 presigned PUT URL 은 expires_at 까지
-- 유효하다. 취소 직후 도착한 PUT 이나 클라이언트 재시도가 성공하면 객체만 되살아나고, DELETED 인
-- 행은 PENDING·READY 를 훑는 정리 작업의 대상이 아니라 영구 고아 객체가 된다.
--
-- reclaimed_at 은 "URL 이 만료된 뒤 객체를 최종 회수했다"는 표시다. 취소 시점에는 채우지 않는다 —
-- 그 시점의 삭제는 아직 되살아날 수 있는 삭제라 최종 회수가 아니다. 정리 작업은 status = 'DELETED'
-- 이면서 reclaimed_at 이 비어 있고 expires_at 이 지난 행을 다시 훑어 객체를 지우고 이 값을 채운다.
ALTER TABLE files ADD COLUMN reclaimed_at TIMESTAMP WITH TIME ZONE;

-- 회수 대상만 담는 부분 인덱스. 대부분의 행(PENDING·READY·회수 완료)은 여기에 들어오지 않는다.
CREATE INDEX idx_files_reclaim_targets ON files(expires_at)
    WHERE status = 'DELETED' AND reclaimed_at IS NULL;
