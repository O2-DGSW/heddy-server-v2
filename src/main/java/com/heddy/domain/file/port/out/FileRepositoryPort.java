package com.heddy.domain.file.port.out;

import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;

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
}
