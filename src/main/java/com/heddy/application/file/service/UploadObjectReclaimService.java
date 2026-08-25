package com.heddy.application.file.service;

import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.in.ReclaimUploadObjectsUseCase;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 취소된 업로드 세션의 객체를 만료 이후 다시 지운다. 취소 시점의 삭제는 아직 살아 있는
 * presigned PUT URL 때문에 되돌려질 수 있어 최종 회수가 아니다 —
 * {@link ReclaimUploadObjectsUseCase} 에 그 이유를 적어 뒀다.
 */
@Service
public class UploadObjectReclaimService implements ReclaimUploadObjectsUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadObjectReclaimService.class);

    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;

    public UploadObjectReclaimService(
            FileRepositoryPort fileRepositoryPort, FileStoragePort fileStoragePort) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    @Transactional
    public int reclaimExpired(int limit) {
        Instant now = Instant.now();
        int reclaimed = 0;
        for (StoredFile target : fileRepositoryPort.findReclaimTargets(now, limit)) {
            if (reclaim(target, now)) {
                reclaimed++;
            }
        }
        return reclaimed;
    }

    private boolean reclaim(StoredFile target, Instant now) {
        try {
            fileStoragePort.deleteObject(target.objectKey());
        } catch (RuntimeException exception) {
            // 표시하지 않고 넘어가면 다음 회차의 대상으로 그대로 남는다. 한 건의 실패로 나머지
            // 대상까지 놓치지 않도록 여기서 끊는다. 키는 남기지 않는다.
            log.warn("업로드 객체 회수 실패: fileId={}", target.fileId(), exception);
            return false;
        }
        fileRepositoryPort.markReclaimed(target.fileId(), now);
        return true;
    }
}
