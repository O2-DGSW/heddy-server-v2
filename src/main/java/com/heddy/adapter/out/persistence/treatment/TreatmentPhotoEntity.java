package com.heddy.adapter.out.persistence.treatment;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 시술기록 첨부 사진의 JPA 표현. 바이너리는 스토리지에 있고 여기엔 {@code file_id} 참조만 둔다.
 */
@Entity
@Table(name = "treatment_record_photos")
class TreatmentPhotoEntity extends BaseEntity {

    @Id
    @Column(name = "photo_id", nullable = false, updatable = false)
    private UUID photoId;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20, updatable = false)
    private ImageType imageType;

    protected TreatmentPhotoEntity() {
    }

    TreatmentPhotoEntity(TreatmentPhoto photo) {
        photoId = photo.photoId();
        recordId = photo.recordId();
        fileId = photo.fileId();
        imageType = photo.imageType();
    }

    TreatmentPhoto toDomain() {
        return new TreatmentPhoto(photoId, recordId, fileId, imageType, getCreatedAt());
    }
}
