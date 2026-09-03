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
 *
 * <p>{@code image_type} 과 {@code file_id} 는 API(PATCH .../photos/{photoId})로 고칠 수 있으므로
 * {@code updatable} 제약을 두지 않는다. 걸어두면 병합 시 조용히 누락된다.
 */
@Entity
@Table(name = "treatment_record_photos")
class TreatmentPhotoEntity extends BaseEntity {

    @Id
    @Column(name = "photo_id", nullable = false, updatable = false)
    private UUID photoId;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    private ImageType imageType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TreatmentPhotoEntity() {
    }

    TreatmentPhotoEntity(TreatmentPhoto photo) {
        photoId = photo.photoId();
        recordId = photo.recordId();
        fileId = photo.fileId();
        imageType = photo.imageType();
        sortOrder = photo.sortOrder();
    }

    /** 페이지 조립이 사진을 기록별로 모을 때 쓴다(#66). */
    UUID recordId() {
        return recordId;
    }

    TreatmentPhoto toDomain() {
        return new TreatmentPhoto(
                photoId, recordId, fileId, imageType, sortOrder, getCreatedAt());
    }

    void update(TreatmentPhoto photo) {
        fileId = photo.fileId();
        imageType = photo.imageType();
        sortOrder = photo.sortOrder();
    }
}
