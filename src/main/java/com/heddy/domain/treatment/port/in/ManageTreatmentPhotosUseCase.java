package com.heddy.domain.treatment.port.in;

import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.TreatmentPhoto;

import java.net.URI;
import java.util.UUID;

public interface ManageTreatmentPhotosUseCase {

    Result add(AddCommand command);

    Result update(UpdateCommand command);

    void delete(DeleteCommand command);

    /** {@code sortOrder} 가 null 이면 기존 사진들 뒤에 붙인다. */
    record AddCommand(
            UUID requesterId,
            UUID recordId,
            UUID fileId,
            ImageType imageType,
            Integer sortOrder
    ) {
    }

    /** 셋 다 null 이면 바꿀 것이 없다. {@code fileId} 는 사진이 가리키는 파일을 교체한다. */
    record UpdateCommand(
            UUID requesterId,
            UUID recordId,
            UUID photoId,
            UUID fileId,
            ImageType imageType,
            Integer sortOrder
    ) {
    }

    record DeleteCommand(UUID requesterId, UUID recordId, UUID photoId) {
    }

    record Result(TreatmentPhoto photo, URI displayUrl) {
    }
}
