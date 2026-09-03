package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.port.in.ManageTreatmentPhotosUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record UpdateTreatmentPhotoRequest(
        @JsonProperty("file_id")
        @Schema(description = "교체할 파일 식별자. 업로드를 마친(READY) 요청자 소유 파일이어야 하고, "
                + "다른 사진이 이미 쓰는 파일은 붙일 수 없다. 사진 식별자는 그대로 유지된다")
        UUID fileId,

        @JsonProperty("image_type")
        @Schema(description = "변경할 촬영 시점 구분. BEFORE AFTER OTHER")
        ImageType imageType,

        @PositiveOrZero
        @JsonProperty("sort_order")
        @Schema(description = "변경할 표시 순서")
        Integer sortOrder
) {
    @AssertTrue(message = "file_id, image_type, sort_order 중 하나 이상을 입력해야 합니다.")
    @Schema(hidden = true)
    public boolean isPatchValid() {
        return fileId != null || imageType != null || sortOrder != null;
    }

    public ManageTreatmentPhotosUseCase.UpdateCommand toCommand(
            UUID userId, UUID recordId, UUID photoId
    ) {
        return new ManageTreatmentPhotosUseCase.UpdateCommand(
                userId, recordId, photoId, fileId, imageType, sortOrder);
    }
}
