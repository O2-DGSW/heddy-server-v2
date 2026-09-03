package com.heddy.adapter.in.web.style.dto;

import com.heddy.domain.style.model.HairColor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "헤어 컬러 팔레트 응답")
public record HairColorsResponse(
        @Schema(description = "노출 순서대로 정렬된 활성 색상. 내려간 색은 빠진다")
        List<HairColorResponse> items
) {
    public static HairColorsResponse from(List<HairColor> colors) {
        return new HairColorsResponse(colors.stream().map(HairColorResponse::from).toList());
    }
}
