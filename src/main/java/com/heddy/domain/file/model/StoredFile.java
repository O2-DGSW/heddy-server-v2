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
 * <p>{@code fileName} 은 클라이언트가 선언한 원본 이름이다. 오브젝트 키 생성에는 절대 쓰지 않고
 * ({@link com.heddy.domain.file.service.ObjectKeyGenerator}), 감사·표시 목적으로만 보관한다.
 *
 * <p>{@code sha256} 은 presign 시점에는 클라이언트가 <em>선언한</em> 값이고, {@code width}·{@code height} 는
 * {@code null} 이다. 선언 해시는 검증된 값이 아니다 — 실물을 보기 전에는 알 수 없는 값이라 선언으로
 * 믿지 않는다. 내용 해시·치수가 <em>실측</em>으로 바뀌는 지점은 객체를 내려받아 확인하는
 * {@link #markReady(VerifiedContent)} 전이다. HEAD 만으로 전이하는 {@link #markReady(StorageObject)} 는
 * 선언 해시를 그대로 둔다(HEAD 로는 내용을 알 수 없다).
 *
 * <p>{@code reclaimedAt} 은 업로드 객체가 최종 회수된 시각이다. 취소 시점의 삭제는 살아 있는
 * presigned PUT URL 때문에 되돌려질 수 있어 채우지 않고, URL 이 못 미치는 만료 이후 회수 경로가
 * 지워서 채운다. 이 값이 있는 DELETED 행은 객체가 이미 없다는 뜻이라 정리 경로에서 스토리지를
 * 다시 건드려서는 안 된다.
 */
public record StoredFile(
        UUID fileId,
        UUID uploadId,
        UUID userId,
        FilePurpose purpose,
        FileStatus status,
        String objectKey,
        String contentType,
        String fileName,
        long fileSize,
        String sha256,
        Integer width,
        Integer height,
        Instant expiresAt,
        Instant createdAt,
        Instant reclaimedAt
) {
    public StoredFile {
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(uploadId, "uploadId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(objectKey, "objectKey");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(fileName, "fileName");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName 은 비어 있을 수 없습니다.");
        }
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (fileSize <= 0) {
            throw new IllegalArgumentException("fileSize 는 양수여야 합니다: " + fileSize);
        }
    }

    /**
     * 업로드 세션을 연다. 여기서 통과한 크기·형식은 클라이언트가 <em>선언한</em> 값이고,
     * {@code declaredSha256} 도 마찬가지로 선언일 뿐이다.
     */
    public static StoredFile pending(
            UUID userId,
            FilePurpose purpose,
            String objectKey,
            String contentType,
            String fileName,
            long fileSize,
            String declaredSha256,
            Instant expiresAt
    ) {
        requireUploadable(purpose, contentType, fileSize);
        return new StoredFile(
                UUID.randomUUID(), UUID.randomUUID(), userId, purpose, FileStatus.PENDING,
                objectKey, contentType, fileName, fileSize, declaredSha256, null, null,
                expiresAt, null, null);
    }

    /**
     * HEAD 로 확인한 실측값으로 검증하고 {@code READY} 로 전이한다.
     *
     * <p>선언 크기와 실측 크기의 일치는 서비스가 먼저 확인한다. 여기서는 선언을 다시 믿지 않고
     * 실측값으로 허용 형식·최대 크기를 재검증한다 — presign 때 1MB 라고 해놓고 20MB 를 올리는 것을
     * 스토리지는 막지 않는다.
     *
     * <p>{@code sha256}·{@code width}·{@code height} 는 HEAD 로는 알 수 없어 그대로 둔다.
     * 이들을 실측값으로 채우려면 객체를 내려받아 디코딩해야 하므로, 내용 해시까지 확인하는 전이는
     * 별도 단위({@link VerifiedContent} 를 받는 {@link #markReady(VerifiedContent)})가 담당한다.
     */
    public StoredFile markReady(StorageObject object) {
        requireStatus(FileStatus.PENDING);
        requireUploadable(purpose, object.contentType(), object.byteSize());
        return new StoredFile(
                fileId, uploadId, userId, purpose, FileStatus.READY, objectKey,
                object.contentType(), fileName, object.byteSize(), sha256, width, height,
                expiresAt, createdAt, null);
    }

    /**
     * 객체를 내려받아 확인한 값으로 검증하고 {@code READY} 로 전이한다.
     *
     * <p>HEAD 만으로는 알 수 없는 해시·이미지 치수까지 아는 시점에 쓴다.
     */
    public StoredFile markReady(VerifiedContent verified) {
        requireStatus(FileStatus.PENDING);
        requireUploadable(purpose, verified.contentType(), verified.fileSize());
        return new StoredFile(
                fileId, uploadId, userId, purpose, FileStatus.READY, objectKey,
                verified.contentType(), fileName, verified.fileSize(), verified.sha256(),
                verified.width(), verified.height(), expiresAt, createdAt, null);
    }

    /** 회수 대상으로 표시한다. 실제 스토리지 객체 삭제는 정리 작업이 맡는다. */
    public StoredFile markDeleted() {
        if (status == FileStatus.DELETED) {
            throw new FileException(FileError.INVALID_STATE_TRANSITION);
        }
        return new StoredFile(
                fileId, uploadId, userId, purpose, FileStatus.DELETED, objectKey,
                contentType, fileName, fileSize, sha256, width, height, expiresAt, createdAt, null);
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
