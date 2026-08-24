package com.heddy.application.file.service;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.PresignedUpload;
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
import java.util.Map;
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
    private static final String DECLARED_SHA256 = "b".repeat(64);

    @Mock FileRepositoryPort fileRepositoryPort;
    @Mock FileStoragePort fileStoragePort;

    private UploadSessionService service;

    @BeforeEach
    void setUp() {
        service = new UploadSessionService(fileRepositoryPort, fileStoragePort, SESSION_TTL_SECONDS);
    }

    // ------------------------------------------------------------------ presign

    @Test
    void createsPendingSessionAndReturnsSignedUploadRequest() {
        given(fileRepositoryPort.insert(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(fileStoragePort.createUploadUrl(any())).willReturn(new PresignedUpload(
                UPLOAD_URL, "PUT", Map.of("Content-Type", "image/jpeg", "If-None-Match", "*")));

        var result = service.presign(command(
                FilePurpose.TREATMENT_PHOTO, "image/jpeg", 1_024));

        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(fileRepositoryPort).insert(captor.capture());
        StoredFile inserted = captor.getValue();
        assertThat(inserted.status()).isEqualTo(FileStatus.PENDING);
        assertThat(inserted.userId()).isEqualTo(USER_ID);
        assertThat(inserted.fileName()).isEqualTo("after.jpg");
        assertThat(inserted.sha256()).isEqualTo(DECLARED_SHA256);
        assertThat(inserted.objectKey()).startsWith(
                "TREATMENT_PHOTO/" + USER_ID + "/").endsWith(".jpg");
        assertThat(inserted.expiresAt()).isCloseTo(
                NOW.plusSeconds(SESSION_TTL_SECONDS), within(2, ChronoUnit.SECONDS));

        assertThat(result.uploadId()).isEqualTo(inserted.uploadId());
        assertThat(result.fileId()).isEqualTo(inserted.fileId());
        assertThat(result.upload().url()).isEqualTo(UPLOAD_URL);
        assertThat(result.upload().method()).isEqualTo("PUT");
        assertThat(result.upload().requiredHeaders())
                .containsEntry("If-None-Match", "*")
                .containsEntry("Content-Type", "image/jpeg");
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

    /**
     * 내부 생성물 용도는 외부 발급 경로에서 거부한다. 사용자가 ANALYSIS_OVERLAY_INTERNAL 로 객체를
     * 올릴 수 있으면 이후 단계가 이 purpose 를 "시스템이 만든 파일"로 신뢰할 근거가 사라진다.
     */
    @Test
    void refusesToIssueSessionsForInternalPurposes() {
        assertPresignError(FilePurpose.ANALYSIS_OVERLAY_INTERNAL, "image/png", 1_024,
                FileError.PURPOSE_NOT_ALLOWED);
        verifyNoInteractions(fileRepositoryPort, fileStoragePort);
    }

    // ------------------------------------------------------------------ complete

    @Test
    void verifiesObjectWithHeadAndTransitionsPendingToReady() {
        StoredFile pending = insertPending();
        given(fileStoragePort.findObject(pending.objectKey()))
                .willReturn(Optional.of(new StorageObject("image/jpeg", 1_024)));
        given(fileRepositoryPort.transition(any(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        var result = service.complete(new CompleteUploadCommand(USER_ID, pending.uploadId()));

        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(fileRepositoryPort).transition(captor.capture(), eq(FileStatus.PENDING));
        StoredFile ready = captor.getValue();
        assertThat(ready.status()).isEqualTo(FileStatus.READY);
        assertThat(ready.fileSize()).isEqualTo(1_024);
        assertThat(ready.contentType()).isEqualTo("image/jpeg");
        assertThat(result.status()).isEqualTo(FileStatus.READY);
        assertThat(result.fileSize()).isEqualTo(1_024);
        // HEAD 로는 치수를 알 수 없어 완료 응답의 width·height 는 아직 비어 있다.
        assertThat(result.width()).isNull();
        assertThat(result.height()).isNull();
    }

    /**
     * 명세의 "크기 일치" 검증. presigned PUT 은 크기를 서명하지 않으므로 선언과 다른 크기로 올라갈 수
     * 있고, purpose 최대치 이하라 해도 통과시키면 선언 근거 없는 객체를 READY 로 확정하게 된다.
     */
    @Test
    void rejectsObjectsWhoseActualSizeDiffersFromTheDeclaredSize() {
        StoredFile pending = insertPending();
        given(fileStoragePort.findObject(pending.objectKey()))
                .willReturn(Optional.of(new StorageObject("image/jpeg", 2_048)));

        assertCompleteError(pending.uploadId(), FileError.SIZE_MISMATCH);
        verify(fileRepositoryPort, never()).transition(any(), any());
    }

    /** 최대치 초과도 결국 선언 불일치다. 일치 검증이 먼저 막는다. */
    @Test
    void reportsSizeMismatchEvenWhenActualExceedsThePurposeMaximum() {
        long declaredWithinMaximum = 1_024;
        long actualOverMaximum = FilePurpose.AR_CAPTURE.maximumBytes() + 1;
        StoredFile pending = insertPending(FilePurpose.AR_CAPTURE, "image/jpeg",
                declaredWithinMaximum);
        given(fileStoragePort.findObject(pending.objectKey()))
                .willReturn(Optional.of(new StorageObject("image/jpeg", actualOverMaximum)));

        assertCompleteError(pending.uploadId(), FileError.SIZE_MISMATCH);
        verify(fileRepositoryPort, never()).transition(any(), any());
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
                "image/jpeg", "expired.jpg", 1_024, null, null, null,
                NOW.minusSeconds(1), null);
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

    /**
     * 재완료 규칙 — 멱등 성공. 이미 READY 인 세션은 저장된 결과를 다시 돌려준다. complete 은
     * 응답을 잃은 클라이언트가 재시도하는 요청이라 거부하면 정상 완료 건이 실패로 보이고, READY 는
     * 종착 상태라 저장된 메타데이터가 변할 수 없으므로 재검증 없이 답해도 안전하다. READY 뒤 같은
     * URL 로 덮어쓰기는 서명된 If-None-Match:* 조건이 막으므로 저장된 결과가 낡을 일도 없다.
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

    private static PresignUploadCommand command(
            FilePurpose purpose, String contentType, long fileSize) {
        return new PresignUploadCommand(
                USER_ID, purpose, contentType, "after.jpg", fileSize, DECLARED_SHA256);
    }

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
                "after.jpg",
                fileSize,
                DECLARED_SHA256,
                NOW.plus(5, ChronoUnit.MINUTES));
    }

    private void assertPresignError(
            FilePurpose purpose, String contentType, long fileSize, FileError expected) {
        assertThatThrownBy(() -> service.presign(command(purpose, contentType, fileSize)))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.error()).isEqualTo(expected));
    }

    private void assertCompleteError(UUID uploadId, FileError expected) {
        assertThatThrownBy(() -> service.complete(new CompleteUploadCommand(USER_ID, uploadId)))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.error()).isEqualTo(expected));
    }
}
