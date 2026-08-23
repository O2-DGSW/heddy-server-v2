package com.heddy.domain.file.model;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoredFileTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final String OBJECT_KEY = "TREATMENT_PHOTO/owner/photo.jpg";

    @Test
    void opensUploadSessionInPendingState() {
        StoredFile file = pendingPhoto(1_024);

        assertThat(file.status()).isEqualTo(FileStatus.PENDING);
        assertThat(file.isReady()).isFalse();
        assertThat(file.createdAt()).isNull();
    }

    @Test
    void rejectsContentTypeThatPurposeDoesNotAllow() {
        assertThatThrownBy(() -> StoredFile.pending(
                OWNER_ID, FilePurpose.TREATMENT_PHOTO, OBJECT_KEY, "application/pdf", 1_024))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.CONTENT_TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsDeclaredSizeOverPurposeMaximum() {
        long overMaximum = FilePurpose.TREATMENT_PHOTO.maximumBytes() + 1;

        assertThatThrownBy(() -> pendingPhoto(overMaximum))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.TOO_LARGE);
    }

    @Test
    void rejectsNonPositiveByteSize() {
        assertThatThrownBy(() -> pendingPhoto(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void becomesReadyWithVerifiedValuesFromStorage() {
        StoredFile ready = pendingPhoto(1_024).markReady("image/png", 2_048);

        assertThat(ready.isReady()).isTrue();
        assertThat(ready.contentType()).isEqualTo("image/png");
        assertThat(ready.byteSize()).isEqualTo(2_048);
    }

    @Test
    void rejectsVerifiedSizeOverPurposeMaximumEvenIfDeclaredSizeWasFine() {
        StoredFile pending = pendingPhoto(1_024);
        long overMaximum = FilePurpose.TREATMENT_PHOTO.maximumBytes() + 1;

        assertThatThrownBy(() -> pending.markReady("image/jpeg", overMaximum))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.TOO_LARGE);
    }

    @Test
    void refusesToCompleteUploadTwice() {
        StoredFile ready = pendingPhoto(1_024).markReady("image/jpeg", 1_024);

        assertThatThrownBy(() -> ready.markReady("image/jpeg", 1_024))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.INVALID_STATE_TRANSITION);
    }

    @Test
    void marksPendingAndReadyFilesAsDeleted() {
        assertThat(pendingPhoto(1_024).markDeleted().status()).isEqualTo(FileStatus.DELETED);
        assertThat(pendingPhoto(1_024).markReady("image/jpeg", 1_024).markDeleted().status())
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
        StoredFile ready = pending.markReady("image/jpeg", 1_024);

        assertThat(ready.fileId()).isEqualTo(pending.fileId());
        assertThat(ready.ownerId()).isEqualTo(pending.ownerId());
        assertThat(ready.objectKey()).isEqualTo(pending.objectKey());
        assertThat(ready.purpose()).isEqualTo(pending.purpose());
    }

    private static StoredFile pendingPhoto(long byteSize) {
        return StoredFile.pending(
                OWNER_ID, FilePurpose.TREATMENT_PHOTO, OBJECT_KEY, "image/jpeg", byteSize);
    }
}
