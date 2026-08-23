package com.heddy.domain.file.model;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 오브젝트 스토리지에 올라간(또는 올라갈) 파일 하나.
 *
 * <p>{@code java.io.File} 과 이름이 겹치지 않도록 {@code StoredFile} 로 둔다. 이 타입을 쓰는 클래스가
 * 늘어날수록 잘못된 자동 임포트가 섞일 여지가 커진다.
 *
 * <p>{@code PENDING} 으로 만들어져 실물 검증을 통과하면 {@code READY} 가 된다. 상태 전이는 이 모델만
 * 결정한다. 서비스가 상태 필드를 직접 세팅하면 "검증 안 된 파일이 READY 로 올라오는" 경로가 생긴다.
 *
 * <p>{@code uploadId} 는 업로드 세션 식별자로 {@code fileId} 와 따로 둔다. presign 응답과 complete
 * 요청이 쓰는 값이라 파일 식별자와 같다고 전제하면 나중에 분리할 수 없다.
 *
 * <p>{@code sha256}·{@code width}·{@code height} 는 PENDING 동안 {@code null} 이다. 실물을 보기 전에는
 * 알 수 없는 값이라 선언값으로 채우지 않는다.
 */
public record StoredFile(
        UUID fileId,
        UUID uploadId,
        UUID userId,
        FilePurpose purpose,
        FileStatus status,
        String objectKey,
        String contentType,
        long fileSize,
        String sha256,
        Integer width,
        Integer height,
        Instant expiresAt,
        Instant createdAt
) {
    public StoredFile {
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(uploadId, "uploadId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(objectKey, "objectKey");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (fileSize <= 0) {
            throw new IllegalArgumentException("fileSize 는 양수여야 합니다: " + fileSize);
        }
    }

    /** 업로드 세션을 연다. 여기서 통과한 크기·형식은 클라이언트가 <em>선언한</em> 값이다. */
    public static StoredFile pending(
            UUID userId,
            FilePurpose purpose,
            String objectKey,
            String contentType,
            long fileSize,
            Instant expiresAt
    ) {
        requireUploadable(purpose, contentType, fileSize);
        return new StoredFile(
                UUID.randomUUID(), UUID.randomUUID(), userId, purpose, FileStatus.PENDING,
                objectKey, contentType, fileSize, null, null, null, expiresAt, null);
    }

    /**
     * 실물에서 확인한 값으로 검증하고 {@code READY} 로 전이한다.
     *
     * <p>선언값을 다시 믿지 않고 실측값으로 재검증한다. presign 때 1MB 라고 해놓고 20MB 를 올리는 것을
     * 스토리지는 막지 않는다.
     */
    public StoredFile markReady(VerifiedContent verified) {
        requireStatus(FileStatus.PENDING);
        requireUploadable(purpose, verified.contentType(), verified.fileSize());
        return new StoredFile(
                fileId, uploadId, userId, purpose, FileStatus.READY, objectKey,
                verified.contentType(), verified.fileSize(), verified.sha256(),
                verified.width(), verified.height(), expiresAt, createdAt);
    }

    /** 회수 대상으로 표시한다. 실제 스토리지 객체 삭제는 정리 작업이 맡는다. */
    public StoredFile markDeleted() {
        if (status == FileStatus.DELETED) {
            throw new FileException(FileError.INVALID_STATE_TRANSITION);
        }
        return new StoredFile(
                fileId, uploadId, userId, purpose, FileStatus.DELETED, objectKey,
                contentType, fileSize, sha256, width, height, expiresAt, createdAt);
    }

    public boolean isReady() {
        return status == FileStatus.READY;
    }

    /** 업로드 세션이 만료됐는지. 만료된 세션은 완료 처리하지 않고 정리 대상으로 넘긴다. */
    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    private void requireStatus(FileStatus expected) {
        if (status != expected) {
            throw new FileException(FileError.INVALID_STATE_TRANSITION);
        }
    }

    private static void requireUploadable(FilePurpose purpose, String contentType, long fileSize) {
        if (!purpose.allows(contentType)) {
            throw new FileException(FileError.CONTENT_TYPE_NOT_ALLOWED);
        }
        if (purpose.exceedsMaximum(fileSize)) {
            throw new FileException(FileError.TOO_LARGE);
        }
    }
}
