package com.heddy.application.file.service;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FileCleanupProcessorTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock FileRepositoryPort fileRepositoryPort;
    @Mock FileStoragePort fileStoragePort;

    private FileCleanupProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new FileCleanupProcessor(fileRepositoryPort, fileStoragePort);
    }

    @Test
    void movesExpiredSessionToDeletedThenRemovesObjectAndMetadata() {
        StoredFile pending = file(FileStatus.PENDING, null);
        given(fileRepositoryPort.transition(any(), any())).willAnswer(invocation ->
                invocation.<StoredFile>getArgument(0));

        processor.process(pending);

        verify(fileRepositoryPort).transition(pending.markDeleted(), FileStatus.PENDING);
        verify(fileStoragePort).deleteObject(pending.objectKey());
        verify(fileRepositoryPort).deleteMetadata(pending.fileId());
    }

    @Test
    void guardsTheReadyToDeletedTransitionWithTheExpectedStatus() {
        StoredFile ready = file(FileStatus.READY, null);
        given(fileRepositoryPort.transition(any(), any())).willAnswer(invocation ->
                invocation.<StoredFile>getArgument(0));

        processor.process(ready);

        verify(fileRepositoryPort).transition(ready.markDeleted(), FileStatus.READY);
        verify(fileStoragePort).deleteObject(ready.objectKey());
        verify(fileRepositoryPort).deleteMetadata(ready.fileId());
    }

    /**
     * 회수 표시가 찼다는 건 객체를 이미 최종 지웠다는 뜻이다. 스토리지를 다시 건드리는 것은
     * ReclaimUploadObjectsUseCase 와의 중복 삭제다 — 메타데이터만 마무리한다.
     */
    @Test
    void removesOnlyMetadataForCancelledSessionAlreadyReclaimed() {
        StoredFile reclaimed = file(FileStatus.DELETED, NOW.minusSeconds(30));

        processor.process(reclaimed);

        verifyNoInteractions(fileStoragePort);
        verify(fileRepositoryPort, never()).transition(any(), any());
        verify(fileRepositoryPort).deleteMetadata(reclaimed.fileId());
    }

    @Test
    void deletesObjectAndMetadataForCancelledSessionNotYetReclaimed() {
        StoredFile cancelled = file(FileStatus.DELETED, null);

        processor.process(cancelled);

        verify(fileRepositoryPort, never()).transition(any(), any());
        verify(fileStoragePort).deleteObject(cancelled.objectKey());
        verify(fileRepositoryPort).deleteMetadata(cancelled.fileId());
    }

    /** 예외를 삼키면 스케줄러가 실패를 셀 수 없다. 그대로 던져 카운트와 재시도 대상을 남긴다. */
    @Test
    void propagatesFailureSoTheCallerCountsItInsteadOfCommittingHalfway() {
        StoredFile cancelled = file(FileStatus.DELETED, null);
        willThrow(new IllegalStateException("스토리지 장애"))
                .given(fileStoragePort).deleteObject(cancelled.objectKey());

        assertThatThrownBy(() -> processor.process(cancelled))
                .isInstanceOf(IllegalStateException.class);

        verify(fileRepositoryPort, never()).deleteMetadata(cancelled.fileId());
    }

    /** 행 삭제가 실패하면 전이까지 함께 돌아가 PENDING 이 남아 재시도할 수 있어야 한다. */
    @Test
    void rollsTheWholeCandidateBackWhenMetadataRemovalFails() {
        StoredFile pending = file(FileStatus.PENDING, null);
        given(fileRepositoryPort.transition(any(), any())).willAnswer(invocation ->
                invocation.<StoredFile>getArgument(0));
        doAnswer(invocation -> {
            throw new IllegalStateException("DB 제약 위반");
        }).when(fileRepositoryPort).deleteMetadata(pending.fileId());

        assertThatThrownBy(() -> processor.process(pending))
                .isInstanceOf(IllegalStateException.class);

        verify(fileRepositoryPort)
                .transition(eq(pending.markDeleted()), eq(FileStatus.PENDING));
    }

    private StoredFile file(FileStatus status, Instant reclaimedAt) {
        return new StoredFile(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FilePurpose.TREATMENT_PHOTO, status, "TREATMENT_PHOTO/" + UUID.randomUUID(),
                "image/jpeg", "photo.jpg", 100, null, null, null,
                NOW.minusSeconds(120), NOW.minusSeconds(172_800), reclaimedAt);
    }
}
