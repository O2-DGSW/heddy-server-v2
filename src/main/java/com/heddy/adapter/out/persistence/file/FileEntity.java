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
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * PK 를 애플리케이션이 배정하므로 {@link Persistable} 로 신규 여부를 직접 알린다. 그렇게 하지 않으면
 * 스프링 데이터가 id 가 채워진 새 엔티티를 기존 행으로 보고 저장할 때마다 SELECT 를 한 번 더 던진다.
 */
@Entity
@Table(name = "files")
class FileEntity extends BaseEntity implements Persistable<UUID> {

    @Id
    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private FilePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FileStatus status;

    @Column(name = "object_key", nullable = false, length = 255, updatable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Transient
    private boolean newEntity = true;

    protected FileEntity() {
    }

    FileEntity(StoredFile file) {
        fileId = file.fileId();
        ownerId = file.ownerId();
        purpose = file.purpose();
        objectKey = file.objectKey();
        update(file);
    }

    /** 소유자·용도·object_key 는 바뀌지 않는다. 바뀌면 다른 파일이지 같은 파일의 갱신이 아니다. */
    void update(StoredFile file) {
        status = file.status();
        contentType = file.contentType();
        byteSize = file.byteSize();
    }

    StoredFile toDomain() {
        return new StoredFile(
                fileId, ownerId, purpose, status, objectKey, contentType, byteSize, getCreatedAt());
    }

    @Override
    public UUID getId() {
        return fileId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        newEntity = false;
    }
}
