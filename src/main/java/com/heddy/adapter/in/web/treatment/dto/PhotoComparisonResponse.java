package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.in.GetPhotoComparisonUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(description = "시술 전후 사진 비교 응답. 전·후 사진이 한쪽이라도 없으면 응답 대신 "
        + "422 PHOTO_COMPARISON_NOT_AVAILABLE 이다")
public record PhotoComparisonResponse(
        @Schema(description = "기록 식별자")
        @JsonProperty("record_id") UUID recordId,

        @Schema(description = "시술 전(BEFORE) 사진. 정렬 순서대로이며 최소 1장이다")
        @JsonProperty("before_photos") List<Photo> beforePhotos,

        @Schema(description = "시술 후(AFTER) 사진. 정렬 순서대로이며 최소 1장이다")
        @JsonProperty("after_photos") List<Photo> afterPhotos,

        @Schema(description = "비교 화면에 함께 보여줄 시술 정보 요약")
        @JsonProperty("treatment_summary") TreatmentSummary treatmentSummary
) {
    @Schema(name = "PhotoComparisonPhoto")
    public record Photo(
            @Schema(description = "사진 식별자")
            @JsonProperty("photo_id") UUID photoId,

            @Schema(description = "같은 구분 안에서의 정렬 순서. 작을수록 앞이다")
            @JsonProperty("sort_order") int sortOrder,

            @Schema(description = "Presigned GET URL. 저장값이 아니라 조회 시점에 짧은 만료로 "
                    + "발급되므로 응답을 캐시해 두고 재사용하면 만료된 URL 이 된다")
            @JsonProperty("display_url") String displayUrl
    ) {
    }

    @Schema(name = "PhotoComparisonTreatmentSummary")
    public record TreatmentSummary(
            @Schema(description = "시술 종류 목록")
            @JsonProperty("service_types") Set<ServiceType> serviceTypes,

            @Schema(description = "만족도(1~5). 입력하지 않았으면 비어 있다")
            @JsonProperty("satisfaction") Integer satisfaction,

            @Schema(description = "다음 방문 주의사항. 입력하지 않았으면 비어 있다")
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
