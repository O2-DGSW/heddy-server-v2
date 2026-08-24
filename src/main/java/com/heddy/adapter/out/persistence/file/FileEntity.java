package com.heddy.adapter.out.persistence.file;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.file.model.FilePurpose;
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

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

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

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(length = 64)
    private String sha256;

    private Integer width;

    private Integer height;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    protected FileEntity() {
    }

    FileEntity(StoredFile file) {
        fileId = file.fileId();
        uploadId = file.uploadId();
        userId = file.userId();
        purpose = file.purpose();
        objectKey = file.objectKey();
        expiresAt = file.expiresAt();
        status = file.status();
        contentType = file.contentType();
        fileSize = file.fileSize();
        sha256 = file.sha256();
        width = file.width();
        height = file.height();
    }

    StoredFile toDomain() {
        return new StoredFile(
                fileId, uploadId, userId, purpose, status, objectKey, contentType,
                fileSize, sha256, width, height, expiresAt, getCreatedAt());
    }
}
