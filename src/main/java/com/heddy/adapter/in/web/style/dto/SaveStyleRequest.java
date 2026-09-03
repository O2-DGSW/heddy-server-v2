package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.port.in.SavedStyleUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "후보 스타일 저장 요청")
public record SaveStyleRequest(
        @NotNull
        @Schema(description = "카탈로그 스타일 식별자. 내려간 스타일은 404",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("hairstyle_id") UUID hairstyleId,

        @Schema(description = "선택한 색상. GET /hair-colors 의 color_id 이며 생략할 수 있다")
        @JsonProperty("color_id") UUID colorId,

        @Schema(description = "AR 캡처 파일 식별자. 업로드를 마친(READY) 요청자 소유의 "
                + "AR_CAPTURE 파일이어야 한다. 생략하면 카탈로그 썸네일이 카드에 쓰인다")
        @JsonProperty("capture_id") UUID captureId,

        @Size(max = 500)
        @Schema(description = "메모. 선택 입력, 최대 500자")
        String memo
) {
    public SavedStyleUseCase.SaveCommand toCommand(UUID userId) {
        return new SavedStyleUseCase.SaveCommand(userId, hairstyleId, colorId, captureId, memo);
    }
}
