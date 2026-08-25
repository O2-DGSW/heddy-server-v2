package com.heddy.domain.treatment.model;

import com.heddy.domain.treatment.exception.TreatmentError;
import com.heddy.domain.treatment.exception.TreatmentException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 시술기록에 첨부된 사진 한 장. 실제 바이너리는 스토리지에 있고 여기엔 {@code fileId} 참조만 둔다.
 *
 * <p>공개 URL 을 저장하지 않는 것은 파일 도메인과 같은 보안 규칙이다. 조회 시점에 단기 서명 URL을
 * 발급하므로 이 모델은 파일 식별자만 알면 된다.
 */
public record TreatmentPhoto(
        UUID photoId,
        UUID recordId,
        UUID fileId,
        ImageType imageType,
        int sortOrder,
        Instant createdAt
) {
    public TreatmentPhoto {
        Objects.requireNonNull(photoId, "photoId");
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(imageType, "imageType");
        if (sortOrder < 0) {
            throw new TreatmentException(TreatmentError.PHOTO_SORT_ORDER_NEGATIVE);
        }
    }

    /** 새 사진을 만든다. 식별자는 도메인이 발급하고 {@code createdAt} 은 저장 계층이 채운다. */
    public static TreatmentPhoto create(UUID recordId, UUID fileId, ImageType imageType) {
        return create(recordId, fileId, imageType, 0);
    }

    public static TreatmentPhoto create(
            UUID recordId, UUID fileId, ImageType imageType, int sortOrder
    ) {
        return new TreatmentPhoto(
                UUID.randomUUID(), recordId, fileId, imageType, sortOrder, null);
    }

    /** 순서 컬럼 도입 전 호출부와의 호환을 위한 생성자. */
    public TreatmentPhoto(
            UUID photoId, UUID recordId, UUID fileId, ImageType imageType, Instant createdAt
    ) {
        this(photoId, recordId, fileId, imageType, 0, createdAt);
    }

    public TreatmentPhoto update(ImageType newImageType, int newSortOrder) {
        return new TreatmentPhoto(
                photoId, recordId, fileId, newImageType, newSortOrder, createdAt);
    }
}
