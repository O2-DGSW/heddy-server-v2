package com.heddy.domain.file.port.out;

import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepositoryPort {

    StoredFile insert(StoredFile file);

    /**
     * 상태 전이를 저장한다. {@code expectedStatus} 는 전이 <em>전</em>의 상태다.
     *
     * <p>전이를 통째로 덮어쓰지 않고 기대 상태를 함께 받는 이유는, 그 사이 다른 요청이 상태를
     * 바꿔놨을 수 있기 때문이다. 도메인 모델이 막아둔 전이(예: DELETED → READY)가 스냅샷을
     * 조건 없이 저장하는 순간 되살아난다.
     *
     * @throws com.heddy.domain.file.exception.FileException 그 사이 상태가 바뀌었으면
     */
    StoredFile transition(StoredFile file, FileStatus expectedStatus);

    Optional<StoredFile> findById(UUID fileId);

    /** presign 응답과 complete 요청이 쓰는 업로드 세션 식별자로 조회한다. */
    Optional<StoredFile> findByUploadId(UUID uploadId);
    /**
     * 만료 이후 스토리지 객체를 다시 지워야 하는 취소 세션들. 취소가 지운 객체는 발급된
     * presigned PUT URL 이 살아 있는 동안 되살아날 수 있어, DELETED 라는 이유로 정리 대상에서
     * 빼면 그 객체가 영영 회수되지 않는다.
     *
     * @param now   이 시각까지 만료된 세션만 대상이다. 만료 전에는 URL 로 객체가 다시 생길 수 있다
     * @param limit 한 번에 가져올 최대 행 수
     */
    List<StoredFile> findReclaimTargets(Instant now, int limit);

    /** 스토리지 객체를 최종 회수했다고 표시한다. 이미 표시된 행은 건드리지 않는다. */
    void markReclaimed(UUID fileId, Instant reclaimedAt);

    List<StoredFile> findAllByUserId(UUID userId);
}
