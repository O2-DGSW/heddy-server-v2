package com.heddy.domain.treatment.port.in;

import com.heddy.domain.treatment.model.ServiceType;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface GetPhotoComparisonUseCase {

    Result getPhotoComparison(Query query);

    record Query(UUID requesterId, UUID recordId) {
    }

    record Photo(UUID photoId, int sortOrder, URI displayUrl) {
    }

    record TreatmentSummary(
            Set<ServiceType> serviceTypes,
            Integer satisfaction,
            String nextVisitCautions
    ) {
    }

    record Result(
            UUID recordId,
            List<Photo> beforePhotos,
            List<Photo> afterPhotos,
            TreatmentSummary treatmentSummary
    ) {
    }
}
