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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FileCleanupSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock FileRepositoryPort fileRepositoryPort;
    @Mock FileStoragePort fileStoragePort;

    private FileCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new FileCleanupScheduler(
                fileRepositoryPort, fileStoragePort,
                Clock.fixed(NOW, ZoneOffset.UTC), 60, 86_400, 100);
    }

    @Test
    void cleansCandidatesUsingFixedClockAndConfiguredGracePeriods() {
        StoredFile pending = file(FileStatus.PENDING, "pending.jpg");
        StoredFile ready = file(FileStatus.READY, "ready.jpg");
        StoredFile deleted = file(FileStatus.DELETED, "deleted.jpg");
        given(fileRepositoryPort.tryAcquireCleanupLock()).willReturn(true);
        given(fileRepositoryPort.findCleanupCandidates(
                NOW.minusSeconds(60), NOW.minusSeconds(86_400), 100))
                .willReturn(List.of(pending, ready, deleted));
        given(fileRepositoryPort.transition(any(), any())).willAnswer(invocation ->
                invocation.<StoredFile>getArgument(0));

        var result = scheduler.cleanup();

        assertThat(result.deleted()).isEqualTo(3);
        assertThat(result.failed()).isZero();
        verify(fileRepositoryPort).transition(any(), org.mockito.ArgumentMatchers.eq(FileStatus.PENDING));
        verify(fileRepositoryPort).transition(any(), org.mockito.ArgumentMatchers.eq(FileStatus.READY));
        verify(fileRepositoryPort, never()).transition(
                org.mockito.ArgumentMatchers.argThat(file -> file.fileId().equals(deleted.fileId())), any());
        for (StoredFile file : List.of(pending, ready, deleted)) {
            verify(fileStoragePort).deleteObject(file.objectKey());
            verify(fileRepositoryPort).deleteMetadata(file.fileId());
        }
    }

    @Test
    void leavesDeletedMetadataForRetryWhenStorageDeletionFails() {
        StoredFile deleted = file(FileStatus.DELETED, "retry.jpg");
        given(fileRepositoryPort.tryAcquireCleanupLock()).willReturn(true);
        given(fileRepositoryPort.findCleanupCandidates(
                any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(List.of(deleted));
        org.mockito.Mockito.doThrow(new IllegalStateException("S3 unavailable"))
                .when(fileStoragePort).deleteObject(deleted.objectKey());

        var result = scheduler.cleanup();

        assertThat(result.failed()).isEqualTo(1);
        verify(fileRepositoryPort, never()).deleteMetadata(deleted.fileId());
    }

    @Test
    void skipsRunWhenAnotherInstanceOwnsDatabaseLock() {
        given(fileRepositoryPort.tryAcquireCleanupLock()).willReturn(false);

        assertThat(scheduler.cleanup().skippedByLock()).isTrue();

        verifyNoInteractions(fileStoragePort);
        verify(fileRepositoryPort, never()).findCleanupCandidates(
                any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    private StoredFile file(FileStatus status, String name) {
        return new StoredFile(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FilePurpose.TREATMENT_PHOTO, status, "TREATMENT_PHOTO/" + name,
                "image/jpeg", name, 100, null, null, null,
                NOW.minusSeconds(120), NOW.minusSeconds(172_800));
    }
}
