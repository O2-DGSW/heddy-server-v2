package com.heddy.adapter.out.persistence.file;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileOwnerType;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
class FileEntity extends BaseEntity {

    @Id
    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @Column(name = "upload_id", nullable = false, updatable = false)
    private UUID uploadId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 10, updatable = false)
    private FileOwnerType ownerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private FilePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status;

    @Column(name = "object_key", nullable = false, length = 500, updatable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /** 클라이언트가 선언한 원본 파일명. 키 생성에는 쓰지 않고 감사·표시 목적으로만 둔다. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(length = 64)
    private String sha256;

    private Integer width;

    private Integer height;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /**
     * 스토리지 객체를 최종 회수한 시각. 취소 시점의 삭제로는 채우지 않는다 — 발급된 presigned PUT
     * URL 이 {@code expiresAt} 까지 유효해 그 삭제 뒤에도 객체가 되살아날 수 있기 때문이다.
     * 비어 있는 DELETED 행은 만료 이후 정리 경로가 다시 훑는다.
     */
    @Column(name = "reclaimed_at")
    private Instant reclaimedAt;

    protected FileEntity() {
    }

    FileEntity(StoredFile file) {
        fileId = file.fileId();
        uploadId = file.uploadId();
        userId = file.userId();
        ownerType = file.ownerType();
        purpose = file.purpose();
        objectKey = file.objectKey();
        expiresAt = file.expiresAt();
        status = file.status();
        contentType = file.contentType();
        fileName = file.fileName();
        fileSize = file.fileSize();
        sha256 = file.sha256();
        width = file.width();
        height = file.height();
    }

    StoredFile toDomain() {
        return new StoredFile(
                fileId, uploadId, userId, ownerType, purpose, status, objectKey, contentType,
                fileName, fileSize, sha256, width, height, expiresAt, getCreatedAt(),
                reclaimedAt);
    }
}
