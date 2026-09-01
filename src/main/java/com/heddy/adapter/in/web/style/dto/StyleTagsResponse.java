package com.heddy.adapter.in.web.style.dto;

import com.heddy.domain.style.model.StyleTag;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스타일 태그 목록 응답")
public record StyleTagsResponse(
        @Schema(description = "태그 목록. 조건에 맞는 태그가 없으면 빈 배열이다")
        List<StyleTagResponse> items
) {
    public static StyleTagsResponse from(List<StyleTag> styleTags) {
        return new StyleTagsResponse(
                styleTags.stream().map(StyleTagResponse::from).toList());
    }
}
