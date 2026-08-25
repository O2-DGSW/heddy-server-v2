package com.heddy.application.file.service;

import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정리 후보 파일 한 건을 자기 트랜잭션으로 마무리한다.
 *
 * <p>{@code REQUIRES_NEW} 인 이유 — 여러 후보를 한 트랜잭션에 몰아 넣으면 한 건의 DB 실패가
 * 세션 전체를 물고 늘어져 이미 성공한 건까지 롤백되고, 실패한 건 뒤의 후보도 연쇄로 놓친다.
 * 스토리지 객체 삭제는 트랜잭션이 되돌리지 못하므로, 건별 커밋이 남긴 "행만 남은 상태"는
 * 다음 실행이 다시 훑어 회수한다. 스케줄러는 결과 카운트만 본다.
 */
@Component
public class FileCleanupProcessor {

    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;

    public FileCleanupProcessor(
            FileRepositoryPort fileRepositoryPort, FileStoragePort fileStoragePort) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(StoredFile candidate) {
        if (candidate.status() == FileStatus.DELETED && candidate.reclaimedAt() != null) {
            // 회수가 확정된 취소 세션이다. ReclaimUploadObjectsUseCase 가 만료 이후 객체를 이미
            // 지웠고 그때 reclaimed_at 을 채웠으므로 스토리지를 다시 건드리지 않는다. 남은 것은
            // 메타데이터 행뿐이다.
            fileRepositoryPort.deleteMetadata(candidate.fileId());
            return;
        }
        StoredFile deletedFile = candidate.status() == FileStatus.DELETED
                ? candidate
                : fileRepositoryPort.transition(candidate.markDeleted(), candidate.status());
        fileStoragePort.deleteObject(deletedFile.objectKey());
        // 행을 먼저 남겨 두면 삭제 실패 시 같은 후보가 재시도 대상으로 남는다. 스토리지 삭제가
        // 끝난 뒤에 행을 지운다 — 행이 사라지면 더는 훑을 대상이 없어 객체가 영영 회수되지 않는다.
        fileRepositoryPort.deleteMetadata(deletedFile.fileId());
    }
}
