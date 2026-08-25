package com.heddy.application.file.service;

import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(
        prefix = "app.file-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupScheduler.class);

    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final Clock clock;
    private final Duration pendingGrace;
    private final Duration readyGrace;
    private final int batchSize;

    public FileCleanupScheduler(
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort,
            Clock clock,
            @Value("${app.file-cleanup.pending-grace-seconds:60}") long pendingGraceSeconds,
            @Value("${app.file-cleanup.ready-grace-seconds:86400}") long readyGraceSeconds,
            @Value("${app.file-cleanup.batch-size:100}") int batchSize
    ) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
        this.clock = clock;
        this.pendingGrace = Duration.ofSeconds(pendingGraceSeconds);
        this.readyGrace = Duration.ofSeconds(readyGraceSeconds);
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${app.file-cleanup.interval-ms:60000}",
            initialDelayString = "${app.file-cleanup.initial-delay-ms:60000}")
    @Transactional
    public CleanupResult cleanup() {
        if (!fileRepositoryPort.tryAcquireCleanupLock()) {
            return new CleanupResult(0, 0, true);
        }
        Instant now = clock.instant();
        int deleted = 0;
        int failed = 0;
        for (StoredFile candidate : fileRepositoryPort.findCleanupCandidates(
                now.minus(pendingGrace), now.minus(readyGrace), batchSize)) {
            try {
                StoredFile deletedFile = candidate.status() == FileStatus.DELETED
                        ? candidate
                        : fileRepositoryPort.transition(candidate.markDeleted(), candidate.status());
                fileStoragePort.deleteObject(deletedFile.objectKey());
                fileRepositoryPort.deleteMetadata(deletedFile.fileId());
                deleted++;
            } catch (RuntimeException failure) {
                failed++;
                log.warn("파일 정리에 실패해 다음 실행에서 재시도합니다. fileId={}, objectKey={}",
                        candidate.fileId(), candidate.objectKey(), failure);
            }
        }
        return new CleanupResult(deleted, failed, false);
    }

    public record CleanupResult(int deleted, int failed, boolean skippedByLock) {
    }
}
