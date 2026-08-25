package com.heddy.application.file.service;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FileCleanupSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock FileRepositoryPort fileRepositoryPort;
    @Mock FileCleanupProcessor cleanupProcessor;

    private FileCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new FileCleanupScheduler(
                fileRepositoryPort, cleanupProcessor,
                Clock.fixed(NOW, ZoneOffset.UTC), 60, 86_400, 100);
    }

    /**
     * 건별 트랜잭션(REQUIRES_NEW)은 프로세서가 소유한다. 스케줄러는 후보를 하나씩 넘기고
     * 예외를 삼켜 카운트만 정확히 남긴다.
     */
    @Test
    void delegatesEachCandidateToTheProcessorAndCountsSuccessesAndFailures() {
        StoredFile pending = file(FileStatus.PENDING, "pending.jpg");
        StoredFile ready = file(FileStatus.READY, "ready.jpg");
        StoredFile deleted = file(FileStatus.DELETED, "deleted.jpg");
        given(fileRepositoryPort.tryAcquireCleanupLock()).willReturn(true);
        given(fileRepositoryPort.findCleanupCandidates(
                NOW.minusSeconds(60), NOW.minusSeconds(86_400), 100))
                .willReturn(List.of(pending, ready, deleted));
        // 나머지 후보는 아무 일도 없이 성공으로 지나간다. 엄선 스터빙이 다른 인자의 호출을
        // 실패로 몰아가지 않게 기본 동작을 먼저 깔아 둔다.
        doNothing().when(cleanupProcessor).process(any());
        doThrow(new IllegalStateException("DB 장애")).when(cleanupProcessor).process(ready);

        var result = scheduler.cleanup();

        assertThat(result.deleted()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.skippedByLock()).isFalse();
        verify(cleanupProcessor).process(pending);
        verify(cleanupProcessor).process(deleted);
    }

    /** 한 건의 실패가 뒤 후보를 놓치게 해서는 안 된다 — 격리의 핵심이다. */
    @Test
    void keepsProcessingRemainingCandidatesAfterAFailure() {
        StoredFile first = file(FileStatus.PENDING, "first.jpg");
        StoredFile broken = file(FileStatus.PENDING, "broken.jpg");
        StoredFile last = file(FileStatus.DELETED, "last.jpg");
        given(fileRepositoryPort.tryAcquireCleanupLock()).willReturn(true);
        given(fileRepositoryPort.findCleanupCandidates(any(), any(), anyInt()))
                .willReturn(List.of(first, broken, last));
        doNothing().when(cleanupProcessor).process(any());
        doThrow(new IllegalStateException("세션이 깨진 실패"))
                .when(cleanupProcessor).process(broken);

        var result = scheduler.cleanup();

        assertThat(result.deleted()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(1);
        InOrder order = inOrder(cleanupProcessor);
        order.verify(cleanupProcessor).process(first);
        order.verify(cleanupProcessor).process(broken);
        order.verify(cleanupProcessor).process(last);
    }

    @Test
    void skipsRunWhenAnotherInstanceOwnsDatabaseLock() {
        given(fileRepositoryPort.tryAcquireCleanupLock()).willReturn(false);

        var result = scheduler.cleanup();

        assertThat(result.skippedByLock()).isTrue();
        verifyNoInteractions(cleanupProcessor);
        verify(fileRepositoryPort, never()).findCleanupCandidates(
                any(), any(), anyInt());
    }

    private StoredFile file(FileStatus status, String name) {
        return new StoredFile(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FilePurpose.TREATMENT_PHOTO, status, "TREATMENT_PHOTO/" + name,
                "image/jpeg", name, 100, null, null, null,
                NOW.minusSeconds(120), NOW.minusSeconds(172_800),
                status == FileStatus.DELETED ? NOW.minusSeconds(60) : null);
    }
}
