package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.port.in.SavedStyleUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "저장한 후보 스타일 한 건")
public record SavedStyleResponse(
        @Schema(description = "후보 식별자. 공유 대상 지정과 삭제에 쓴다")
        @JsonProperty("saved_style_id") UUID savedStyleId,

        @Schema(description = "카탈로그 스타일 식별자. AR 로 다시 체험할 때 넘긴다. "
                + "AI 추천 스냅샷으로 저장된 예전 후보는 null 일 수 있다")
        @JsonProperty("hairstyle_id") UUID hairstyleId,

        @Schema(description = "저장 당시의 스타일 이름. 카탈로그가 바뀌어도 흔들리지 않는다",
                example = "남자 다운펌")
        @JsonProperty("style_name") String styleName,

        @Schema(description = "선택한 색상. 색을 고르지 않고 저장했으면 null")
        HairColorResponse color,

        @Schema(description = "카드에 띄울 이미지. AR 캡처가 있으면 그 캡처를, 없으면 카탈로그 "
                + "썸네일을 짧은 만료의 Presigned GET 으로 발급한다. 둘 다 없으면 null")
        @JsonProperty("image_url") String imageUrl,

        @Schema(description = "저장할 때 남긴 메모")
        String memo,

        @Schema(description = "저장 시각")
        @JsonProperty("created_at") Instant createdAt
) {
    public static SavedStyleResponse from(SavedStyleUseCase.Item item) {
        return new SavedStyleResponse(
                item.savedStyle().savedStyleId(),
                item.savedStyle().hairstyleId(),
                item.savedStyle().styleName(),
                item.color() == null ? null : HairColorResponse.from(item.color()),
                item.imageUrl() == null ? null : item.imageUrl().toString(),
                item.savedStyle().memo(),
                item.savedStyle().createdAt());
    }
}
