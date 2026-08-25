package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.port.in.ManageTreatmentPhotosUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record AddTreatmentPhotoRequest(
        @NotNull
        @JsonProperty("file_id")
        @Schema(description = "업로드를 마친 요청자 소유 파일 식별자")
        UUID fileId,

        @NotNull
        @JsonProperty("image_type")
        @Schema(description = "촬영 시점 구분. BEFORE AFTER OTHER")
        ImageType imageType,

        @PositiveOrZero
        @JsonProperty("sort_order")
        @Schema(description = "표시 순서", defaultValue = "0")
        Integer sortOrder
) {
    public ManageTreatmentPhotosUseCase.AddCommand toCommand(UUID userId, UUID recordId) {
        return new ManageTreatmentPhotosUseCase.AddCommand(
                userId, recordId, fileId, imageType, sortOrder == null ? 0 : sortOrder);
    }
}
