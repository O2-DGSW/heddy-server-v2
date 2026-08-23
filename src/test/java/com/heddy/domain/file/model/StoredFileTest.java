package com.heddy.domain.file.model;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoredFileTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String OBJECT_KEY = "TREATMENT_PHOTO/user/photo.jpg";
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-23T12:00:00Z");
    private static final String SHA256 = "a".repeat(64);

    @Test
    void opensUploadSessionInPendingStateWithoutContentFacts() {
        StoredFile file = pendingPhoto(1_024);

        assertThat(file.status()).isEqualTo(FileStatus.PENDING);
        assertThat(file.isReady()).isFalse();
        assertThat(file.sha256()).isNull();
        assertThat(file.width()).isNull();
        assertThat(file.height()).isNull();
        assertThat(file.createdAt()).isNull();
    }

    @Test
    void givesUploadSessionAnIdentityOfItsOwn() {
        StoredFile file = pendingPhoto(1_024);

        assertThat(file.uploadId()).isNotNull().isNotEqualTo(file.fileId());
    }

    @Test
    void rejectsContentTypeThatPurposeDoesNotAllow() {
        assertThatThrownBy(() -> StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO, OBJECT_KEY,
                "application/pdf", 1_024, EXPIRES_AT))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.CONTENT_TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsDeclaredSizeOverPurposeMaximum() {
        assertThatThrownBy(() -> pendingPhoto(FilePurpose.TREATMENT_PHOTO.maximumBytes() + 1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.TOO_LARGE);
    }

    @Test
    void rejectsNonPositiveFileSize() {
        assertThatThrownBy(() -> pendingPhoto(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordsVerifiedContentFactsWhenBecomingReady() {
        StoredFile ready = pendingPhoto(1_024).markReady(
                new VerifiedContent("image/png", 2_048, SHA256, 800, 600));

        assertThat(ready.isReady()).isTrue();
        assertThat(ready.contentType()).isEqualTo("image/png");
        assertThat(ready.fileSize()).isEqualTo(2_048);
        assertThat(ready.sha256()).isEqualTo(SHA256);
        assertThat(ready.width()).isEqualTo(800);
        assertThat(ready.height()).isEqualTo(600);
    }

    @Test
    void rejectsVerifiedSizeOverPurposeMaximumEvenIfDeclaredSizeWasFine() {
        StoredFile pending = pendingPhoto(1_024);
        long overMaximum = FilePurpose.TREATMENT_PHOTO.maximumBytes() + 1;
        VerifiedContent oversized = new VerifiedContent("image/jpeg", overMaximum, SHA256, 800, 600);

        assertThatThrownBy(() -> pending.markReady(oversized))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.TOO_LARGE);
    }

    @Test
    void refusesToCompleteUploadTwice() {
        StoredFile ready = pendingPhoto(1_024).markReady(verified());

        assertThatThrownBy(() -> ready.markReady(verified()))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.INVALID_STATE_TRANSITION);
    }

    @Test
    void marksPendingAndReadyFilesAsDeleted() {
        assertThat(pendingPhoto(1_024).markDeleted().status()).isEqualTo(FileStatus.DELETED);
        assertThat(pendingPhoto(1_024).markReady(verified()).markDeleted().status())
                .isEqualTo(FileStatus.DELETED);
    }

    @Test
    void refusesToDeleteAlreadyDeletedFile() {
        StoredFile deleted = pendingPhoto(1_024).markDeleted();

        assertThatThrownBy(deleted::markDeleted)
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.INVALID_STATE_TRANSITION);
    }

    @Test
    void keepsIdentityAndImmutableFieldsAcrossTransitions() {
        StoredFile pending = pendingPhoto(1_024);
        StoredFile ready = pending.markReady(verified());

        assertThat(ready.fileId()).isEqualTo(pending.fileId());
        assertThat(ready.uploadId()).isEqualTo(pending.uploadId());
        assertThat(ready.userId()).isEqualTo(pending.userId());
        assertThat(ready.objectKey()).isEqualTo(pending.objectKey());
        assertThat(ready.purpose()).isEqualTo(pending.purpose());
        assertThat(ready.expiresAt()).isEqualTo(pending.expiresAt());
    }

    @Test
    void treatsSessionAsExpiredFromTheExpiryInstantOnward() {
        StoredFile file = pendingPhoto(1_024);

        assertThat(file.isExpiredAt(EXPIRES_AT.minus(Duration.ofSeconds(1)))).isFalse();
        assertThat(file.isExpiredAt(EXPIRES_AT)).isTrue();
        assertThat(file.isExpiredAt(EXPIRES_AT.plus(Duration.ofSeconds(1)))).isTrue();
    }

    private static VerifiedContent verified() {
        return new VerifiedContent("image/jpeg", 1_024, SHA256, 800, 600);
    }

    private static StoredFile pendingPhoto(long fileSize) {
        return StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO, OBJECT_KEY, "image/jpeg", fileSize, EXPIRES_AT);
    }
}
