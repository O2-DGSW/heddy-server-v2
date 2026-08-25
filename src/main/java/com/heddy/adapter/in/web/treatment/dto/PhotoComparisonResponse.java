package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.in.GetPhotoComparisonUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PhotoComparisonResponse(
        @JsonProperty("record_id") UUID recordId,
        @JsonProperty("before_photos") List<Photo> beforePhotos,
        @JsonProperty("after_photos") List<Photo> afterPhotos,
        @JsonProperty("treatment_summary") TreatmentSummary treatmentSummary
) {
    public record Photo(
            @JsonProperty("photo_id") UUID photoId,
            @JsonProperty("sort_order") int sortOrder,
            @JsonProperty("display_url") String displayUrl
    ) {
    }

    public record TreatmentSummary(
            @JsonProperty("service_types") Set<ServiceType> serviceTypes,
            @JsonProperty("satisfaction") Integer satisfaction,
            @JsonProperty("next_visit_cautions") String nextVisitCautions
    ) {
    }

    public static PhotoComparisonResponse from(GetPhotoComparisonUseCase.Result result) {
        return new PhotoComparisonResponse(
                result.recordId(), photos(result.beforePhotos()), photos(result.afterPhotos()),
                new TreatmentSummary(
                        result.treatmentSummary().serviceTypes(),
                        result.treatmentSummary().satisfaction(),
                        result.treatmentSummary().nextVisitCautions()));
    }

    private static List<Photo> photos(List<GetPhotoComparisonUseCase.Photo> photos) {
        return photos.stream()
                .map(photo -> new Photo(
                        photo.photoId(), photo.sortOrder(), photo.displayUrl().toString()))
                .toList();
    }
}
