package com.heddy.application.file.service;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.in.CompleteUploadCommand;
import com.heddy.domain.file.port.in.PresignUploadCommand;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UploadSessionServiceTest {

    private static final long SESSION_TTL_SECONDS = 300;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final URI UPLOAD_URL = URI.create("https://bucket.s3.example/key?X-Amz-Signature=s");
    private static final Instant NOW = Instant.now();

    @Mock FileRepositoryPort fileRepositoryPort;
    @Mock FileStoragePort fileStoragePort;

    private UploadSessionService service;

    @BeforeEach
    void setUp() {
        service = new UploadSessionService(fileRepositoryPort, fileStoragePort, SESSION_TTL_SECONDS);
    }

    // ------------------------------------------------------------------ presign

    @Test
    void createsPendingSessionAndReturnsSignedUploadUrl() {
        given(fileRepositoryPort.insert(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(fileStoragePort.createUploadUrl(any())).willReturn(UPLOAD_URL);

        var result = service.presign(new PresignUploadCommand(
                USER_ID, FilePurpose.TREATMENT_PHOTO, "image/jpeg", 1_024));

        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(fileRepositoryPort).insert(captor.capture());
        StoredFile inserted = captor.getValue();
        assertThat(inserted.status()).isEqualTo(FileStatus.PENDING);
        assertThat(inserted.userId()).isEqualTo(USER_ID);
        assertThat(inserted.objectKey()).startsWith(
                "TREATMENT_PHOTO/" + USER_ID + "/").endsWith(".jpg");
        assertThat(inserted.expiresAt()).isCloseTo(
                NOW.plusSeconds(SESSION_TTL_SECONDS), within(2, ChronoUnit.SECONDS));

        assertThat(result.uploadId()).isEqualTo(inserted.uploadId());
        assertThat(result.fileId()).isEqualTo(inserted.fileId());
        assertThat(result.uploadUrl()).isEqualTo(UPLOAD_URL);
        assertThat(result.expiresAt()).isEqualTo(inserted.expiresAt());
    }

    @Test
    void rejectsContentTypeOutsideThePurposeAllowListBeforeCreatingASession() {
        assertPresignError(FilePurpose.AR_CAPTURE, "image/heic", 1_024,
                FileError.CONTENT_TYPE_NOT_ALLOWED);
        verifyNoInteractions(fileRepositoryPort);
    }

    @Test
    void rejectsDeclaredSizeOverThePurposeMaximumBeforeCreatingASession() {
        long overMaximum = FilePurpose.TREATMENT_PHOTO.maximumBytes() + 1;

        assertPresignError(FilePurpose.TREATMENT_PHOTO, "image/jpeg", overMaximum,
                FileError.TOO_LARGE);
        verifyNoInteractions(fileRepositoryPort, fileStoragePort);
    }

    // ------------------------------------------------------------------ complete

    @Test
    void verifiesObjectWithHeadAndTransitionsPendingToReady() {
        StoredFile pending = insertPending();
        given(fileStoragePort.findObject(pending.objectKey()))
                .willReturn(Optional.of(new StorageObject("image/jpeg", 2_048)));
        given(fileRepositoryPort.transition(any(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        var result = service.complete(new CompleteUploadCommand(USER_ID, pending.uploadId()));

        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(fileRepositoryPort).transition(captor.capture(), eq(FileStatus.PENDING));
        StoredFile ready = captor.getValue();
        assertThat(ready.status()).isEqualTo(FileStatus.READY);
        // 선언값(1_024)이 아니라 HEAD 실측값(2_048)이 저장된다.
        assertThat(ready.fileSize()).isEqualTo(2_048);
        assertThat(ready.contentType()).isEqualTo("image/jpeg");
        assertThat(result.status()).isEqualTo(FileStatus.READY);
        assertThat(result.fileSize()).isEqualTo(2_048);
    }

    @Test
    void checksOwnershipBeforeRevealingWhetherTheSessionExists() {
        StoredFile pending = pendingPhoto(1_024);
        given(fileRepositoryPort.findByUploadId(pending.uploadId()))
                .willReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.complete(
                new CompleteUploadCommand(UUID.randomUUID(), pending.uploadId())))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_RESOURCE));
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void reportsUnknownSessionsAsResourceNotFound() {
        given(fileRepositoryPort.findByUploadId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(
                new CompleteUploadCommand(USER_ID, UUID.randomUUID())))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void rejectsExpiredSessionsInsteadOfVerifyingTheirObjects() {
        StoredFile expired = new StoredFile(
                UUID.randomUUID(), UUID.randomUUID(), USER_ID, FilePurpose.TREATMENT_PHOTO,
                FileStatus.PENDING, "TREATMENT_PHOTO/" + USER_ID + "/expired.jpg",
                "image/jpeg", 1_024, null, null, null, NOW.minusSeconds(1), null);
        given(fileRepositoryPort.findByUploadId(expired.uploadId())).willReturn(Optional.of(expired));

        assertCompleteError(expired.uploadId(), FileError.UPLOAD_EXPIRED);
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void rejectsCompletionWhenNothingWasUploaded() {
        StoredFile pending = insertPending();
        given(fileStoragePort.findObject(pending.objectKey())).willReturn(Optional.empty());

        assertCompleteError(pending.uploadId(), FileError.OBJECT_NOT_FOUND);
    }

    @Test
    void treatsAnEmptyObjectAsNeverUploaded() {
        StoredFile pending = insertPending();
        given(fileStoragePort.findObject(pending.objectKey()))
                .willReturn(Optional.of(new StorageObject("image/jpeg", 0)));

        assertCompleteError(pending.uploadId(), FileError.OBJECT_NOT_FOUND);
    }

    @Test
    void rejectsObjectsWhoseContentTypeDiffersFromTheSignedSession() {
        StoredFile pending = insertPending();
        given(fileStoragePort.findObject(pending.objectKey()))
                .willReturn(Optional.of(new StorageObject("image/png", 1_024)));

        assertCompleteError(pending.uploadId(), FileError.CONTENT_TYPE_MISMATCH);
    }

    @Test
    void rejectsObjectsBiggerThanThePurposeMaximumEvenWhenDeclaredSmaller() {
        long declaredWithinMaximum = 1_024;
        long actualOverMaximum = FilePurpose.AR_CAPTURE.maximumBytes() + 1;
        StoredFile pending = insertPending(FilePurpose.AR_CAPTURE, "image/jpeg",
                declaredWithinMaximum);
        given(fileStoragePort.findObject(pending.objectKey()))
                .willReturn(Optional.of(new StorageObject("image/jpeg", actualOverMaximum)));

        assertCompleteError(pending.uploadId(), FileError.TOO_LARGE);
        verify(fileRepositoryPort, never()).transition(any(), any());
    }

    /**
     * 재완료 규칙 — 멱등 성공. 이미 READY 인 세션은 저장된 결과를 다시 돌려준다. complete 은
     * 응답을 잃은 클라이언트가 재시도하는 요청이라 거부하면 정상 완료 건이 실패로 보이고, READY 는
     * 종착 상태라 저장된 메타데이터가 변할 수 없으므로 재검증 없이 답해도 안전하다.
     */
    @Test
    void answersRepeatedCompletionWithTheStoredReadyResultWithoutCheckingStorageAgain() {
        StoredFile ready = pendingPhoto(1_024)
                .markReady(new StorageObject("image/jpeg", 1_024));
        given(fileRepositoryPort.findByUploadId(ready.uploadId())).willReturn(Optional.of(ready));

        var result = service.complete(new CompleteUploadCommand(USER_ID, ready.uploadId()));

        assertThat(result.fileId()).isEqualTo(ready.fileId());
        assertThat(result.status()).isEqualTo(FileStatus.READY);
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.fileSize()).isEqualTo(1_024);
        verifyNoInteractions(fileStoragePort);
        verify(fileRepositoryPort, never()).transition(any(), any());
    }

    @Test
    void refusesToCompleteADeletedSession() {
        StoredFile deleted = insertPending().markDeleted();
        given(fileRepositoryPort.findByUploadId(deleted.uploadId())).willReturn(Optional.of(deleted));

        assertCompleteError(deleted.uploadId(), FileError.INVALID_STATE_TRANSITION);
        verifyNoInteractions(fileStoragePort);
    }

    // ------------------------------------------------------------------ 헬퍼

    private StoredFile insertPending() {
        return insertPending(FilePurpose.TREATMENT_PHOTO, "image/jpeg", 1_024);
    }

    private StoredFile insertPending(FilePurpose purpose, String contentType, long fileSize) {
        StoredFile pending = pendingPhoto(purpose, contentType, fileSize);
        given(fileRepositoryPort.findByUploadId(pending.uploadId()))
                .willReturn(Optional.of(pending));
        return pending;
    }

    private static StoredFile pendingPhoto(long fileSize) {
        return pendingPhoto(FilePurpose.TREATMENT_PHOTO, "image/jpeg", fileSize);
    }

    private static StoredFile pendingPhoto(FilePurpose purpose, String contentType, long fileSize) {
        return StoredFile.pending(
                USER_ID,
                purpose,
                purpose.name() + "/" + USER_ID + "/" + UUID.randomUUID(),
                contentType,
                fileSize,
                NOW.plus(5, ChronoUnit.MINUTES));
    }

    private void assertPresignError(
            FilePurpose purpose, String contentType, long fileSize, FileError expected) {
        assertThatThrownBy(() -> service.presign(
                        new PresignUploadCommand(USER_ID, purpose, contentType, fileSize)))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.error()).isEqualTo(expected));
    }

    private void assertCompleteError(UUID uploadId, FileError expected) {
        assertThatThrownBy(() -> service.complete(new CompleteUploadCommand(USER_ID, uploadId)))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.error()).isEqualTo(expected));
    }
}
