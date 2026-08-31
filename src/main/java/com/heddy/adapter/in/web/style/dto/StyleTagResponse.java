package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "스타일 태그 하나")
public record StyleTagResponse(
        @Schema(description = "태그 식별자. 선호 저장 요청에 이 값을 보낸다")
        @JsonProperty("style_tag_id") UUID styleTagId,

        @Schema(description = "태그 이름")
        @JsonProperty("tag_name") String tagName,

        @Schema(description = "태그 카테고리")
        StyleTagCategory category
) {
    public static StyleTagResponse from(StyleTag styleTag) {
        return new StyleTagResponse(
                styleTag.styleTagId(), styleTag.tagName(), styleTag.category());
    }
}
