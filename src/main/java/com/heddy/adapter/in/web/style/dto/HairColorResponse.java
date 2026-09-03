package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.model.HairColor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "헤어 컬러 한 건")
public record HairColorResponse(
        @Schema(description = "색상 식별자. 후보 스타일 저장·추천 응답이 가리키는 값")
        @JsonProperty("color_id") UUID colorId,

        @Schema(description = "표시명과 무관하게 고정된 식별 코드. 클라이언트 분기와 AR "
                + "매핑에 쓴다", example = "NATURAL_BLACK")
        String code,

        @Schema(description = "화면에 보여 줄 이름", example = "내추럴 블랙")
        String name,

        @Schema(description = "색상 칩의 점 색. #RRGGBB 형태다", example = "#1C1C1C")
        @JsonProperty("hex_code") String hexCode
) {
    public static HairColorResponse from(HairColor color) {
        return new HairColorResponse(
                color.colorId(), color.code(), color.name(), color.hexCode());
    }
}
