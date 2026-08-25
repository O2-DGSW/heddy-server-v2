package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.port.in.ManageTreatmentPhotosUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record TreatmentPhotoResponse(
        @JsonProperty("photo_id") @Schema(description = "사진 식별자") UUID photoId,
        @JsonProperty("record_id") @Schema(description = "시술기록 식별자") UUID recordId,
        @JsonProperty("image_type") @Schema(description = "촬영 시점 구분") String imageType,
        @JsonProperty("sort_order") @Schema(description = "표시 순서") int sortOrder,
        @JsonProperty("display_url") @Schema(description = "조회 시점에 발급한 Presigned GET URL") String displayUrl
) {
    public static TreatmentPhotoResponse from(ManageTreatmentPhotosUseCase.Result result) {
        return new TreatmentPhotoResponse(
                result.photo().photoId(), result.photo().recordId(),
                result.photo().imageType().name(), result.photo().sortOrder(),
                result.displayUrl() == null ? null : result.displayUrl().toString());
    }
}
