package com.heddy.domain.treatment.port.in;

import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.TreatmentPhoto;

import java.net.URI;
import java.util.UUID;

public interface ManageTreatmentPhotosUseCase {

    Result add(AddCommand command);

    Result update(UpdateCommand command);

    void delete(DeleteCommand command);

    record AddCommand(
            UUID requesterId,
            UUID recordId,
            UUID fileId,
            ImageType imageType,
            int sortOrder
    ) {
    }

    record UpdateCommand(
            UUID requesterId,
            UUID recordId,
            UUID photoId,
            ImageType imageType,
            Integer sortOrder
    ) {
    }

    record DeleteCommand(UUID requesterId, UUID recordId, UUID photoId) {
    }

    record Result(TreatmentPhoto photo, URI displayUrl) {
    }
}
