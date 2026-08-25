package com.heddy.application.file.service;

import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
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
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "app.file-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupScheduler.class);

    private final FileRepositoryPort fileRepositoryPort;
    private final FileCleanupProcessor cleanupProcessor;
    private final Clock clock;
    private final Duration pendingGrace;
    private final Duration readyGrace;
    private final int batchSize;

    public FileCleanupScheduler(
            FileRepositoryPort fileRepositoryPort,
            FileCleanupProcessor cleanupProcessor,
            Clock clock,
            @Value("${app.file-cleanup.pending-grace-seconds:60}") long pendingGraceSeconds,
            @Value("${app.file-cleanup.ready-grace-seconds:86400}") long readyGraceSeconds,
            @Value("${app.file-cleanup.batch-size:100}") int batchSize
    ) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.cleanupProcessor = cleanupProcessor;
        this.clock = clock;
        this.pendingGrace = Duration.ofSeconds(pendingGraceSeconds);
        this.readyGrace = Duration.ofSeconds(readyGraceSeconds);
        this.batchSize = batchSize;
    }

    /**
     * 트랜잭션은 후보 처리가 아니라 잠금 유지가 이유다. {@code pg_try_advisory_xact_lock} 은
     * 트랜잭션이 끝나는 순간 풀리므로, 실행 내내 다른 인스턴스의 정리를 막으려면 이 메서드가
     * 하나의 트랜잭션 위에서 돌아야 한다. 후보 한 건의 원자성은
     * {@link FileCleanupProcessor} 의 {@code REQUIRES_NEW} 가 담당한다 — 여기서 예외를 삼킨
     * 건은 이 트랜잭션을 더럽히지 않고, 성공한 건도 건별로 이미 커밋돼 있다.
     */
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
        List<StoredFile> candidates = fileRepositoryPort.findCleanupCandidates(
                now.minus(pendingGrace), now.minus(readyGrace), batchSize);
        for (StoredFile candidate : candidates) {
            try {
                cleanupProcessor.process(candidate);
                deleted++;
            } catch (RuntimeException failure) {
                failed++;
                log.warn("파일 정리에 실패해 다음 실행에서 재시도합니다. fileId={}, objectKey={}",
                        candidate.fileId(), candidate.objectKey(), failure);
            }
        }
        CleanupResult result = new CleanupResult(deleted, failed, false);
        log.info("만료 파일 정리 실행 완료: {}", result);
        return result;
    }

    public record CleanupResult(int deleted, int failed, boolean skippedByLock) {
    }
}
